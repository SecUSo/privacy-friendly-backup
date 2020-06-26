package org.secuso.privacyfriendlybackup.openpgpapi

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import org.openintents.openpgp.IOpenPgpService2
import org.openintents.openpgp.OpenPgpDecryptionResult
import org.openintents.openpgp.OpenPgpSignatureResult
import org.openintents.openpgp.util.OpenPgpApi
import org.openintents.openpgp.util.OpenPgpServiceConnection
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference


const val PREF_OPENPGP_PROVIDER = "PREF_OPENPGP_PROVIDER"

const val REQUEST_CODE_SIGN_AND_ENCRYPT = 9912
const val REQUEST_CODE_DECRYPT_AND_VERIFY = 9913

/**
 *
 * This worker can only run if the encryption is set up correctly to fully work in the background.
 * OpenPgpApi.ACTION_CHECK_PERMISSION can be used to check if everything is ok from the UI.
 *
 *
 * @author Christopher Beckmann
 */
class EncryptionWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params), OpenPgpServiceConnection.OnBound {

    var workDone = false
    var errorOccurred = false

    val mPref = PreferenceManager.getDefaultSharedPreferences(context)

    lateinit var mConnection: OpenPgpServiceConnection

    override suspend fun doWork(): Result {
        val cryptoProviderPackage = mPref.getString(PREF_OPENPGP_PROVIDER, "org.sufficientlysecure.keychain")

        if(cryptoProviderPackage.isNullOrEmpty()) {
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
        if(errorOccurred || timeout <= 0) {
            return Result.failure()
        } else {
            return Result.success()
        }
    }

    override fun onBound(service: IOpenPgpService2?) {
        // TODO: encryption or decryption ?
        performEncryption()
        // or
        decryptAndVerify()
    }

    private fun performEncryption() {
        val intent = Intent().apply {
            action = OpenPgpApi.ACTION_SIGN_AND_ENCRYPT
            // TODO one of these has to be put
            // putExtra(OpenPgpApi.EXTRA_USER_ID)
            // putExtra(OpenPgpApi.EXTRA_KEY_IDS)
            // TODO : load passphrase
            // putExtra(OpenPgpApi.EXTRA_PASSPHRASE, "")
            // TODO: filename add to metadata?
            //putExtra(OpenPgpApi.EXTRA_ORIGINAL_FILENAME, "")
            putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true)
        }
        val inputStream = ByteArrayInputStream( /* TODO : DATA from database? */ "data".toByteArray())
        val outputStream = ByteArrayOutputStream()

        val openPgpApi : OpenPgpApi = OpenPgpApi(context, mConnection.service)
        openPgpApi.executeApiAsync(intent, inputStream, outputStream, OpenPGPCallback(this, outputStream, REQUEST_CODE_SIGN_AND_ENCRYPT))
    }

    fun decryptAndVerify() {
        val intent = Intent().apply {
            action = OpenPgpApi.ACTION_DECRYPT_VERIFY
        }
        val inputStream = ByteArrayInputStream( /* TODO : DATA from database? */ "data".toByteArray())
        val outputStream = ByteArrayOutputStream()


        val openPgpApi : OpenPgpApi = OpenPgpApi(context, mConnection.service)
        openPgpApi.executeApiAsync(intent, inputStream, outputStream, OpenPGPCallback(this, outputStream, REQUEST_CODE_DECRYPT_AND_VERIFY))
    }

    override fun onError(e: Exception?) {
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

                    val resultData = outputStream.toString("UTF-8")
                    // TODO: do something with the encrypted / decrypted data

                    when(requestCode) {
                        REQUEST_CODE_DECRYPT_AND_VERIFY -> {
                            val signatureResult: OpenPgpSignatureResult = result.getParcelableExtra(OpenPgpApi.RESULT_SIGNATURE)!!
                            val decryptionResult: OpenPgpDecryptionResult = result.getParcelableExtra(OpenPgpApi.RESULT_DECRYPTION)!!
                            // val metadata: OpenPgpDecryptMetadata = result.getParcelableExtra(OpenPgpApi.RESULT_METADATA)!!
                            // TODO: check signature
                        }
                        else -> {
                            // do nothing
                        }
                    }
                }
                OpenPgpApi.RESULT_CODE_ERROR -> {
                    worker.get()?.onError(null)
                }
                OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
                    worker.get()?.onError(null)

                    /*
                    val pi: PendingIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT)!!

                    GlobalScope.launch(Dispatchers.Main) {
                        context.startIntentSenderFromChild(
                            this, pi.intentSender,
                            requestCode, null, 0, 0, 0
                        )
                    }
                    */
                }
            }
        }
    }

}