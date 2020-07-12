package org.secuso.privacyfriendlybackup.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import org.secuso.privacyfriendlybackup.api.IPFAService
import org.secuso.privacyfriendlybackup.api.common.PfaApi
import org.secuso.privacyfriendlybackup.api.common.PfaError
import org.secuso.privacyfriendlybackup.backupapi.PfaApiConnection
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.data.room.model.enums.PFAJobAction
import org.secuso.privacyfriendlybackup.worker.datakeys.DATA_ID
import org.secuso.privacyfriendlybackup.worker.datakeys.DATA_JOB_ID
import java.lang.Exception
import java.lang.IllegalArgumentException
import kotlin.RuntimeException

class PfaWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params), PfaApiConnection.IPfaApiListener {
    val TAG = "PFABackup"

    private val backupJobId = inputData.getLong(DATA_JOB_ID, -1L)

    private lateinit var connection : PfaApiConnection
    private val db = BackupDatabase.getInstance(context)

    private var workDone = false
    private var error : PfaError? = null

    override suspend fun doWork(): Result {
        try {
            val job = db.backupJobDao().getJobForId(backupJobId)

            when(job.action) {
                BackupJobAction.PFA_JOB_BACKUP -> {
                    /* Nothing to do here */
                }
                BackupJobAction.PFA_JOB_RESTORE -> {
                    if (job.dataId == null) {
                        throw IllegalArgumentException("dataId can not be null.")
                    }
                    val list = db.pfaJobDao().getJobsForPackage(job.packageName)
                    val restoreJobs = list.filter { it.action == PFAJobAction.PFA_RESTORE }

                    if(restoreJobs.isEmpty()) {
                        throw RuntimeException("There is no corresponding PFAJob with action RESTORE for the BackupJob with id $backupJobId")
                    }

                    // there can only be one restoreJob if it is not empty, because there is a unique index over "packageName" and "action"
                    // set internal data id to restore and update database
                    restoreJobs[0].dataId = job.dataId
                    db.pfaJobDao().update(restoreJobs[0])
                }
                else -> {
                    throw IllegalArgumentException("Unknown ${BackupJobAction::class.simpleName}: ${job.action.name} given to ${PfaWorker::class.simpleName}")
                }
            }

            connection = PfaApiConnection(context, job.packageName, this)
            connection.connect()

            // wait max 5 minutes
            var timeout = 60 * 5

            // wait for connection to finish - coroutines <3
            do {
                delay(1000)
            } while(!workDone && --timeout > 0)

            if(connection.isBound()) {
                connection.disconnect()
            }

            if(error != null || timeout <= 0) {
                throw RuntimeException(error?.errorMessage)
            }

            // do not delete yet
            return Result.success()
        } catch (e : Exception) {
            // Error occurred
            //db.backupJobDao().deleteForId(backupJobId)
            return Result.failure()
        }
    }

    override fun onBound(service: IPFAService?) {
        Log.d(TAG,"Bound service successfully.")
        connection.send(PfaApi.ACTION_CONNECT)
    }

    override fun onError(error: PfaError) {
        Log.d(TAG,"Error: ${error.errorMessage}")
        this.error = error
        workDone = true
    }

    override fun onSuccess() {
        Log.d(TAG,"Command sent successfully.")
        workDone = true
        connection.disconnect()
    }

    override fun onDisconnected() {
        Log.d(TAG,"Disconnected")
        if(!workDone) {
            if(error == null) {
                error = PfaError(
                    PfaError.PfaErrorCode.GENERAL_ERROR,
                    "Unknown connection error"
                )
            }
            workDone = true
        }
    }
}