package org.secuso.privacyfriendlybackup.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.secuso.privacyfriendlybackup.data.room.model.PFAJob

@Dao
interface PFAJobDao {
    @Query("SELECT * FROM PFAJob")
    suspend fun getAll() : List<PFAJob>

    @Query("SELECT * FROM PFAJob WHERE uid = :uid")
    suspend fun getJobsForUid(uid : Int) : List<PFAJob>

    @Query("SELECT * FROM PFAJob WHERE packageName = :packageName")
    suspend fun getJobsForPackage(packageName: String) : List<PFAJob>

    @Query("SELECT * FROM PFAJob WHERE uid = :uid")
    fun getJobsForUidLiveData(uid : Int) : LiveData<List<PFAJob>>

    @Query("SELECT * FROM PFAJob WHERE packageName = :packageName")
    fun getJobsForPackageLiveData(packageName: String) : LiveData<List<PFAJob>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: PFAJob) : Long

    @Update
    suspend fun update(data: PFAJob)

    @Query("DELETE FROM PFAJob WHERE uid = :uid")
    suspend fun deleteForUid(uid: Int)

    @Query("DELETE FROM PFAJob WHERE packageName = :packageName")
    suspend fun deleteAllForPackage(packageName: String)

    @Query("DELETE FROM PFAJob WHERE packageName = :packageName AND `action` == :action")
    suspend fun deleteJobForPackage(packageName: String?, action: String)
}