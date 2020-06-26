package org.secuso.privacyfriendlybackup.services

import android.content.Intent
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Log
import org.secuso.privacyfriendlybackup.api.IBackupService
import org.secuso.privacyfriendlybackup.api.common.AbstractAuthService
import org.secuso.privacyfriendlybackup.api.common.BackupApi
import org.secuso.privacyfriendlybackup.api.common.BackupApi.ACTION_SEND_MESSENGER
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_ERROR
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_SUCCESS
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_ERROR
import org.secuso.privacyfriendlybackup.api.common.PfaError
import org.secuso.privacyfriendlybackup.api.util.ApiFormatter
import org.secuso.privacyfriendlybackup.api.util.readString

/**
 * @author Christopher Beckmann
 */
class BackupService : AbstractAuthService() {

    val mMessenger: Messenger? = null

    override val SUPPORTED_API_VERSIONS = listOf(1)

    override val mBinder : IBackupService.Stub = object : IBackupService.Stub() {

        override fun performBackup(input: ParcelFileDescriptor?)  {
            ParcelFileDescriptor.AutoCloseInputStream(input).use {
                // TODO: save backup data
                val backupData = it.readString()
                Log.d(this.javaClass.simpleName, "Received Backup: $backupData");
            }
        }

        override fun performRestore(): ParcelFileDescriptor {
            // write data to parcelfiledescriptor and return it
            val pipes = ParcelFileDescriptor.createPipe()

            // TODO: get restore data from database
            ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
                it.write("".toByteArray(Charsets.UTF_8))
            }

            return pipes[0]
        }

        override fun send(data: Intent?): Intent {
            Log.d(this.javaClass.simpleName, "Intent received: ${ApiFormatter.formatIntent(data)}")
            val result = canAccess(data)
            if(result != null) {
                return result
            }
            // data can not be null here else canAccess(Intent) would have returned an error
            val resultIntent = handle(data!!)
            Log.d(this.javaClass.simpleName, "Sent Reply: ${ApiFormatter.formatIntent(resultIntent)}")
            return resultIntent
        }

        private fun handle(data: Intent): Intent {
            data.setExtrasClassLoader(classLoader)

            return when(data.action) {
                ACTION_SEND_MESSENGER -> {
                    val messenger : Messenger? = data!!.getParcelableExtra(BackupApi.EXTRA_MESSENGER)
                    if(messenger == null) {
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
}