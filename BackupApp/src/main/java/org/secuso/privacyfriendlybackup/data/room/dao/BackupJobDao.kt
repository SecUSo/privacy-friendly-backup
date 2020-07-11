package org.secuso.privacyfriendlybackup.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob

@Dao
interface BackupJobDao {
    @Query("SELECT * FROM BackupJob")
    suspend fun getAll() : List<BackupJob>

    @Query("SELECT * FROM BackupJob")
    fun getAllLive() : LiveData<List<BackupJob>>

    @Query("SELECT * FROM BackupJob WHERE _id = :id")
    suspend fun getJobForId(id : Long) : BackupJob

    @Query("SELECT * FROM BackupJob WHERE packageName = :packageName")
    suspend fun getJobsForPackage(packageName: String) : List<BackupJob>

    @Query("SELECT * FROM BackupJob WHERE packageName = :packageName")
    fun getJobsForPackageLiveData(packageName: String) : LiveData<List<BackupJob>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: BackupJob) : Long

    @Update
    suspend fun update(data: BackupJob)

    @Query("DELETE FROM BackupJob WHERE _id = :id")
    suspend fun deleteForId(id: Long)

    @Query("DELETE FROM BackupJob WHERE packageName = :packageName AND `action` == :action")
    suspend fun deleteJobForPackage(packageName: String?, action: String)
}
