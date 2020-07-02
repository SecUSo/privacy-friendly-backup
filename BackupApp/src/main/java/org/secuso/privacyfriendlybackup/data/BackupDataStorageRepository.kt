package org.secuso.privacyfriendlybackup.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.cloud.WebserviceProvider
import java.util.*

class BackupDataStorageRepository(
    private val webserviceProvider : WebserviceProvider,
    private val database : BackupDatabase
) {

    enum class StorageType {
        INTERNAL,
        EXTERNAL,
        CLOUD
    }

    data class BackupData(
        val filename : String,
        val packageName : String,
        val timestamp : Date,
        val data : ByteArray,
        val encrypted : Boolean
    )

    suspend fun storeFile(data : BackupData) {

    }

    suspend fun getFile() : LiveData<BackupData> {
        val data = MutableLiveData<BackupData>()

        // TODO

        return data
    }

    suspend fun listAvailableBackups() : LiveData<List<BackupData>> {
        val data = MutableLiveData<List<BackupData>>()

        // TODO

        return data
    }


}