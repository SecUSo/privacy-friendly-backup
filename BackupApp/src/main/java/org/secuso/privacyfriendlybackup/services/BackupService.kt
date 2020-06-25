package org.secuso.privacyfriendlybackup.services

import android.content.Intent
import android.os.ParcelFileDescriptor
import org.secuso.privacyfriendlybackup.api.IBackupService
import org.secuso.privacyfriendlybackup.api.common.AbstractAuthService
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_ERROR
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_ERROR
import org.secuso.privacyfriendlybackup.api.common.PfaError

/**
 * @author Christopher Beckmann
 */
class BackupService : AbstractAuthService() {

    override val SUPPORTED_API_VERSIONS = listOf(1)

    override val mBinder : IBackupService.Stub = object : IBackupService.Stub() {

        override fun performBackup() {
            TODO()
        }

        override fun performRestore()  {
            TODO()
        }

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
                //ACTION_BACKUP -> TODO()
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