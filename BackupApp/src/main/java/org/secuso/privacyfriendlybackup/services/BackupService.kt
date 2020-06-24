package org.secuso.privacyfriendlybackup.services

import android.content.Intent
import android.os.IBinder
import org.secuso.privacyfriendlybackup.api.IBackupService
import org.secuso.privacyfriendlybackup.api.pfa.*

/**
 * @author Christopher Beckmann
 */
class BackupService : AbstractAuthService() {

    override val SUPPORTED_API_VERSIONS = listOf(1)

    override val mBinder : IBackupService.Stub = object : IBackupService.Stub() {
        override fun send(data: Intent?): Intent {
            val result = canAccess(data)
            if(result != null) {
                return result
            }
            // data can not be null here else canAccess(Intent) would have returned an error
            return handle(data!!)
        }

        private fun handle(data: Intent): Intent {
            return when(data.action) {
                ACTION_BACKUP -> {}
                ACTION_RESTORE -> {}
                else -> Intent().apply {
                    putExtra(RESULT_CODE, RESULT_CODE_ERROR)
                    putExtra(RESULT_ERROR, PfaError(PfaError.PfaErrorCode.ACTION_ERROR, "Action ${data.action} is unsupported."))
                }
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder {
        return mBinder;
    }
}