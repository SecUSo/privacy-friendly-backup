package org.secuso.privacyfriendlybackup.data.room.model.enums

import androidx.annotation.DrawableRes
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_BACKUP
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_RESTORE

enum class PFAJobAction(val prio : Int, val message: Int) {
    PFA_BACKUP(1, MESSAGE_BACKUP),
    PFA_RESTORE(2, MESSAGE_RESTORE),
}

enum class BackupJobAction(@DrawableRes val image : Int) {
    BACKUP_ENCRYPT(R.drawable.ic_encryption_24),
    BACKUP_DECRYPT(R.drawable.ic_lock_open_24),
    BACKUP_STORE(R.drawable.ic_cloud_upload_24),
    BACKUP_LOAD(R.drawable.ic_cloud_download_24),
    PFA_JOB_BACKUP(R.drawable.ic_save_alt_24),
    PFA_JOB_RESTORE(R.drawable.ic_restore_24)
}
