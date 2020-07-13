package org.secuso.privacyfriendlybackup.data

import android.content.Context
import androidx.preference.PreferenceManager
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.PFAJob
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.data.room.model.enums.PFAJobAction
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys
import java.util.*

class BackupJobManager private constructor(
    val context: Context,
    val db : BackupDatabase = BackupDatabase.getInstance(context)
) {

    companion object {
        var INSTANCE : BackupJobManager? = null

        fun getInstance(context: Context) : BackupJobManager {
            if(INSTANCE == null) {
                INSTANCE = BackupJobManager(context)
            }
            return INSTANCE!!
        }

        fun getTagForJob(job: BackupJob) : String =
            "${job._id} - ${job.packageName} - ${job.action.name} - ${job.dataId}"
    }

    /**
     * Inserts a complete chain from backup to storage into the job database for the given packageName.
     * <ul>
     *     <li>Backup</li>
     *     <li>Encrypt (if enabled)</li>
     *     <li>Store</li>
     * </ul>
     */
    suspend fun createBackupJobChain(packageName: String, location: StorageType = StorageType.EXTERNAL) {
        // The jobs have to be inserted backwards, so that we can set the "nextJob" of the following jobs
        val storeJob = BackupJob(
            packageName = packageName,
            timestamp = Date(),
            action = BackupJobAction.BACKUP_STORE,
            location = location.name
        )
        val storeId = db.backupJobDao().insert(storeJob)

        val isEncryptionEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceKeys.PREF_ENCRYPTION_ENABLE, false)

        var encryptId = -1L
        if(isEncryptionEnabled) {
            val encryptJob = BackupJob(
                packageName = packageName,
                timestamp = Date(),
                action = BackupJobAction.BACKUP_ENCRYPT,
                nextJob = storeId
            )
            encryptId = db.backupJobDao().insert(encryptJob)
        }

        // also add a pfajob so the service can access it
        db.pfaJobDao().insert(PFAJob(0,0,packageName,Date(),PFAJobAction.PFA_BACKUP, null))

        val pfaBackupJob = BackupJob(
            packageName = packageName,
            timestamp = Date(),
            action = BackupJobAction.PFA_JOB_BACKUP,
            nextJob = if(isEncryptionEnabled) encryptId else storeId
        )
        db.backupJobDao().insert(pfaBackupJob)
    }

    suspend fun cancelAllJobs(packageName: String) {
        db.backupJobDao().deleteAllForPackage(packageName)
        db.pfaJobDao().deleteAllForPackage(packageName)
    }

    /**
     * If no id is given, the most recent backup will be used.
     */
    suspend fun createRestoreJobChain(packageName: String, metadataId : Long? = null) {
        val metadata = if(metadataId == null) {
            val metadataList = db.backupMetaDataDao().getFromPackage(packageName)

            if(metadataList.isEmpty())
                return

            val mostRecentBackup = metadataList.fold(metadataList[0]) {
                a, item -> if(item.timestamp > a.timestamp) item else a
            }

            mostRecentBackup
        } else {
            db.backupMetaDataDao().getFromId(metadataId)
        }

        // if no valid metadata -> return
        // TODO: show toast?
        metadata ?: return

        db.pfaJobDao().insert(PFAJob(0,0,packageName,Date(),PFAJobAction.PFA_RESTORE, null))

        // The jobs have to be inserted backwards, so that we can set the "nextJob" of the following jobs
        val restoreJob = BackupJob(
            packageName = packageName,
            timestamp = Date(),
            action = BackupJobAction.PFA_JOB_RESTORE
        )
        val restoreId = db.backupJobDao().insert(restoreJob)

        var decryptId = -1L
        if(metadata.encrypted) {
            val decryptJob = BackupJob(
                packageName = packageName,
                timestamp = Date(),
                action = BackupJobAction.BACKUP_DECRYPT,
                nextJob = restoreId
            )
            decryptId = db.backupJobDao().insert(decryptJob)
        }

        val loadJob = BackupJob(
            packageName = packageName,
            timestamp = Date(),
            action = BackupJobAction.BACKUP_LOAD,
            nextJob = if(metadata.encrypted) decryptId else restoreId,
            dataId = if(metadata._id != -1L) metadata._id else metadataId
        )
        db.backupJobDao().insert(loadJob)
    }
}