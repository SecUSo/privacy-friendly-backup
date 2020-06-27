package org.secuso.privacyfriendlybackup.database.file

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.secuso.privacyfriendlybackup.api.util.copyInputStreamToFile
import org.secuso.privacyfriendlybackup.database.room.BackupDatabase
import org.secuso.privacyfriendlybackup.database.room.model.InternalBackupData
import org.secuso.privacyfriendlybackup.services.BackupService
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker
import org.secuso.privacyfriendlybackup.worker.StoreWorker
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*

object BackupDataStoreHelper {
    const val TAG = "BackupDataStoreHelper"

    const val BACKUP_DIR = "backupData"

    suspend fun storeBackupData(context: Context, uid: Int, packageName: String, inputStream: InputStream) {
        val date = Date()
        val dateString: CharSequence = DateFormat.format("yyyy_MM_dd", date.time)

        val path = File(context.filesDir, BACKUP_DIR)
        path.mkdirs()
        val fileName = "${packageName}_${dateString}.backup"
        File(path, fileName).copyInputStreamToFile(inputStream)

        Log.d(TAG, "Saved $fileName")

        // save filename into db
        val data = InternalBackupData(
            uid = uid,
            packageName = packageName,
            timestamp = date,
            file = fileName,
            encrypted = false
        )
        val dataId = BackupDatabase.getInstance(context).internalBackupDataDao().insert(data)

        // create store and encryption worker here
        // TODO: check settings and only enable encryption if properly setup
        val encryption = false
        if(encryption) {
            val encryptionWorker = OneTimeWorkRequestBuilder<EncryptionWorker>().setInputData(workDataOf("dataId" to dataId)).build()
            val storeWorker = OneTimeWorkRequestBuilder<StoreWorker>().build()
            WorkManager.getInstance(context).beginUniqueWork("$packageName($dataId)", ExistingWorkPolicy.REPLACE, encryptionWorker).then(storeWorker).enqueue()
        } else {
            val storeWorker = OneTimeWorkRequestBuilder<StoreWorker>().setInputData(workDataOf("dataId" to dataId)).build()
            WorkManager.getInstance(context).beginUniqueWork("$packageName($dataId)", ExistingWorkPolicy.REPLACE, storeWorker).enqueue()
        }
    }

    suspend fun getRestoreData(context: Context, callingUid: Int, callingPackageName: String, dataId: Int): InputStream? {
        val data = BackupDatabase.getInstance(context).internalBackupDataDao().getById(dataId)

        if(data.packageName != callingPackageName && data.uid == callingUid) {
            Log.d(TAG, "[No Restore Data found.]")
            return null
        }

        val path = File(context.filesDir, BACKUP_DIR)
        return File(path, data.file).inputStream()
    }

}