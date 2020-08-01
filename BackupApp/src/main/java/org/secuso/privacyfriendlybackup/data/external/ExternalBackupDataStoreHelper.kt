package org.secuso.privacyfriendlybackup.data.external

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.api.util.toHex
import org.secuso.privacyfriendlybackup.api.util.copyInputStreamToFile
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.util.BackupDataUtil.getFileName
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*

object ExternalBackupDataStoreHelper {

    const val BACKUP_DIR = "backupData"
    const val TAG = "PFA External"

    suspend fun storeData(context: Context, data: BackupDataStorageRepository.BackupData) {
        withContext(Dispatchers.IO) {
            val path = File(context.getExternalFilesDir(null), BACKUP_DIR)
            path.mkdirs()
            val file = File(path, data.filename)

            file.copyInputStreamToFile(ByteArrayInputStream(data.data))
            val hash = data.data!!.toHex()

            BackupDatabase.getInstance(context).backupMetaDataDao().insert(StoredBackupMetaData(
                packageName = data.packageName,
                timestamp = data.timestamp,
                storageService = StorageType.EXTERNAL,
                filename = data.filename,
                encrypted = data.encrypted,
                hash = hash
            ))
        }
    }

    suspend fun storeData(context: Context, packageName: String, dataId : Long) : Long {
        return withContext(Dispatchers.IO) {
            val path = File(context.getExternalFilesDir(null), BACKUP_DIR)
            path.mkdirs()
            val date = Date()
            val filename = getFileName(date, packageName)
            val file = File(path, filename)

            Log.d(TAG, file.toString())

            var hash : String? = null

            val (inputStream, data) = InternalBackupDataStoreHelper.getInternalData(context, dataId)
            inputStream?.use {
                file.copyInputStreamToFile(inputStream)
                hash = inputStream.readBytes().toHex()
            }

            BackupDatabase.getInstance(context).backupMetaDataDao().insert(StoredBackupMetaData(
                packageName = data.packageName,
                timestamp = date,
                storageService = StorageType.EXTERNAL,
                filename = filename,
                encrypted = data.encrypted,
                hash = hash!!
            ))
        }
    }

    suspend fun getData(context: Context, metadata : StoredBackupMetaData) : BackupDataStorageRepository.BackupData? {
        return withContext(Dispatchers.IO) {
            val path = File(context.getExternalFilesDir(null), BACKUP_DIR)
            val file = File(path, metadata.filename)

            return@withContext BackupDataStorageRepository.BackupData(
                metadata._id,
                metadata.filename,
                metadata.packageName,
                metadata.timestamp,
                file.inputStream().readBytes(),
                metadata.encrypted,
                StorageType.EXTERNAL,
                true
            )
        }
    }

    suspend fun deleteData(context: Context, metadata : StoredBackupMetaData) {
        withContext(Dispatchers.IO) {
            val path = File(context.getExternalFilesDir(null), BACKUP_DIR)
            val file = File(path, metadata.filename)
            file.delete()
        }
    }

    suspend fun listAvailableData(context: Context) : List<String> {
        return withContext(Dispatchers.IO) {
            val files = File(context.getExternalFilesDir(null), BACKUP_DIR).listFiles { _, name -> name.toLowerCase(Locale.ENGLISH).endsWith(".backup") }
            files?.map { it.name } ?: emptyList()
        }
    }
}