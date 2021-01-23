package org.secuso.privacyfriendlybackup.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.work.*
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_CRYPTO_PROVIDER
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_ENABLE
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_KEY
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_PASSPHRASE
import org.secuso.privacyfriendlybackup.worker.datakeys.*

/**
 * @author Christopher Beckmann
 */
class BackupJobManagerWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    // current encryption settings
    private val isEncryptionEnabled : Boolean = pref.getBoolean(PREF_ENCRYPTION_ENABLE, false)
    private val provider : String? = pref.getString(PREF_ENCRYPTION_CRYPTO_PROVIDER, "")
    private val passphrase : String? = pref.getString(PREF_ENCRYPTION_PASSPHRASE, "")
    private val key : Long = pref.getLong(PREF_ENCRYPTION_KEY, -1)

    override suspend fun doWork(): Result {
        val db = BackupDatabase.getInstance(context)
        val jobDao = db.backupJobDao()
        val jobs = jobDao.getAll()

        val processedJobs = jobs.filter { it.active }
        val unprocessedJobs = jobs.filter { !it.active }

        for(job in processedJobs) {
            val workInfoFuture = WorkManager.getInstance(context).getWorkInfosByTag(job.getWorkerTag())
            val workInfoList = workInfoFuture.get()
            if(workInfoList.isNotEmpty()) {
                val workInfo = workInfoList[0]
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> { /* DO NOTHING */ }
                    WorkInfo.State.SUCCEEDED -> {
                        // SUCCEEDED but job is not deleted?
                        // this is okay if it is a PFA job because they will be handled by the service
                        if(job.action != BackupJobAction.PFA_JOB_BACKUP && job.action != BackupJobAction.PFA_JOB_RESTORE) {
                            jobDao.deleteForId(job._id)
                        }
                    }
                    WorkInfo.State.BLOCKED -> { /* DO NOTHING */ }
                    WorkInfo.State.CANCELLED -> { /* DO NOTHING */ }
                    WorkInfo.State.ENQUEUED -> { /* DO NOTHING */ }
                    WorkInfo.State.FAILED -> {
                        // if it failed - try again
                        job.active = false
                        jobDao.update(job)
                    }
                }
            }
        }

        val encryptJobs = unprocessedJobs.filter { it.action == BackupJobAction.BACKUP_ENCRYPT }
        val storeJobs = unprocessedJobs.filter { it.action == BackupJobAction.BACKUP_STORE }

        if(encryptJobs.isNotEmpty() && provider.isNullOrEmpty()) {
            // TODO: display notification to user to fix encryption settings?
        } else {
            // go though encrypt jobs and set up workers and corresponding store jobs
            for (job in encryptJobs) {
                if (!isEncryptionEnabled) {

                    // if encryption is disabled - remove encryption job and put the dataId into the store job
                    val storeJob = storeJobs.find { it._id == job.nextJob }

                    if (storeJob != null && job.dataId != null) {
                        storeJob.dataId = job.dataId
                        jobDao.update(storeJob)
                        jobDao.deleteForId(job._id)
                    }
                    // if no next job was found - user needs to decide what is done with this job.
                    // Leave job in the database and wait for user ui interaction - TODO
                } else {
                    if(job.dataId != null) {
                        enqueueEncryptionWork(job)
                        job.active = true
                        jobDao.update(job)
                    }
                }
            }
        }

        // go through the rest of the unprocessed jobs
        for(job in unprocessedJobs.filter { it.action != BackupJobAction.BACKUP_ENCRYPT }) {
            when (job.action) {
                BackupJobAction.BACKUP_DECRYPT -> {
                    if(job.dataId != null) {
                        enqueueDecryptionWorker(job)
                        job.active = true
                        jobDao.update(job)
                    }
                }
                BackupJobAction.BACKUP_LOAD,
                BackupJobAction.BACKUP_STORE -> {
                    if(job.dataId != null) {
                        enqueueStoreWorker(job)
                        job.active = true
                        jobDao.update(job)
                    }
                }
                BackupJobAction.PFA_JOB_BACKUP -> {
                    enqueuePfaWorker(job)
                    job.active = true
                    jobDao.update(job)
                }
                BackupJobAction.PFA_JOB_RESTORE -> {
                    if(job.dataId != null) {
                        enqueuePfaWorker(job)
                        job.active = true
                        jobDao.update(job)
                    }
                }
                else -> { /* DO NOTHING */ }
            }
        }

        return Result.success()
    }

    private fun enqueueEncryptionWork(job : BackupJob) {
        val encryptionWork = createEncryptionWorkRequest(job, true, pref)

        WorkManager.getInstance(context)
            .beginUniqueWork("${job.packageName}(${job.dataId})", ExistingWorkPolicy.KEEP, encryptionWork)
            .enqueue()
    }

    private fun enqueueDecryptionWorker(job : BackupJob) {
        val decryptionWork = createEncryptionWorkRequest(job, false, pref)

        WorkManager.getInstance(context)
            .beginUniqueWork("${job.packageName}(${job.dataId})", ExistingWorkPolicy.KEEP, decryptionWork)
            .enqueue()
    }

    private fun enqueueStoreWorker(job : BackupJob) {
        val storeWork = createStoreWorkRequest(job)

        WorkManager.getInstance(context)
            .beginUniqueWork("${job.packageName}(${job.dataId})", ExistingWorkPolicy.REPLACE, storeWork)
            .enqueue()
    }

    private fun enqueuePfaWorker(job : BackupJob) {
        val pfaWork = createPfaWorkRequest(job)

        WorkManager.getInstance(context)
            .beginUniqueWork("${job.packageName}(${job.dataId})", ExistingWorkPolicy.REPLACE, pfaWork)
            .enqueue()
    }

    companion object {
        fun createEncryptionWorkRequest(job: BackupJob, encrypt: Boolean, sharedPreferences: SharedPreferences): OneTimeWorkRequest {

            val builder = OneTimeWorkRequestBuilder<EncryptionWorker>()
            builder.addTag(job.getWorkerTag())

            val data: MutableList<Pair<String, Any?>> = ArrayList()

            val provider : String? = sharedPreferences.getString(PREF_ENCRYPTION_CRYPTO_PROVIDER, "")
            val passphrase : String? = sharedPreferences.getString(PREF_ENCRYPTION_PASSPHRASE, "")
            val key : Long = sharedPreferences.getLong(PREF_ENCRYPTION_KEY, -1)

            data.add(DATA_JOB_ID to job._id)
            data.add(DATA_ID to job.dataId)
            data.add(DATA_OPENPGP_PROVIDER to provider)
            data.add(DATA_ENCRYPT to encrypt)
            data.add(DATA_KEY_ID to longArrayOf()) // TODO - this is for encryption for other ppl
            data.add(DATA_SIGNING_KEY_ID to key)
            data.add(DATA_PASSPHRASE to passphrase)

            return builder.setInputData(
                workDataOf(*data.toTypedArray())
            ).build()
        }

        fun createStoreWorkRequest(job: BackupJob): OneTimeWorkRequest {
            val builder = OneTimeWorkRequestBuilder<StoreWorker>()
            builder.addTag(job.getWorkerTag())

//        val constraints = Constraints.Builder().setRequiresStorageNotLow(true).build()
//        builder.setConstraints(constraints)

            val data: MutableList<Pair<String, Any?>> = ArrayList()

            data.add(DATA_JOB_ID to job._id)
            data.add(DATA_ID to job.dataId)
            data.add(DATA_TIMESTAMP to job.timestamp.time)
            data.add(DATA_BACKUP_LOCATION to job.location)

            return builder.setInputData(
                workDataOf(*data.toTypedArray())
            ).build()
        }

        private fun createPfaWorkRequest(job: BackupJob): OneTimeWorkRequest {
            val builder = OneTimeWorkRequestBuilder<PfaWorker>()
            builder.addTag(job.getWorkerTag())

            val data: MutableList<Pair<String, Any?>> = ArrayList()

            data.add(DATA_JOB_ID to job._id)
            data.add(DATA_ID to job.dataId)

            return builder.setInputData(
                workDataOf(*data.toTypedArray())
            ).build()
        }
    }

}