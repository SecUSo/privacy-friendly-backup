package org.secuso.privacyfriendlybackup.data.external

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.api.util.toHex
import org.secuso.privacyfriendlybackup.api.util.copyInputStreamToFile
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData
import org.secuso.privacyfriendlybackup.util.BackupDataUtil.getFileName
import java.io.File
import java.util.*

object ExternalBackupDataStoreHelper {

    const val BACKUP_DIR = "backupData"

    suspend fun storeData(context: Context, packageName: String, dataId : Long) : Long {
        return withContext(Dispatchers.IO) {
            val path = File(context.getExternalFilesDir(null), BACKUP_DIR)
            path.mkdirs()
            val date = Date()
            val filename = getFileName(date, packageName)
            val file = File(path, filename)
            var hash : String? = null

            val (inputStream, data) = InternalBackupDataStoreHelper.getInternalData(context, dataId)
            if (inputStream != null) {
                file.copyInputStreamToFile(inputStream)
                hash = inputStream.readBytes().toHex()
            }

            BackupDatabase.getInstance(context).backupMetaDataDao().insert(StoredBackupMetaData(
                packageName = packageName,
                timestamp = date,
                storageService = BackupDataStorageRepository.StorageType.INTERNAL.name,
                filename = filename,
                encrypted = data.encrypted,
                hash = hash!!
            ))
        }
    }

    suspend fun getData(context: Context, dataId : Long) : BackupDataStorageRepository.BackupData {
        return withContext(Dispatchers.IO) {
            val metaData = BackupDatabase.getInstance(context).backupMetaDataDao().getFromId(dataId)
            val path = File(context.getExternalFilesDir(null), BACKUP_DIR)
            val file = File(path, metaData.filename)

            return@withContext BackupDataStorageRepository.BackupData(
                metaData.filename,
                metaData.packageName,
                metaData.timestamp,
                file.inputStream().readBytes(),
                metaData.encrypted
            )
        }
    }

    suspend fun listAvailableData(context: Context) : List<String> {
        return withContext(Dispatchers.IO) {
            val files = File(context.getExternalFilesDir(null), BACKUP_DIR).listFiles { _, name -> name.toLowerCase(Locale.ENGLISH).endsWith(".backup") }
            files?.map { it.name } ?: emptyList()
        }
    }

}