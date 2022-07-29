package org.secuso.privacyfriendlybackup.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.*
import org.openintents.openpgp.*
import org.openintents.openpgp.OpenPgpError.*
import org.openintents.openpgp.util.OpenPgpApi
import org.openintents.openpgp.util.OpenPgpServiceConnection
import org.secuso.privacyfriendlybackup.BackupApplication.Companion.CHANNEL_ID
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.cache.DecryptionCache
import org.secuso.privacyfriendlybackup.data.cache.DecryptionMetaData
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.InternalBackupData
import org.secuso.privacyfriendlybackup.ui.encryption.UserInteractionRequiredActivity
import org.secuso.privacyfriendlybackup.worker.datakeys.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference


/**
 *
 * This worker can only run if the encryption is set up correctly to fully work in the background.
 * OpenPgpApi.ACTION_CHECK_PERMISSION can be used to check if everything is ok from the UI.
 *
 *
 * @author Christopher Beckmann
 */
class EncryptionWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(
    context,
    params
), OpenPgpServiceConnection.OnBound {

    companion object {
        val REQUEST_CODE_SIGN_AND_ENCRYPT = 9912
        val REQUEST_CODE_DECRYPT_AND_VERIFY = 9913

        val NOTIFICATION_ID = 9000

        val TAG = "PFA Encryption"
    }

    var workDone = false
    var errorOccurred = false


    val backupJobId = inputData.getLong(DATA_JOB_ID, -1)
    val dataId = inputData.getLong(DATA_ID, -1)
    val cryptoProviderPackage = inputData.getString(DATA_OPENPGP_PROVIDER)
    val encrypt = inputData.getBoolean(DATA_ENCRYPT, true)
    val selectedKeyIds = inputData.getLongArray(DATA_KEY_ID)
    val signingKey = inputData.getLong(DATA_SIGNING_KEY_ID, -1L)
    val passphrase = inputData.getString(DATA_PASSPHRASE)
    var internalData : InternalBackupData? = null
    var job : BackupJob? = null
    var decryptedDataId : Long = 0L

    lateinit var mConnection: OpenPgpServiceConnection

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork()")

        if(dataId == -1L) return Result.failure()

        // are we scheduled to decrypt data that is already unencrypted?
        val data = InternalBackupDataStoreHelper.getInternalData(context, dataId)
        if(!encrypt && data.second != null && !data.second!!.encrypted) {
            processJobData(dataId)
            return Result.success()
        }

        if(cryptoProviderPackage.isNullOrEmpty()) {
            // TODO: display notification to fix encryption settings
            return Result.failure()
        }

        mConnection = OpenPgpServiceConnection(context, cryptoProviderPackage, this)
        mConnection.bindToService()

        var timeout = 60 * 5

        // wait for connection to finish
        do {
            delay(1000)
        } while(!workDone && --timeout > 0)

        // always unbind afterwards
        mConnection.unbindFromService()

        // did we have an error?
        return if(errorOccurred || timeout <= 0) {
            Result.failure()
        } else {
            Result.success()
        }
    }

    override fun onBound(service: IOpenPgpService2?) {
        if(encrypt) {
            performEncryption()
        } else {
            decryptAndVerify()
        }
    }

    private fun performEncryption() {
        try {
            runBlocking {
                val (inputStream, data) = InternalBackupDataStoreHelper.getInternalData(
                    context,
                    dataId
                )
                if(data == null) {
                    throw IllegalStateException("Can not perform encryption on null data.")
                }
                internalData = data
                val outputStream = ByteArrayOutputStream()
                val openPgpApi = OpenPgpApi(context, mConnection.service)

                val intent = Intent().apply {
                    action = OpenPgpApi.ACTION_SIGN_AND_ENCRYPT

                    if (selectedKeyIds != null && selectedKeyIds.isNotEmpty() && !selectedKeyIds.contains(
                            0
                        )) {
                        putExtra(OpenPgpApi.EXTRA_KEY_IDS, selectedKeyIds)
                    }

                    if (signingKey != -1L && signingKey != 0L) {
                        putExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, signingKey)
                    }

                    if (!passphrase.isNullOrEmpty()) {
                        putExtra(OpenPgpApi.EXTRA_PASSPHRASE, passphrase)
                    }

                    Log.d(TAG, data.file)
                    putExtra(OpenPgpApi.EXTRA_ORIGINAL_FILENAME, data.file)
                    putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
                }

                openPgpApi.executeApiAsync(
                    intent, inputStream, outputStream,
                    OpenPGPCallback(
                        this@EncryptionWorker,
                        outputStream,
                        REQUEST_CODE_SIGN_AND_ENCRYPT
                    )
                )
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun decryptAndVerify() {
        runBlocking {
            val (inputStream, data) = InternalBackupDataStoreHelper.getInternalData(context, dataId)
            internalData = data
            val outputStream = ByteArrayOutputStream()
            val openPgpApi = OpenPgpApi(context, mConnection.service)

            val intent = Intent().apply {
                action = OpenPgpApi.ACTION_DECRYPT_VERIFY
            }

            openPgpApi.executeApiAsync(
                intent, inputStream, outputStream,
                OpenPGPCallback(
                    this@EncryptionWorker,
                    outputStream,
                    REQUEST_CODE_DECRYPT_AND_VERIFY
                )
            )
        }
    }

    override fun onError(e: Exception?) {
        Log.e(TAG, "Error occurred during decryption.", e)
        e?.printStackTrace()
        errorOccurred = true
        workDone = true
    }

    internal class OpenPGPCallback(
        worker: EncryptionWorker,
        val outputStream: ByteArrayOutputStream,
        val requestCode: Int
    ) : OpenPgpApi.IOpenPgpCallback {
        val worker = WeakReference(worker)


        override fun onReturn(result: Intent) {
            when(result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
                OpenPgpApi.RESULT_CODE_SUCCESS -> {

                    Log.d(TAG, "RESULT_CODE_SUCCESS")

                    when (requestCode) {
                        REQUEST_CODE_DECRYPT_AND_VERIFY -> {
                            val signatureResult: OpenPgpSignatureResult = result.getParcelableExtra(
                                OpenPgpApi.RESULT_SIGNATURE
                            )!!
                            val decryptionResult: OpenPgpDecryptionResult = result.getParcelableExtra(
                                OpenPgpApi.RESULT_DECRYPTION
                            )!!
                            val metadata: OpenPgpMetadata = result.getParcelableExtra(
                                OpenPgpApi.RESULT_METADATA
                            )!!

                            // TODO: check signature
                            // ### INFORMATION for user? -> Display to user -> Save somewhere temporarily?
                            // Signed by
                            signatureResult.confirmedUserIds
                            // primary
                            signatureResult.primaryUserId
                            // signed date
                            signatureResult.signatureTimestamp
                            // hex key id of the signer key
                            signatureResult.keyId

                            when (signatureResult.result) {
                                OpenPgpSignatureResult.RESULT_NO_SIGNATURE -> {
                                    // not signed
                                }
                                OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE -> {
                                    // invalid signature
                                }
                                OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED -> {
                                    // everything is fine
                                }
                                OpenPgpSignatureResult.RESULT_KEY_MISSING -> {
                                    // no key was found for this signature verification
                                }
                                OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED -> {
                                    // successfully verified signature, but with unconfirmed key
                                }
                                OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED -> {
                                    // key has been revoked -> invalid signature!
                                }
                                OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED -> {
                                    // key is expired -> invalid signature!
                                }
                                OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE -> {
                                    // insecure cryptographic algorithms/protocol -> invalid signature!
                                }
                            }

                            // write to temp storage
                            val currentWorker = worker.get()
                            if(currentWorker?.internalData != null) {
                                DecryptionCache.writeDecryptionMetaData(
                                    currentWorker.internalData!!,
                                    DecryptionMetaData(signatureResult, decryptionResult, metadata)
                                )
                            }
                        }
                        else -> {
                            // do nothing
                        }
                    }

                    // store data
                    worker.get()?.storeData(outputStream, requestCode)

                    worker.get()?.workDone = true
                }
                OpenPgpApi.RESULT_CODE_ERROR -> {
                    when (result.getParcelableExtra<OpenPgpError>(OpenPgpApi.RESULT_ERROR)!!.errorId) {
                        CLIENT_SIDE_ERROR -> {
                            Log.d(TAG, "CLIENT_SIDE_ERROR")
                        }
                        GENERIC_ERROR -> {
                            Log.d(TAG, "GENERIC_ERROR")
                        }
                        INCOMPATIBLE_API_VERSIONS -> {
                            Log.d(TAG, "INCOMPATIBLE_API_VERSIONS")
                            // TODO:
                        }
                        NO_OR_WRONG_PASSPHRASE -> {
                            Log.d(TAG, "NO_OR_WRONG_PASSPHRASE")
                            // TODO: inform user about wrong passphrase
                        }
                        NO_USER_IDS -> {
                            Log.d(TAG, "NO_USER_IDS")
                            // TODO:
                        }
                        OPPORTUNISTIC_MISSING_KEYS -> {
                            Log.d(TAG, "OPPORTUNISTIC_MISSING_KEYS")
                        }
                        else -> { /* do nothing */
                        }
                    }
                    worker.get()?.onError(null)

                    Log.d(TAG, "RESULT_CODE_ERROR")
                }
                OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {

                    Log.d(TAG, "RESULT_CODE_USER_INTERACTION_REQUIRED")

                    //worker.get()?.onError(null)
                    val pi: PendingIntent? = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)
                    pi ?: run {
                        worker.get()?.onError(null)
                        return
                    }
                    worker.get()?.postUserInteractionNotification(requestCode, pi)
                }
            }
        }
    }

    private fun storeData(outputStream: ByteArrayOutputStream, requestCode: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val encrypted = when(requestCode) {
                REQUEST_CODE_DECRYPT_AND_VERIFY -> false
                REQUEST_CODE_SIGN_AND_ENCRYPT -> true
                else -> true
            }

            // store to internal storage and delete unencrypted data
            val id = InternalBackupDataStoreHelper.storeData(
                context, internalData!!.packageName, ByteArrayInputStream(
                    outputStream.toByteArray()
                ), internalData!!.timestamp, encrypted
            )
            decryptedDataId = id
            InternalBackupDataStoreHelper.clearData(context, dataId)
            processJobData(id)
        }
    }

    private suspend fun processJobData(id: Long) {
        // store new internal data id into next job
        val dao = BackupDatabase.getInstance(context).backupJobDao()

        job = dao.getJobForId(backupJobId)

        if(job?.nextJob != null) {
            dao.deleteForId(job!!._id)
            val nextJob = dao.getJobForId(job!!.nextJob!!).apply {
                dataId = id
            }
            dao.update(nextJob)
        }
    }

    private fun postUserInteractionNotification(requestCode: Int, pi: PendingIntent) {
        GlobalScope.launch(Dispatchers.Main) {

            val notification = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setContentTitle(context.getString(R.string.notification_user_interaction_required_title))
                setContentText(context.getString(R.string.notification_user_interaction_required_content))
                color = ContextCompat.getColor(context, R.color.ic_launcher_background)
                setSmallIcon(R.drawable.ic_stat_backup)
                setAutoCancel(true)

                val intent = Intent(context, UserInteractionRequiredActivity::class.java).apply {
                    putExtra(OpenPgpApi.RESULT_INTENT, pi)
                    putExtra(UserInteractionRequiredActivity.EXTRA_REQUEST_CODE, requestCode)
                    putExtra(DATA_ID, dataId) // inputData.getLong(DATA_ID, -1)
                    putExtra(DATA_ENCRYPT, encrypt)
                    putExtra(DATA_OPENPGP_PROVIDER, cryptoProviderPackage)
                    putExtra(DATA_KEY_ID, selectedKeyIds)
                    putExtra(DATA_PASSPHRASE, passphrase)
                    putExtra(DATA_SIGNING_KEY_ID, signingKey)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                setContentIntent(pendingIntent)
            }.build()

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, notification)
            }

            // end this worker and wait for user interaction
            onError(null)
        }
    }

}