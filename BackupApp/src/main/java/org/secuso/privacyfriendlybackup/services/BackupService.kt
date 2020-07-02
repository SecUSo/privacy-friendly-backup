package org.secuso.privacyfriendlybackup.services

import android.content.Intent
import android.os.Binder
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
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
import org.secuso.privacyfriendlybackup.api.util.*
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.PFAJob
import org.secuso.privacyfriendlybackup.data.room.model.PFAJobAction

/**
 * @author Christopher Beckmann
 */
class BackupService : AbstractAuthService() {
    val TAG = "PFABackup"

    var mMessenger: Messenger? = null
    var mCurrentJob: PFAJob? = null

    override val SUPPORTED_API_VERSIONS = listOf(1)

    override val mBinder : IBackupService.Stub = object : IBackupService.Stub() {

        override fun performBackup(input: ParcelFileDescriptor?) {
            // is client allowed to call this?
            if (!AuthenticationHelper.authenticate(applicationContext, Binder.getCallingUid())) {
                return
            }
            val callingPackageName = AuthenticationHelper.getPackageName(this@BackupService, Binder.getCallingUid())

            runBlocking {
                ParcelFileDescriptor.AutoCloseInputStream(input).use {
                    InternalBackupDataStoreHelper.storeBackupData(this@BackupService, Binder.getCallingUid(), callingPackageName!!, it)
                }

                val db = BackupDatabase.getInstance(this@BackupService)
                val jobDao = db.pfaJobDao()
                jobDao.deleteJobForPackage(callingPackageName, PFAJobAction.PFA_BACKUP.name)

                // is the PFA waiting for commands?
                if (mMessenger != null) {
                    executeCommandsForPackageName(mMessenger!!)
                }
            }
        }

        override fun performRestore(): ParcelFileDescriptor? {
            // is client allowed to call this?
            if(!AuthenticationHelper.authenticate(applicationContext, Binder.getCallingUid())) {
                return null
            }
            val callingPackageName = AuthenticationHelper.getPackageName(this@BackupService, Binder.getCallingUid())

            // write data to parcelfiledescriptor and return it
            val pipes = ParcelFileDescriptor.createPipe()

            // is the data to restore available?
            if(mCurrentJob == null) {
                return null
            }

            runBlocking {
                val restoreData = InternalBackupDataStoreHelper.getInternalData(this@BackupService, mCurrentJob!!.dataId!!).first
                ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
                    restoreData?.copyTo(it)
                }

                val jobDao = BackupDatabase.getInstance(this@BackupService).pfaJobDao()
                jobDao.deleteJobForPackage(callingPackageName, PFAJobAction.PFA_RESTORE.name)

                // is the PFA waiting for commands?
                if(mMessenger != null) {
                    executeCommandsForPackageName(mMessenger!!)
                }
            }

            return pipes[0]
        }

        override fun send(data: Intent?): Intent {
            Log.d(TAG, "Intent received: ${ApiFormatter.formatIntent(data)}")
            val result = canAccess(data)
            if(result != null) {
                return result
            }

            // data can not be null here else canAccess(Intent) would have returned an error
            val resultIntent = handle(data!!)
            Log.d(TAG, "Sent Reply: ${ApiFormatter.formatIntent(resultIntent)}")
            return resultIntent
        }

        private fun handle(data: Intent): Intent {
            data.setExtrasClassLoader(classLoader)

            return when(data.action) {
                ACTION_SEND_MESSENGER -> {
                    mMessenger = data.getParcelableExtra(BackupApi.EXTRA_MESSENGER)
                    if(mMessenger == null) {
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
                        executeCommandsForPackageName(mMessenger!!)

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

    fun executeCommandsForPackageName(messenger: Messenger) {
        val packageName = AuthenticationHelper.getPackageName(this, Binder.getCallingUid())

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
                Log.d(TAG, "sending ${currentJob!!.action.message}")
                messenger.send(Message.obtain(null, currentJob!!.action.message, 0, 0))
                mCurrentJob = currentJob
            }
        }
    }
}