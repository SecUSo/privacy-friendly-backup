package org.secuso.privacyfriendlybackup.database.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.secuso.privacyfriendlybackup.database.room.model.BackupJob

@Dao
interface BackupJobDao {
    @Query("SELECT * FROM BackupJob")
    suspend fun getAll() : List<BackupJob>

    @Query("SELECT * FROM BackupJob WHERE uid = :uid")
    suspend fun getJobsForUid(uid : Int) : List<BackupJob>

    @Query("SELECT * FROM BackupJob WHERE packageName = :packageName")
    suspend fun getJobsForPackage(packageName: String) : List<BackupJob>

    @Query("SELECT * FROM BackupJob WHERE uid = :uid")
    suspend fun getJobsForUidLiveData(uid : Int) : LiveData<List<BackupJob>>

    @Query("SELECT * FROM BackupJob WHERE packageName = :packageName")
    suspend fun getJobsForPackageLiveData(packageName: String) : LiveData<List<BackupJob>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: BackupJob)

    @Query("DELETE FROM BackupJob WHERE uid = :uid")
    suspend fun deleteForUid(uid: Int)

    @Query("DELETE FROM BackupJob WHERE packageName = :packageName AND `action` == :action")
    suspend fun deleteJobForPackage(packageName: String?, action: String)
}