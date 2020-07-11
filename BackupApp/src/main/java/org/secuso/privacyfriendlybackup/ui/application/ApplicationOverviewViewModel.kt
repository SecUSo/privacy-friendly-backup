package org.secuso.privacyfriendlybackup.ui.application

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.data.apps.PFApplicationHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob

class ApplicationOverviewViewModel(app : Application) : AndroidViewModel(app) {

    class BackupApplicationDataList : ArrayList<BackupApplicationData>() {

    }

    data class BackupApplicationData(
        val pfaInfo: PFApplicationHelper.PFAInfo,
        val id: Long,
        val jobs: List<BackupJob>
    )

    private val internalBackupApplicationLiveData = MediatorLiveData<List<BackupApplicationData>>()
    private lateinit var pfaList : List<PFApplicationHelper.PFAInfo>

    val appLiveData: LiveData<List<BackupApplicationData>> = internalBackupApplicationLiveData

    init {
        viewModelScope.launch {
            pfaList = PFApplicationHelper.getAvailablePFAs(getApplication())
            getApplicationDataWithJobs()
        }
    }

    private fun getApplicationDataWithJobs() {
        // get jobs for them
        val dao = BackupDatabase.getInstance(getApplication()).backupJobDao()
        val backupJobsLiveData = dao.getAllLive()

        internalBackupApplicationLiveData.addSource(backupJobsLiveData) { list ->
            val applicationList = BackupApplicationDataList()

            for(pfa in pfaList) {
                val jobList = list.filter { it.packageName == pfa.packageName }
                val pfaId = pfa.packageName.hashCode().toLong()
                // jobList.map { it to pfaId.shl(32).or(it._id) }
                applicationList.add(
                    BackupApplicationData(pfa, pfaId, jobList)
                )
            }

            internalBackupApplicationLiveData.postValue(applicationList)
        }
    }


}