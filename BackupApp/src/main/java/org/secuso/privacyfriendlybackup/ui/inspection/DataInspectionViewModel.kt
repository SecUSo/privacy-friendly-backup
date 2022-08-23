package org.secuso.privacyfriendlybackup.ui.inspection

import android.app.Application
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.api.util.readString
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.cache.DecryptionCache
import org.secuso.privacyfriendlybackup.data.cache.DecryptionMetaData
import org.secuso.privacyfriendlybackup.data.exporter.DataExporter
import org.secuso.privacyfriendlybackup.data.importer.DataImporter
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.worker.BackupJobManagerWorker
import java.util.*

class DataInspectionViewModel(app : Application) : AndroidViewModel(app) {
    val TAG : String = "DataInspectionViewModel"

    private val backupDataLiveData = MutableLiveData<String>()
    private val loadStatusLiveData = MediatorLiveData<LoadStatus>().apply { postValue(LoadStatus.UNKNOWN) }
    private val decryptionMetaLiveData = MutableLiveData<DecryptionMetaData?>()
    private var dataId = -1L
    private var localLoadedDataId = -1L
    private var fileName : String = ""
    private var encryptedFileName : String = ""
    private var backupMetaData : BackupDataStorageRepository.BackupData? = null
    var isEncrypted : Boolean = false
        private set

    private var backupData : String = ""

    fun getFileName(encrypted: Boolean) : String {
        return if(backupMetaData?.encrypted == true && encrypted) {
            getEncryptedFilename(fileName)
        } else {
            getUnencryptedFilename(fileName)
        }
    }

    fun getLoadStatus() : LiveData<LoadStatus> {
        return loadStatusLiveData
    }

    fun getDecryptionMetaData() : LiveData<DecryptionMetaData?> = decryptionMetaLiveData

    fun loadData(dataId: Long) {
        this.dataId = dataId

        viewModelScope.launch(IO) {
            val storageRepository = BackupDataStorageRepository.getInstance(getApplication())

            backupMetaData = storageRepository.getFile(getApplication(), dataId)
            val data = backupMetaData
            if(data?.data == null) {
                loadStatusLiveData.postValue(LoadStatus.ERROR)
                return@launch
            }

            fileName = data.filename

            if(data.encrypted) {
                isEncrypted = true
                val internalDataId = InternalBackupDataStoreHelper.storeData(getApplication(), data.packageName, data.data.inputStream(), data.timestamp, data.encrypted)
                encryptedFileName = InternalBackupDataStoreHelper.getInternalDataFileName(getApplication(), internalDataId)
                val fileName = getUnencryptedFilename(encryptedFileName)

                val job = BackupJob(
                    packageName = data.packageName,
                    timestamp = data.timestamp,
                    action = BackupJobAction.BACKUP_DECRYPT,
                    dataId = internalDataId
                )

                val work = BackupJobManagerWorker.createEncryptionWorkRequest(
                    job,
                    false,
                    PreferenceManager.getDefaultSharedPreferences(getApplication())
                )

                val cont = WorkManager.getInstance(getApplication()).beginUniqueWork("DATA_INSPECTION", ExistingWorkPolicy.REPLACE, work)
                val workInfoLiveData = cont.workInfosLiveData
                cont.enqueue()
                withContext(Main) {
                    loadStatusLiveData.addSource(workInfoLiveData) { workInfoList ->
                        Log.d(TAG, "### workInfoList State changed!")
                        if (workInfoList.isNotEmpty()) {
                            val workInfo = workInfoList[0]
                            when (workInfo.state) {
                                WorkInfo.State.RUNNING -> {
                                    loadStatusLiveData.postValue(LoadStatus.DECRYPTING)
                                }
                                WorkInfo.State.SUCCEEDED -> {
                                    Log.d(TAG, "### State: SUCCEEDED")
                                    loadStatusLiveData.postValue(LoadStatus.LOADING)
                                    loadStatusLiveData.removeSource(workInfoLiveData)
                                    viewModelScope.launch {
                                        Log.d(TAG, "### Getting internal data for file: $fileName")
                                        val (decryptedData, metaData) = InternalBackupDataStoreHelper.getInternalData(
                                            getApplication(),
                                            fileName
                                        )
                                        if (decryptedData == null || metaData == null) {
                                            Log.d(TAG, "### decrypted data is null?")
                                            loadStatusLiveData.postValue(LoadStatus.DECRYPTION_ERROR)
                                            return@launch
                                        }

                                        setBackupData(decryptedData.readString())
                                        setDecryptionMetaData(DecryptionCache.getDecryptionMetaData(metaData))
                                    }
                                }
                                WorkInfo.State.BLOCKED -> {
                                    loadStatusLiveData.postValue(LoadStatus.DECRYPTION_ERROR)
                                }
                                WorkInfo.State.CANCELLED -> {
                                    loadStatusLiveData.postValue(LoadStatus.DECRYPTION_ERROR)
                                    loadStatusLiveData.removeSource(workInfoLiveData)
                                }
                                WorkInfo.State.ENQUEUED -> { /* DO NOTHING - STILL WAITING */
                                }
                                WorkInfo.State.FAILED -> {
                                    loadStatusLiveData.postValue(LoadStatus.DECRYPTION_ERROR)
                                }
                            }
                        }
                    }
                }
            } else {
                setBackupData(String(data.data))
            }
        }
    }

