package org.secuso.privacyfriendlybackup.data.internal

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.secuso.privacyfriendlybackup.api.util.copyInputStreamToFile
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.data.room.model.InternalBackupData
import org.secuso.privacyfriendlybackup.util.BackupDataUtil.getFileName
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker
import org.secuso.privacyfriendlybackup.worker.StoreWorker
import java.io.File
import java.io.InputStream
import java.util.*

object InternalBackupDataStoreHelper {
    const val TAG = "BackupDataStoreHelper"

    const val BACKUP_DIR = "backupData"

    suspend fun storeBackupData(context: Context, packageName: String, inputStream: InputStream, encrypted: Boolean = false) : Long {
        val dataId = storeData(context, packageName, inputStream, encrypted)

        val backupJobDao = BackupDatabase.getInstance(context).backupJobDao()

        // delete corresponding backup job and update the dataID of the next
        val pfaJobs = backupJobDao.getJobsForPackage(packageName)
        val pfaJob = pfaJobs.find { it.action == BackupJobAction.PFA_JOB_BACKUP }
        if(pfaJob != null) {
            val nextJob = pfaJobs.find { it._id == pfaJob.nextJob }
            if(nextJob != null) {
                nextJob.dataId = dataId
                backupJobDao.deleteForId(pfaJob._id)
                backupJobDao.update(nextJob)
                return dataId
            }
        }

        return dataId
    }

    suspend fun storeData(context: Context, packageName: String, inputStream: InputStream, encrypted : Boolean = false) : Long {
        val date = Date()

        val path = File(context.filesDir, BACKUP_DIR)
        path.mkdirs()
        val fileName = getFileName(date, packageName)
        File(path, fileName).copyInputStreamToFile(inputStream)

        Log.d(TAG, "Saved $fileName")

        // save filename into db
        val data = InternalBackupData(
            packageName = packageName,
            timestamp = date,
            file = fileName,
            encrypted = encrypted
        )
        val dataId = BackupDatabase.getInstance(context).internalBackupDataDao().insert(data)
        return dataId
    }

    suspend fun getInternalData(context: Context, dataId: Long): Pair<InputStream?, InternalBackupData> {
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getById(dataId)

//        if(data.packageName != callingPackageName && data.uid == callingUid) {
//            Log.d(TAG, "[No Restore Data found.]")
//            return null
//        }

        val path = File(context.filesDir, BACKUP_DIR)
        return File(path, data.file).inputStream() to data
    }

    suspend fun getInternalData(context: Context, filename: String): Pair<InputStream?, InternalBackupData> {
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getByFilename(filename)

//        if(data.packageName != callingPackageName && data.uid == callingUid) {
//            Log.d(TAG, "[No Restore Data found.]")
//            return null
//        }

        val path = File(context.filesDir, BACKUP_DIR)
        return File(path, data.file).inputStream() to data
    }

    suspend fun clearData(context: Context, dataId: Long) {
        BackupDatabase.getInstance(context).internalBackupDataDao().delete(dataId)
    }

}