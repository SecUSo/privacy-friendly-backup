package org.secuso.privacyfriendlybackup.services

import android.content.Intent
import android.os.Binder
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.secuso.privacyfriendlybackup.api.IBackupService
import org.secuso.privacyfriendlybackup.api.common.AbstractAuthService
import org.secuso.privacyfriendlybackup.api.common.BackupApi
import org.secuso.privacyfriendlybackup.api.common.BackupApi.ACTION_SEND_MESSENGER
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_DONE
import org.secuso.privacyfriendlybackup.api.common.BackupApi.MESSAGE_ERROR
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_ERROR
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_CODE_SUCCESS
import org.secuso.privacyfriendlybackup.api.common.CommonApiConstants.RESULT_ERROR
import org.secuso.privacyfriendlybackup.api.common.PfaError
import org.secuso.privacyfriendlybackup.api.util.ApiFormatter
import org.secuso.privacyfriendlybackup.api.util.AuthenticationHelper
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.PFAJob
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.data.room.model.enums.PFAJobAction
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Christopher Beckmann
 */
class BackupService : AbstractAuthService() {
    val TAG = "PFA BackupService"

    var mMessengers = ConcurrentHashMap<Int, Messenger?>()
    var mActiveJobs = ConcurrentHashMap<Int, PFAJob?>()

    override val SUPPORTED_API_VERSIONS = listOf(1)

