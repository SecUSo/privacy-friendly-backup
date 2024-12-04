package org.secuso.privacyfriendlybackup.data.cloud

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.api.util.copyInputStreamToFile
import org.secuso.privacyfriendlybackup.api.util.hash
import org.secuso.privacyfriendlybackup.api.util.toHex
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.cloud.drive.GoogleDriveHelper
import org.secuso.privacyfriendlybackup.data.external.ExternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.util.BackupDataUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class WebserviceProvider {

    companion object {
        val SERVICES = listOf(GoogleDriveHelper::class)
    }

    suspend fun storeData(context: Context, packageName: String, dataId : Long) : Long {
        return withContext(Dispatchers.IO) {
            var filename = ""
            var hash = ""

            val (internalFile, data) = InternalBackupDataStoreHelper.getInternalDataAsFile(context, dataId)

            if (data == null) return@withContext -1L

            if (internalFile != null) {
                // TODO: filename = GoogleDriveHelper.createFile(context, internalFile, data.encrypted)
                hash = internalFile.inputStream().readBytes().hash("SHA-1").toHex()
            }

            BackupDatabase.getInstance(context).backupMetaDataDao().insert(StoredBackupMetaData(
                packageName = data.packageName,
                timestamp = data.timestamp,
                storageService = StorageType.CLOUD,
                filename = filename,
                encrypted = data.encrypted,
                hash = hash
            ))
        }
    }

    suspend fun deleteData(context: Context, metadata : StoredBackupMetaData) {
        withContext(Dispatchers.IO) {
            // TODO: GoogleDriveHelper.deleteFile(context, metadata.filename)
        }
    }

    suspend fun getData(context: Context, metadata : StoredBackupMetaData) : BackupDataStorageRepository.BackupData? {
        return withContext(Dispatchers.IO) {
            return@withContext BackupDataStorageRepository.BackupData(
                metadata._id,
                metadata.filename,
                metadata.packageName,
                metadata.timestamp,
                ByteArray(0),// TODO: GoogleDriveHelper.readFile(context, metadata.filename).use { it.readBytes() },
                metadata.encrypted,
                StorageType.CLOUD,
                true
            )
        }
    }

    suspend fun listAvailableData(context: Context) : List<String> {
        return withContext(Dispatchers.IO) {
            val files = File(context.getExternalFilesDir(null),
                ExternalBackupDataStoreHelper.BACKUP_DIR
            ).listFiles { _, name -> name.toLowerCase(Locale.ENGLISH).endsWith(".backup") }
            files?.map { it.name } ?: emptyList()
        }
    }



}