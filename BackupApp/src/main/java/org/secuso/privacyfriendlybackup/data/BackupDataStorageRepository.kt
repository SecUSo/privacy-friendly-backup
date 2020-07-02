package org.secuso.privacyfriendlybackup.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.cloud.WebserviceProvider
import org.secuso.privacyfriendlybackup.data.external.ExternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import java.util.*

class BackupDataStorageRepository(
    private val webserviceProvider : WebserviceProvider,
    private val database : BackupDatabase
) {

    data class BackupData(
        val filename : String,
        val packageName : String,
        val timestamp : Date,
        val data : ByteArray?,
        val encrypted : Boolean,
        val storageType : StorageType
    )

    suspend fun storeFile(context: Context, packageName: String, dataId : Long, storageType: StorageType = StorageType.EXTERNAL) {
        when(storageType) {
            StorageType.EXTERNAL -> ExternalBackupDataStoreHelper.storeData(context, packageName, dataId)
            StorageType.CLOUD -> { TODO() }
        }
    }

    suspend fun getFile(context: Context, metadataId : Long) : LiveData<BackupData> {
        val data = MutableLiveData<BackupData>()

        withContext(IO) {
            val metadata = database.backupMetaDataDao().getFromId(metadataId)
            if (metadata != null) {
                when (metadata.storageService) {
                    StorageType.EXTERNAL -> {
                        data.postValue(ExternalBackupDataStoreHelper.getData(context,metadata))
                    }
                    StorageType.CLOUD -> {
                        data.postValue( TODO() )
                    }
                }
            }
        }

        return data
    }

    suspend fun listAvailableBackups(context: Context) : LiveData<List<Pair<BackupData, Boolean>>> {
        val livedata = MutableLiveData<List<Pair<BackupData, Boolean>>>()

        val result : MutableList<Pair<BackupData, Boolean>> = ArrayList()

        withContext(IO) {
            val metaList = database.backupMetaDataDao().getAll()
            val externalFilenames = ExternalBackupDataStoreHelper.listAvailableData(context)
            for(meta in metaList) {
                when(meta.storageService) {
                    StorageType.EXTERNAL -> {
                        val available = externalFilenames.contains(meta.filename)
                        val backupDataInfo = BackupData(
                            filename = meta.filename,
                            packageName = meta.packageName,
                            timestamp = meta.timestamp,
                            data = null,
                            encrypted  = meta.encrypted,
                            storageType = meta.storageService
                        )
                        result.add(backupDataInfo to available)
                    }
                    StorageType.CLOUD -> {
                        TODO()
                    }
                }
            }
            livedata.postValue(result)
        }

        return livedata
    }


}