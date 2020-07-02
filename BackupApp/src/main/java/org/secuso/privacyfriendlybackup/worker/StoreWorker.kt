package org.secuso.privacyfriendlybackup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.cloud.WebserviceProvider
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.worker.datakeys.DATA_ID
import org.secuso.privacyfriendlybackup.worker.datakeys.DATA_JOB_ID

/**
 *
 *
 * @author Christopher Beckmann
 */
class StoreWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    val jobId = inputData.getLong(DATA_JOB_ID, -1)

    override suspend fun doWork(): Result {
        // do we have a job id?
        if(jobId == -1L) {
            return Result.failure()
        }

        // init repository
        val repository = BackupDataStorageRepository(
            WebserviceProvider(),
            BackupDatabase.getInstance(context)
        )

        val jobData = BackupDatabase.getInstance(context).backupJobDao().getJobForId(jobId)
        if(jobData.dataId == null) {
            return Result.failure()
        }

        // TODO : determine where to save to - prolly via preferences or via data input
        repository.storeFile(context, jobData.packageName, jobData.dataId)

        return Result.success()
    }

}