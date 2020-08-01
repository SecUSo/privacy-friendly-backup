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
import java.io.IOException
import java.io.InputStream
import java.util.*

object InternalBackupDataStoreHelper {
    const val TAG = "PFA Internal"

    const val BACKUP_DIR = "tempData"

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
                Log.d(TAG, "Deleting job with id ${pfaJob._id}")
                backupJobDao.deleteForId(pfaJob._id)

                Log.d(TAG, "Updating job with id ${nextJob._id}")
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

    suspend fun getInternalDataAsFile(context: Context, dataId: Long): Pair<File?, InternalBackupData> {
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getById(dataId)

        val path = File(context.filesDir, BACKUP_DIR)
        return File(path, data.file) to data
    }

    suspend fun getInternalData(context: Context, filename: String): Pair<InputStream?, InternalBackupData> {
        Log.d(TAG, "getInternalData(context, $filename)")
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getByFilename(filename)

//        if(data.packageName != callingPackageName && data.uid == callingUid) {
//            Log.d(TAG, "[No Restore Data found.]")
//            return null
//        }

        val path = File(context.filesDir, BACKUP_DIR)
        return File(path, data.file).inputStream() to data
    }

    suspend fun clearData(context: Context, dataId: Long) {
        Log.d(TAG, "clearData(context, $dataId)")
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getById(dataId)
        val file = File(data.file)

        try {
            file.delete()
            BackupDatabase.getInstance(context).internalBackupDataDao().delete(dataId)
            Log.d(TAG, "File(${file.absolutePath}) deleted.")
        } catch (e : IOException) {
            e.printStackTrace()
        }
    }

}