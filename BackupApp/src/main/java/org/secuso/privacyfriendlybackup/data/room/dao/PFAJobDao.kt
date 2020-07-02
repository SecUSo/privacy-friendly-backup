package org.secuso.privacyfriendlybackup.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    suspend fun insert(data: PFAJob)

    @Query("DELETE FROM PFAJob WHERE uid = :uid")
    suspend fun deleteForUid(uid: Int)

    @Query("DELETE FROM PFAJob WHERE packageName = :packageName AND `action` == :action")
    suspend fun deleteJobForPackage(packageName: String?, action: String)
}