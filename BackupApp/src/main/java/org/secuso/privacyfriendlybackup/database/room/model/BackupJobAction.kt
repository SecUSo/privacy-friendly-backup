package org.secuso.privacyfriendlybackup.database.room.model

import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_BACKUP
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_RESTORE

enum class BackupJobAction(val prio : Int, val message: Int) {
    PFA_BACKUP(1, MESSAGE_BACKUP),
    PFA_RESTORE(2, MESSAGE_RESTORE)
}
