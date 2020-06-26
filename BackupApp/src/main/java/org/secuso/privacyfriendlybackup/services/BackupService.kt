package org.secuso.privacyfriendlybackup.services

import android.content.Intent
import android.os.Binder
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.api.IBackupService
import org.secuso.privacyfriendlybackup.api.common.AbstractAuthService
import org.secuso.privacyfriendlybackup.api.common.BackupApi
import org.secuso.privacyfriendlybackup.api.common.BackupApi.ACTION_SEND_MESSENGER
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_BACKUP
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_DONE
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_ERROR
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_RESTORE
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_ERROR
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_SUCCESS
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_ERROR
import org.secuso.privacyfriendlybackup.api.common.PfaError
import org.secuso.privacyfriendlybackup.api.util.ApiFormatter
import org.secuso.privacyfriendlybackup.api.util.AuthenticationHelper
import org.secuso.privacyfriendlybackup.api.util.readString

/**
 * @author Christopher Beckmann
 */
class BackupService : AbstractAuthService() {
    val TAG = "PFABackup"

    var mMessenger: Messenger? = null

    override val SUPPORTED_API_VERSIONS = listOf(1)

    override val mBinder : IBackupService.Stub = object : IBackupService.Stub() {

        override fun performBackup(input: ParcelFileDescriptor?)  {
            // TODO: check access?

            ParcelFileDescriptor.AutoCloseInputStream(input).use {
                // TODO: save backup data
                val backupData = it.readString()
                Log.d(TAG, "Received Backup: $backupData");
            }

            // is the PFA waiting for commands?
            if(mMessenger != null) {
                executeCommandsForPackageName(mMessenger!!)
            }
        }

        override fun performRestore(): ParcelFileDescriptor {
            // TODO: check access?

            // write data to parcelfiledescriptor and return it
            val pipes = ParcelFileDescriptor.createPipe()

            // TODO: get restore data from database
            ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
                it.write("exampleData".toByteArray(Charsets.UTF_8))
            }

            // is the PFA waiting for commands?
            if(mMessenger != null) {
                executeCommandsForPackageName(mMessenger!!)
            }

            return pipes[0]
        }

        override fun send(data: Intent?): Intent {
            Log.d(TAG, "Intent received: ${ApiFormatter.formatIntent(data)}")
            val result = canAccess(data)
            if(result != null) {
                return result
            }

            // data can not be null here else canAccess(Intent) would have returned an error
            val resultIntent = handle(data!!)
            Log.d(TAG, "Sent Reply: ${ApiFormatter.formatIntent(resultIntent)}")
            return resultIntent
        }

        private fun handle(data: Intent): Intent {
            data.setExtrasClassLoader(classLoader)

            return when(data.action) {
                ACTION_SEND_MESSENGER -> {
                    mMessenger = data.getParcelableExtra(BackupApi.EXTRA_MESSENGER)
                    if(mMessenger == null) {
                        Intent().apply {
                            putExtra(RESULT_CODE, RESULT_CODE_ERROR)
                            putExtra(
                                RESULT_ERROR,
                                PfaError(
                                    PfaError.PfaErrorCode.ACTION_ERROR,
                                    "Action ${data.action} is unsupported."
                                )
                            )
                        }
                    } else {
                        // execute commands for this pfa - messenger is not null because of the check above
                        executeCommandsForPackageName(mMessenger!!)

                        // send success result
                        Intent().apply {
                            putExtra(RESULT_CODE, RESULT_CODE_SUCCESS)
                        }
                    }
                }
                else -> Intent().apply {
                    putExtra(RESULT_CODE, RESULT_CODE_ERROR)
                    putExtra(RESULT_ERROR,
                        PfaError(
                            PfaError.PfaErrorCode.ACTION_ERROR,
                            "Action ${data.action} is unsupported."
                        )
                    )
                }
            }
        }
    }

    fun executeCommandsForPackageName(messenger: Messenger) {
        val packageName = AuthenticationHelper.getPackageName(this, Binder.getCallingUid())

        GlobalScope.launch(Dispatchers.Default) {
            if(packageName.isNullOrEmpty()) {
                messenger.send(Message.obtain(null, MESSAGE_ERROR, 0, 0))
                return@launch
            }

            // TODO: get Command from database

            Log.d(TAG, "sending MESSAGE_DONE")
            messenger.send(Message.obtain(null, MESSAGE_DONE, 0, 0))
        }
    }

    /**
     * only for testing
     */
    fun executeCommandsForPackageName2(messenger: Messenger) {
        val packageName = AuthenticationHelper.getPackageName(this, Binder.getCallingUid())

        GlobalScope.launch(Dispatchers.Default) {
            if(packageName.isNullOrEmpty()) {
                messenger.send(Message.obtain(null, MESSAGE_ERROR, 0, 0))
                return@launch
            }

            Log.d(TAG, "sending MESSAGE_RESTORE")
            messenger.send(Message.obtain(null, MESSAGE_RESTORE, 0, 0))
        }
    }

    /**
     * only for testing
     */
    fun executeCommandsForPackageName3(messenger: Messenger) {
        val packageName = AuthenticationHelper.getPackageName(this, Binder.getCallingUid())

        GlobalScope.launch(Dispatchers.Default) {
            if(packageName.isNullOrEmpty()) {
                messenger.send(Message.obtain(null, MESSAGE_ERROR, 0, 0))
                return@launch
            }

            Log.d(TAG, "sending MESSAGE_DONE")
            messenger.send(Message.obtain(null, MESSAGE_DONE, 0, 0))
        }
    }
}