    override val mBinder : IBackupService.Stub = object : IBackupService.Stub() {

        override fun performBackup(input: ParcelFileDescriptor?) {
            val callerId = Binder.getCallingUid()
            // is client allowed to call this?

            Log.d(TAG, "Authenticating ${callerId}...")
            if (!AuthenticationHelper.authenticate(applicationContext, callerId)) {
                Log.d(TAG, "Authentication failed for ${callerId}")
                return
            }
            val callingPackageName = AuthenticationHelper.getPackageName(this@BackupService, callerId)
            Log.d(TAG, "Retrieved package name ${callingPackageName}")

            runBlocking {

                Log.d(TAG, "Storing backup data for ${callingPackageName}")
                ParcelFileDescriptor.AutoCloseInputStream(input).use {
                    InternalBackupDataStoreHelper.storeBackupData(this@BackupService, callingPackageName!!, it)
                }

                val db = BackupDatabase.getInstance(this@BackupService)
                val jobDao = db.pfaJobDao()
                jobDao.deleteJobForPackage(callingPackageName, PFAJobAction.PFA_BACKUP.name)

                // is the PFA waiting for commands?
                val messenger = mMessengers[callerId]
                if (messenger != null) {
                    executeCommandsForPackageName(messenger, callerId)
                }
            }
        }

        override fun performRestore(): ParcelFileDescriptor? {
            val callerId = Binder.getCallingUid()

            // is client allowed to call this?
            Log.d(TAG, "Authenticating ${callerId}...")
            if(!AuthenticationHelper.authenticate(this@BackupService, callerId)) {
                Log.d(TAG, "Authentication failed for ${callerId}")
                return null
            }
            val callingPackageName = AuthenticationHelper.getPackageName(this@BackupService, Binder.getCallingUid())
                ?: return null
            Log.d(TAG, "Retrieved package name ${callingPackageName}")

            Log.d(TAG, "Create pipes for ${callingPackageName}")
            // write data to parcelfiledescriptor and return it
            val pipes = ParcelFileDescriptor.createPipe()
            Log.d(TAG, "Pipes created: ${pipes}")

            // is the data to restore available?
            val currentJob = mActiveJobs[Binder.getCallingUid()] ?: return null
            Log.d(TAG, "Get active job $currentJob")

            runBlocking {
                val restoreData = InternalBackupDataStoreHelper.getInternalData(this@BackupService, currentJob.dataId!!).first
                Log.d(TAG, "Restore data read $restoreData")

                // start writing async, because PFA has to read at the same time -
                // that means the outer method has to finish while writing can block if the buffer
                // is full
                GlobalScope.launch(IO) {
                    ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use { outS ->
                        restoreData?.use { inS ->
                            inS.copyTo(outS)
                        }
                    }
                }

                Log.d(TAG, "Copied data into pipe")

                InternalBackupDataStoreHelper.clearData(this@BackupService, currentJob.dataId!!)
                Log.d(TAG, "Clear internal data.")

                val backupJobDao = BackupDatabase.getInstance(this@BackupService).backupJobDao()

                // delete corresponding backup job
                val pfaJobs = backupJobDao.getJobsForPackage(callingPackageName)
                Log.d(TAG, "List of jobs: $pfaJobs")
                val pfaJob = pfaJobs.find { it.action == BackupJobAction.PFA_JOB_RESTORE }
                Log.d(TAG, "Get backup job: $pfaJob")

                if(pfaJob != null) {
                    Log.d(TAG, "Deleting job $pfaJob")
                    backupJobDao.deleteForId(pfaJob._id)
                }

                // delete pfa job
                val pfaJobDao = BackupDatabase.getInstance(this@BackupService).pfaJobDao()
                pfaJobDao.deleteJobForPackage(callingPackageName, PFAJobAction.PFA_RESTORE.name)

                // is the PFA waiting for commands?
                val messenger = mMessengers[callerId]
                if (messenger != null) {
                    executeCommandsForPackageName(messenger, callerId)
                }
            }

            return pipes[0]
        }

        override fun send(data: Intent?): Intent {
            val callerId = Binder.getCallingUid()
            Log.d(TAG, "Intent received: ${ApiFormatter.formatIntent(data)}")
            val result = canAccess(data, callerId)
            if(result != null) {
                return result
            }

            // data can not be null here else canAccess(Intent) would have returned an error
            val resultIntent = handle(data!!, callerId)
            Log.d(TAG, "Sent Reply: ${ApiFormatter.formatIntent(resultIntent)}")
            return resultIntent
        }

        private fun handle(data: Intent, callerId : Int): Intent {
            data.setExtrasClassLoader(classLoader)

            return when(data.action) {
                ACTION_SEND_MESSENGER -> {
                    val messenger : Messenger? = data.getParcelableExtra(BackupApi.EXTRA_MESSENGER)
                    mMessengers[callerId] = messenger
                    if(messenger == null) {
                        Intent().apply {
                            putExtra(RESULT_CODE, RESULT_CODE_ERROR)
                            putExtra(
                                RESULT_ERROR,
                                PfaError(
                                    PfaError.PfaErrorCode.ACTION_ERROR,
                                    "Action ${data.action} is unsupported."
                                )
                            )
                        }
                    } else {
                        // execute commands for this pfa - messenger is not null because of the check above
                        executeCommandsForPackageName(messenger, callerId)

                        // send success result
                        Intent().apply {
                            putExtra(RESULT_CODE, RESULT_CODE_SUCCESS)
                        }
                    }
                }
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

    fun executeCommandsForPackageName(messenger: Messenger, callerId: Int) {
        val packageName = AuthenticationHelper.getPackageName(this, callerId)

        GlobalScope.launch(Dispatchers.Default) {
            if(packageName.isNullOrEmpty()) {
                messenger.send(Message.obtain(null, MESSAGE_ERROR, 0, 0))
                return@launch
            }

            val jobDao = BackupDatabase.getInstance(this@BackupService).pfaJobDao()
            val jobsForCallingApp = jobDao.getJobsForPackage(packageName)

            Log.d(TAG, "BackupJobs: $jobsForCallingApp loaded from database")

            if(jobsForCallingApp.isEmpty()) {
                Log.d(TAG, "sending MESSAGE_DONE")
                messenger.send(Message.obtain(null, MESSAGE_DONE, 0, 0))
            } else {
                // get next job and send message
                var currentJob : PFAJob? = null
                for (job in jobsForCallingApp) {
                    if(currentJob == null || job.action.prio < currentJob.action.prio) {
                        currentJob = job
                    }
                }
                Log.d(TAG, "sending ${currentJob!!.action.name}")
                Log.d(TAG, "mActiveJobs[${callerId}] = ${currentJob}")
                Log.d(TAG, "callerId: $callerId - Binder.getCallingUid(): ${Binder.getCallingUid()}")
                mActiveJobs[callerId] = currentJob
                messenger.send(Message.obtain(null, currentJob.action.message, 0, 0))
            }
        }
    }
}