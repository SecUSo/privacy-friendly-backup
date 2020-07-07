package org.secuso.privacyfriendlybackup.ui

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository.BackupData
import org.secuso.privacyfriendlybackup.data.apps.PFApplicationHelper
import org.secuso.privacyfriendlybackup.data.cloud.WebserviceProvider
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.ui.BackupOverviewFragment.Mode
import java.io.ByteArrayInputStream

class BackupOverviewViewModel(app : Application) : AndroidViewModel(app) {

    private val internalBackupLiveData : MediatorLiveData<List<BackupData>> = MediatorLiveData()

    val backupLiveData : LiveData<List<BackupData>> = internalBackupLiveData
    val filterLiveData : LiveData<String> = MutableLiveData("")
    val filteredBackupLiveData : LiveData<List<BackupData>> = MutableLiveData(emptyList())
    val currentMode : LiveData<Mode> = MutableLiveData<Mode>(Mode.NORMAL)

    init {
        viewModelScope.launch {
            internalBackupLiveData.addSource(
                BackupDataStorageRepository(WebserviceProvider(), BackupDatabase.getInstance(app)).listAvailableBackups(app)
            ) {
                internalBackupLiveData.value = it
                setFilterText(filterLiveData.value)
            }
            // build up cache
            PFApplicationHelper.getAvailablePFAs(app)
        }
    }

    fun getCurrentMode() : Mode {
        return currentMode.value!!
    }

    fun enableMode(mode: Mode) {
        if(!mode.isActiveIn(currentMode.value!!)) {
            (currentMode as MutableLiveData<Mode>).postValue(Mode[getCurrentMode().value or mode.value])
        }
    }
    fun disableMode(mode: Mode) {
        if(mode.isActiveIn(currentMode.value!!)) {
            (currentMode as MutableLiveData<Mode>).postValue(Mode[getCurrentMode().value and mode.value.inv()])
        }
    }

    fun setFilterText(filter : String?) {
        val result = ArrayList<BackupData>()

        val internalData = internalBackupLiveData.value ?: emptyList()

        for(data in internalData) {
            if(filter.isNullOrEmpty() || data.packageName.contains(filter, true)) {
                result.add(data)
            }
        }

        (filteredBackupLiveData as MutableLiveData<List<BackupData>>).postValue(result)

        if(filter != filterLiveData.value) {
            (filterLiveData as MutableLiveData<String>).postValue(filter)
        }
    }

    fun insertTestData() {
        viewModelScope.launch {
            for(i in 1 .. 10) {
                val dataId = InternalBackupDataStoreHelper.storeData(
                    getApplication(),
                    "org.secuso.example",
                    ByteArrayInputStream("TestData_$i".toByteArray()),
                    false
                )
                BackupDataStorageRepository(
                    WebserviceProvider(),
                    BackupDatabase.getInstance(getApplication())
                ).storeFile(getApplication(), "org.secuso.example", dataId)
            }
        }
    }
}
