package org.secuso.privacyfriendlybackup.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.cloud.WebserviceProvider
import org.secuso.privacyfriendlybackup.data.external.ExternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import java.util.*

class BackupDataStorageRepository(
    private val webserviceProvider : WebserviceProvider = WebserviceProvider(),
    private val database : BackupDatabase
) {

    data class BackupData(
        val id : Long = 0,
        val filename : String,
        val packageName : String,
        val timestamp : Date,
        val data : ByteArray?,
        val encrypted : Boolean,
        val storageType : StorageType,
        val available : Boolean = false
    )

    suspend fun storeFile(context: Context, packageName: String, dataId : Long, storageType: StorageType = StorageType.EXTERNAL) {
        when(storageType) {
            StorageType.EXTERNAL -> ExternalBackupDataStoreHelper.storeData(context, packageName, dataId)
            StorageType.CLOUD -> { TODO() }
        }
    }

    suspend fun isBackupAvailableForApp(context: Context, packageName: String) : Boolean {
        val metadata = database.backupMetaDataDao().getFromPackage(packageName)
        return metadata.isNotEmpty()
    }

    suspend fun getFileLiveData(context: Context, metadataId : Long) : LiveData<BackupData> {
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

    suspend fun getFile(context: Context, metadataId : Long) : BackupData? {
        val metadata = database.backupMetaDataDao().getFromId(metadataId)
            ?: throw IllegalArgumentException("Provided metadataId is not valid.")

        return when (metadata.storageService) {
            StorageType.EXTERNAL -> ExternalBackupDataStoreHelper.getData(context, metadata)
            StorageType.CLOUD -> {
                TODO()
            }
        }
    }

    suspend fun listAvailableBackups(context: Context) : LiveData<List<BackupData>> {
        val livedata = MediatorLiveData<List<BackupData>>()

        val metaListLiveData = database.backupMetaDataDao().getAllLive()
        livedata.addSource(metaListLiveData) { metalist ->
            val result : MutableList<BackupData> = ArrayList()

            runBlocking {
                val externalFilenames = ExternalBackupDataStoreHelper.listAvailableData(context)
                for(meta in metalist) {
                    when(meta.storageService) {
                        StorageType.EXTERNAL -> {
                            val available = externalFilenames.contains(meta.filename)
                            val backupDataInfo = BackupData(
                                id = meta._id,
                                filename = meta.filename,
                                packageName = meta.packageName,
                                timestamp = meta.timestamp,
                                data = null,
                                encrypted  = meta.encrypted,
                                storageType = meta.storageService,
                                available = available
                            )
                            result.add(backupDataInfo)
                        }
                        StorageType.CLOUD -> {
                            TODO()
                        }
                    }
                }
            }
            livedata.postValue(result)
        }

        return livedata
    }

    suspend fun deleteFiles(context: Context, metadataIds: List<Long>) {
        val metaDataList = database.backupMetaDataDao().getFromIds(metadataIds)

        for(metaData in metaDataList) {
            try {
                when (metaData.storageService) {
                    StorageType.EXTERNAL -> {
                        ExternalBackupDataStoreHelper.deleteData(context, metaData)
                    }
                    StorageType.CLOUD -> {
                        TODO("Not yet implemented.")
                    }
                }
            } catch (e : Exception) {
                // TODO: Error Handling - mark, which entry could not be deleted
                //  do not delete metaData of this entry
            }
        }

        database.backupMetaDataDao().deleteForIds(metadataIds)

    }


}