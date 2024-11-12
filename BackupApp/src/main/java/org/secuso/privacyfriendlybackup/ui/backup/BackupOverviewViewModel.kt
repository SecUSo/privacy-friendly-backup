package org.secuso.privacyfriendlybackup.ui.backup

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.collection.ArraySet
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository.BackupData
import org.secuso.privacyfriendlybackup.data.BackupJobManager
import org.secuso.privacyfriendlybackup.data.apps.PFApplicationHelper
import org.secuso.privacyfriendlybackup.data.exporter.DataExporter
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.ui.common.Mode
import java.io.ByteArrayInputStream
import java.util.Date

class BackupOverviewViewModel(app: Application) : AndroidViewModel(app) {
    private val internalBackupLiveData: MediatorLiveData<List<BackupData>> = MediatorLiveData()

    val backupLiveData: LiveData<List<BackupData>> = internalBackupLiveData
    val filterLiveData: LiveData<String> = MutableLiveData("")
    val filteredBackupLiveData: LiveData<List<BackupData>> = MutableLiveData(emptyList())
    val currentMode: LiveData<Mode> = MutableLiveData<Mode>(Mode.NORMAL)
    private val _exportStatus = MutableLiveData(ExportStatus(ExportStatus.Status.UNKNOWN, 0, 0))
    val exportStatus: LiveData<ExportStatus>
        get() = _exportStatus

    class ExportStatus(val status: ExportStatus.Status, val completed: Int, val total: Int) {
        enum class Status {
            UNKNOWN, LOADING, WRITING, ERROR, COMPLETE
        }
    }

    init {
        viewModelScope.launch {
            internalBackupLiveData.addSource(
                BackupDataStorageRepository.getInstance(app).listAvailableBackups(app)
            ) {
                internalBackupLiveData.value = it
                setFilterText(filterLiveData.value)
            }
            // build up cache
            PFApplicationHelper.getAvailablePFAs(app)
        }
    }

    fun getCurrentMode(): Mode {
        return currentMode.value!!
    }

    fun enableMode(mode: Mode) {
        if (!mode.isActiveIn(currentMode.value!!)) {
            (currentMode as MutableLiveData<Mode>).postValue(Mode[getCurrentMode().value or mode.value])
        }
    }

    fun disableMode(mode: Mode) {
        if (mode.isActiveIn(currentMode.value!!)) {
            (currentMode as MutableLiveData<Mode>).postValue(Mode[getCurrentMode().value and mode.value.inv()])
        }
    }

    fun setFilterText(filter: String?) {
        val result = ArrayList<BackupData>()

        val internalData = internalBackupLiveData.value ?: emptyList()

        for (data in internalData) {
            val pfaInfo = PFApplicationHelper.getDataForPackageName(getApplication(), data.packageName)
            if (filter.isNullOrEmpty()
                || data.packageName.contains(filter, true)
                || pfaInfo?.label?.contains(filter, true) == true
            ) {
                result.add(data)
            }
        }

        (filteredBackupLiveData as MutableLiveData<List<BackupData>>).postValue(result)

        if (filter != filterLiveData.value) {
            (filterLiveData as MutableLiveData<String>).postValue(filter)
        }
    }

    fun insertTestData() {
        viewModelScope.launch {
            for (i in 1..10) {
                val dataId = InternalBackupDataStoreHelper.storeData(
                    getApplication(),
                    "org.secuso.example",
                    ByteArrayInputStream("TestData_$i".toByteArray()),
                    Date(),
                    false
                )
                BackupDataStorageRepository.getInstance(getApplication()).storeFile(getApplication(), "org.secuso.example", dataId)
            }
        }
    }

    fun deleteData(deleteList: Set<BackupData>) {
        viewModelScope.launch {
            val repo = BackupDataStorageRepository.getInstance(getApplication())
            repo.deleteFiles(getApplication(), deleteList.map { it.id })
        }
    }

    fun restoreBackup(backupData: BackupData) {
        viewModelScope.launch {
            val jobManager = BackupJobManager.getInstance(getApplication())
            jobManager.createRestoreJobChain(backupData.packageName, backupData.id)
        }
    }

    fun exportData(uri: Uri?, selectionList: Set<BackupData>, exportEncrypted: Boolean) {
        _exportStatus.postValue(ExportStatus(ExportStatus.Status.UNKNOWN, 0, selectionList.size))
        val write = viewModelScope.async(IO) {
            val storageRepository = BackupDataStorageRepository.getInstance(getApplication())
            return@async uri?.let {
                //Create BackupData objects to export
                val exportBackupData = ArraySet<BackupData>()
                _exportStatus.postValue(ExportStatus(ExportStatus.Status.LOADING, 0, selectionList.size))
                for (backupData in selectionList) {
                    val encrypted = exportEncrypted && backupData.encrypted

                    val fullBackupData = storageRepository.getFile(getApplication(), backupData.id)
                    if (fullBackupData?.data == null) {
                        return@let false
                    }

                    val exportData = BackupData(
                        filename = DataExporter.getSingleExportFileName(fullBackupData, exportEncrypted),
                        encrypted = encrypted,
                        timestamp = fullBackupData.timestamp,
                        packageName = fullBackupData.packageName,
                        available = true,
                        data = if (encrypted) fullBackupData.data else
                            String(fullBackupData.data).toByteArray(Charsets.UTF_8),//TODO decrypt data and get decrypted data
                        storageType = StorageType.EXTERNAL
                    )
                    exportBackupData.add(exportData)
                    _exportStatus.postValue(ExportStatus(ExportStatus.Status.LOADING, exportBackupData.size, selectionList.size))
                }
                _exportStatus.postValue(ExportStatus(ExportStatus.Status.WRITING, 0, selectionList.size))
                return@let DataExporter.exportDataZip(getApplication(), uri, exportBackupData)
            }
        }

        viewModelScope.launch(Main) {
            if (write.await() == true) {
                _exportStatus.postValue(ExportStatus(ExportStatus.Status.COMPLETE, selectionList.size, selectionList.size))
            } else {
                _exportStatus.postValue(ExportStatus(ExportStatus.Status.ERROR, selectionList.size, selectionList.size))
            }
        }
    }
}
