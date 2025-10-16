package org.secuso.privacyfriendlybackup.data.internal

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.api.util.copyInputStreamToFile
import org.secuso.privacyfriendlybackup.api.util.hash
import org.secuso.privacyfriendlybackup.api.util.toHex
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.data.room.model.InternalBackupData
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.util.BackupDataUtil.getFileName
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

object InternalBackupDataStoreHelper {
    const val TAG = "PFA Internal"

    const val BACKUP_DIR = "tempData"
    const val INTERNAL_BACKUP_DIR = "backupData"

    suspend fun storeBackupData(context: Context, packageName: String, inputStream: InputStream, date: Date, encrypted: Boolean = false) : Long {
        val dataId = storeData(context, packageName, inputStream, date, encrypted)

        val backupJobDao = BackupDatabase.getInstance(context).backupJobDao()

        // delete corresponding backup job and update the dataID of the next
        val pfaJobs = backupJobDao.getJobsForPackage(packageName)
        val pfaJob = pfaJobs.find { it.action == BackupJobAction.PFA_JOB_BACKUP }
        if(pfaJob != null) {
            val nextJob = pfaJobs.find { it._id == pfaJob.nextJob }
            if(nextJob != null) {
                nextJob.dataId = dataId
                Log.d(TAG, "Deleting job with id ${pfaJob._id}")
                backupJobDao.deleteForId(pfaJob._id)

                Log.d(TAG, "Updating job with id ${nextJob._id}")
                backupJobDao.update(nextJob)
                return dataId
            }
        }

        return dataId
    }

    suspend fun storeData(context: Context, packageName: String, dataId: Long) : Long {
        return withContext(Dispatchers.IO) {
            val path = File(context.filesDir, INTERNAL_BACKUP_DIR)
            path.mkdirs()

            val date = Date()

            val (inputStream, data) = getInternalData(context, dataId)
            val fileName = getFileName(date, packageName, data?.encrypted == true)
            val file = File(path, fileName)

            Log.d(TAG, file.toString())

            val dataBytes = inputStream?.use {
                return@use inputStream.readBytes()
            }
            file.copyInputStreamToFile(ByteArrayInputStream(dataBytes))

            if (data != null) {
                val hash = dataBytes!!.hash("SHA-1").toHex()
                BackupDatabase.getInstance(context).backupMetaDataDao().insert(
                    StoredBackupMetaData(
                        packageName = data.packageName,
                        timestamp = date,
                        storageService = StorageType.INTERNAL,
                        filename = fileName,
                        encrypted = data.encrypted,
                        hash = hash
                    )
                )
            } else {
                -1L
            }
        }
    }

    suspend fun storeData(context: Context, packageName: String, inputStream: InputStream, date: Date, encrypted : Boolean = false) : Long {
        val path = File(context.filesDir, BACKUP_DIR)
        path.mkdirs()

        val fileName = getFileName(date, packageName, encrypted)
        File(path, fileName).copyInputStreamToFile(inputStream)

        Log.d(TAG, "Saved $fileName")

        // save filename into db
        val data = InternalBackupData(
            packageName = packageName,
            timestamp = date,
            file = fileName,
            encrypted = encrypted
        )
        return BackupDatabase.getInstance(context).internalBackupDataDao().insert(data)
    }

    suspend fun getData(context: Context, metadata : StoredBackupMetaData) : BackupDataStorageRepository.BackupData? {
        return withContext(Dispatchers.IO) {
            val path = File(context.filesDir, INTERNAL_BACKUP_DIR)
            val file = File(path, metadata.filename)

            return@withContext BackupDataStorageRepository.BackupData(
                metadata._id,
                metadata.filename,
                metadata.packageName,
                metadata.timestamp,
                file.inputStream().readBytes(),
                metadata.encrypted,
                StorageType.INTERNAL,
                true
            )
        }
    }

    suspend fun getInternalData(context: Context, dataId: Long): Pair<InputStream?, InternalBackupData?> {
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getById(dataId)
            ?: return Pair(null, null)

//        if(data.packageName != callingPackageName && data.uid == callingUid) {
//            Log.d(TAG, "[No Restore Data found.]")
//            return null
//        }

        val path = File(context.filesDir, BACKUP_DIR)
        return File(path, data.file).inputStream() to data
    }

    suspend fun getInternalDataFileName(context: Context, dataId: Long): String {
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getById(dataId)
            ?: return ""
        return data.file
    }

    suspend fun getInternalDataAsFile(context: Context, dataId: Long): Pair<File?, InternalBackupData?> {
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getById(dataId)
            ?: return Pair(null, null)

        val path = File(context.filesDir, BACKUP_DIR)
        return File(path, data.file) to data
    }

    suspend fun getInternalData(context: Context, filename: String): Pair<InputStream?, InternalBackupData?> {
        Log.d(TAG, "getInternalData(context, $filename)")
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getByFilename(filename)
            ?: return Pair(null, null)

//        if(data.packageName != callingPackageName && data.uid == callingUid) {
//            Log.d(TAG, "[No Restore Data found.]")
//            return null
//        }

        val path = File(context.filesDir, BACKUP_DIR)
        return File(path, data.file).inputStream() to data
    }

    suspend fun clearData(context: Context, dataId: Long) {
        Log.d(TAG, "clearData(context, $dataId)")
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getById(dataId)
            ?: return
        val file = File(data.file)

        try {
            file.delete()
            BackupDatabase.getInstance(context).internalBackupDataDao().delete(dataId)
            Log.d(TAG, "File(${file.absolutePath}) deleted.")
        } catch (e : IOException) {
            e.printStackTrace()
        }
    }

    suspend fun deleteData(context: Context, metadata : StoredBackupMetaData) {
        withContext(Dispatchers.IO) {
            val path = File(context.filesDir, INTERNAL_BACKUP_DIR)
            val file = File(path, metadata.filename)
            file.delete()
        }
    }

    suspend fun listAvailableData(context: Context) : List<String> {
        return withContext(Dispatchers.IO) {
            val files = File(context.filesDir, INTERNAL_BACKUP_DIR).listFiles { _, name ->
                name.lowercase(Locale.ENGLISH).endsWith(".backup")
            }
            files?.map { it.name } ?: emptyList()
        }
    }

}