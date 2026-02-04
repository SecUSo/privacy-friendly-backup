package org.secuso.privacyfriendlybackup.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.data.external.ExternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType

class SettingsViewmodel(app: Application) : AndroidViewModel(app) {

    fun moveBackups(location: StorageType) {
        viewModelScope.launch {
            val data = BackupDatabase.getInstance(getApplication()).backupMetaDataDao().getAllOfOtherStorageType(location)

            when (location) {
                StorageType.INTERNAL -> {
                    data.forEach { metadata ->
                        val backupData = ExternalBackupDataStoreHelper.getData(getApplication(), metadata)
                        InternalBackupDataStoreHelper.storeData(getApplication(), backupData!!)
                        ExternalBackupDataStoreHelper.deleteData(getApplication(), metadata)
                    }
                }
                StorageType.EXTERNAL -> {
                    data.forEach { metadata ->
                        val backupData = InternalBackupDataStoreHelper.getData(getApplication(), metadata)
                        ExternalBackupDataStoreHelper.storeData(getApplication(), backupData!!)
                        InternalBackupDataStoreHelper.deleteData(getApplication(), metadata)
                    }
                }
                else -> {}
            }
        }
    }

}