    private fun setDecryptionMetaData(decryptionMetaData: DecryptionMetaData?) {
        decryptionMetaData ?: return

        decryptionMetaLiveData.postValue(decryptionMetaData)
    }

    private fun getUnencryptedFilename(filename: String) =
        filename.replace("_encrypted.backup", ".backup")

    private fun getEncryptedFilename(filename: String) : String {
        return if(filename.contains("_encrypted.backup", true)) {
            filename
        } else {
            filename.replace(".backup", "_encrypted.backup", true)
        }
    }



    private fun setBackupData(backupDataString: String) {
        backupData = backupDataString

        if(!TextUtils.isEmpty(backupData)) {

            // short validity check if it is valid json
            if(DataImporter.isValidJSON(backupDataString)) {
                backupDataLiveData.postValue(backupData)
                loadStatusLiveData.postValue(LoadStatus.DONE)
            } else {
                loadStatusLiveData.postValue(LoadStatus.ERROR_INVALID_JSON)
                // JSON STRING INVALID
            }
        }
    }

    fun getData(): LiveData<String> {
        return backupDataLiveData
    }

    override fun onCleared() {
        super.onCleared()

        if(localLoadedDataId != -1L) {
            viewModelScope.launch {
                InternalBackupDataStoreHelper.clearData(getApplication(), localLoadedDataId)
                Log.d(TAG, "## cleared internal data for id $localLoadedDataId")
            }
        }
    }

    fun exportData(uri: Uri?, exportEncrypted : Boolean) {
        val write = viewModelScope.async(IO) {
            return@async uri?.let {
                if(backupMetaData == null) return@let false

                val encrypted = exportEncrypted && backupMetaData!!.encrypted

                val backupData = BackupDataStorageRepository.BackupData(
                    filename = getFileName(exportEncrypted),
                    encrypted = encrypted,
                    timestamp = backupMetaData!!.timestamp,
                    packageName = backupMetaData!!.packageName,
                    available = true,
                    data = if(encrypted) backupMetaData!!.data else backupData.toByteArray(Charsets.UTF_8),
                    storageType = StorageType.EXTERNAL
                )

                return@let DataExporter.exportData(getApplication(), uri, backupData)
            }
        }

        viewModelScope.launch(Main) {
            if (write.await() == true) {
                Toast.makeText(getApplication(), "saved file", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
    }
}