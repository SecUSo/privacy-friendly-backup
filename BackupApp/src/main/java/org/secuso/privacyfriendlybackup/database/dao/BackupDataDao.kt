package org.secuso.privacyfriendlybackup.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.secuso.privacyfriendlybackup.database.model.BackupData

@Dao
interface BackupDataDao {
    @Query("SELECT * FROM BackupData")
    suspend fun getAll() : List<BackupData>

    @Query("SELECT * FROM BackupData WHERE uid = :uid")
    suspend fun getBackupsForUid(uid : Int) : List<BackupData>

    @Query("SELECT * FROM BackupData WHERE packageName = :packageName")
    suspend fun getBackupsForPackage(packageName: String) : List<BackupData>

    @Query("SELECT * FROM BackupData WHERE uid = :uid")
    suspend fun getBackupsForUidLiveData(uid : Int) : LiveData<List<BackupData>>

    @Query("SELECT * FROM BackupData WHERE packageName = :packageName")
    suspend fun getBackupsForPackageLiveData(packageName: String) : LiveData<List<BackupData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: BackupData)

    @Query("DELETE FROM BackupData WHERE uid = :uid")
    suspend fun deleteForUid(uid: Int)
}