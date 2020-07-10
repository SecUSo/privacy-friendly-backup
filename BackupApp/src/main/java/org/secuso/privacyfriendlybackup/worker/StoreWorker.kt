package org.secuso.privacyfriendlybackup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.cloud.WebserviceProvider
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.worker.datakeys.DATA_JOB_ID
import java.io.ByteArrayInputStream
import java.lang.Exception
import kotlin.IllegalArgumentException

/**
 * @author Christopher Beckmann
 */
class StoreWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    val backupJobId = inputData.getLong(DATA_JOB_ID, -1)

    val repository = BackupDataStorageRepository(WebserviceProvider(), BackupDatabase.getInstance(context))

    override suspend fun doWork(): Result {
        try {
            val job = BackupDatabase.getInstance(context).backupJobDao().getJobForId(backupJobId)

            if(job.dataId == null) {
                throw IllegalArgumentException("No valid data id provided.")
            }

            when(job.action) {
                BackupJobAction.BACKUP_STORE -> {
                    val result = handleStore(job)
                    if(result != Result.success()) {
                        return result
                    }
                }
                BackupJobAction.BACKUP_LOAD -> {
                    val result = handleLoad(job)
                    if(result != Result.success()) {
                        return result
                    }
                }
                else -> throw IllegalArgumentException("Unknown ${BackupJobAction::class.simpleName}: ${job.action.name} given to ${StoreWorker::class.simpleName}")
            }

            BackupDatabase.getInstance(context).backupJobDao().deleteForId(backupJobId)
            return Result.success()
        } catch (e : Exception) {
            BackupDatabase.getInstance(context).backupJobDao().deleteForId(backupJobId)
            return Result.failure()
        }
    }

    suspend fun handleStore(job : BackupJob) : Result {
        if(job.location.isNullOrEmpty())
            throw IllegalArgumentException("location can not be null if job action is set to store.")

        val storageType = StorageType.valueOf(job.location!!)

        repository.storeFile(context, job.packageName, job.dataId!!, storageType)
        return Result.success()
    }

    suspend fun handleLoad(job : BackupJob) : Result {
        val data = repository.getFile(context, job.dataId!!)
            ?: throw RuntimeException("Loaded data is null. There seems to be no data for given dataId.")

        if(job.nextJob == null) {
            return Result.failure()
        }

        if(data.data == null) {
            // bytes are null - maybe something went wrong with the network - try again
            return Result.retry()
        }

        // store to internal storage
        val id = InternalBackupDataStoreHelper.storeData(context, job.packageName, ByteArrayInputStream(data.data), data.encrypted)

        val nextJob = BackupDatabase.getInstance(context).backupJobDao().getJobForId(job.nextJob!!)
        nextJob.dataId = id
        BackupDatabase.getInstance(context).backupJobDao().update(nextJob)
        return Result.success()
    }
}