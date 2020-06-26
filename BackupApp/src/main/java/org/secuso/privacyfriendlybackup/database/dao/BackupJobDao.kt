package org.secuso.privacyfriendlybackup.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.secuso.privacyfriendlybackup.database.model.BackupJob

@Dao
interface BackupJobDao {
    @Query("SELECT * FROM BackupJob")
    suspend fun getAll() : List<BackupJob>

    @Query("SELECT * FROM BackupJob WHERE uid = :uid")
    suspend fun getBackupJobsForUid(uid : Int) : List<BackupJob>

    @Query("SELECT * FROM BackupJob WHERE packageName = :packageName")
    suspend fun getBackupJobsForPackage(packageName: String) : List<BackupJob>

    @Query("SELECT * FROM BackupJob WHERE uid = :uid")
    suspend fun getBackupJobsForUidLiveData(uid : Int) : LiveData<List<BackupJob>>

    @Query("SELECT * FROM BackupJob WHERE packageName = :packageName")
    suspend fun getBackupJobsForPackageLiveData(packageName: String) : LiveData<List<BackupJob>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: BackupJob)

    @Query("DELETE FROM BackupJob WHERE uid = :uid")
    suspend fun deleteForUid(uid: Int)
}