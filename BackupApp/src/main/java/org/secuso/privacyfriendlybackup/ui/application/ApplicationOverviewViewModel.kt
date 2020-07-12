package org.secuso.privacyfriendlybackup.ui.application

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.api.util.addSources
import org.secuso.privacyfriendlybackup.data.BackupJobManager
import org.secuso.privacyfriendlybackup.data.apps.PFApplicationHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData


typealias BackupApplicationDataList = ArrayList<ApplicationOverviewViewModel.BackupApplicationData>

class ApplicationOverviewViewModel(app : Application) : AndroidViewModel(app) {

    data class BackupApplicationData(
        val pfaInfo: PFApplicationHelper.PFAInfo,
        val jobs: List<BackupJob>,
        val backups: List<StoredBackupMetaData>
    ) {
        override fun equals(other: Any?): Boolean {
            if(other == null) return false
            if(other !is BackupApplicationData) return false
            if(pfaInfo != other.pfaInfo) return false
            if(jobs.size != other.jobs.size) return false
            jobs.forEachIndexed {
                i,j -> if(other.jobs[i] != j) return false
            }
            if(backups.size != other.backups.size) return false
            backups.forEachIndexed {
                i,b -> if(other.backups[i] != b) return false
            }
            return true
        }
    }

    private val internalBackupApplicationLiveData = MediatorLiveData<BackupApplicationDataList>()
    private lateinit var pfaList : List<PFApplicationHelper.PFAInfo>

    val appLiveData: LiveData<BackupApplicationDataList> = internalBackupApplicationLiveData

    init {
        viewModelScope.launch {
            pfaList = PFApplicationHelper.getAvailablePFAs(getApplication())
            getApplicationDataWithJobs()
        }
    }

    private fun getApplicationDataWithJobs() {
        // get jobs for them
        val db = BackupDatabase.getInstance(getApplication())
        val backupJobsLiveData = db.backupJobDao().getAllLive()
        val metadataLiveData = db.backupMetaDataDao().getAllLive()

        internalBackupApplicationLiveData.addSources(backupJobsLiveData, metadataLiveData) { jobs, metaData ->
            val applicationList = BackupApplicationDataList()

            for(pfa in pfaList) {
                val jobList = jobs?.filter { it.packageName == pfa.packageName } ?: emptyList()
                val metaList = metaData?.filter { it.packageName == pfa.packageName } ?: emptyList()
                applicationList.add(BackupApplicationData(pfa, jobList, metaList))
            }

            Log.d("PFABackupDebug", applicationList.toString())

            applicationList
        }
    }

    fun createBackupForPackage(packageName: String) {
        viewModelScope.launch {
            val jobManager = BackupJobManager.getInstance(getApplication())
            jobManager.createBackupJobChain(packageName)
        }
    }

    fun cancelRunningJobs(packageName: String) {
        viewModelScope.launch {
            val jobManager = BackupJobManager.getInstance(getApplication())
            jobManager.cancelAllJobs(packageName)
        }
    }

    fun restoreRecentBackup(packageName: String) {
        viewModelScope.launch {
            val jobManager = BackupJobManager.getInstance(getApplication())
            jobManager.createRestoreJobChain(packageName)
        }
    }
}