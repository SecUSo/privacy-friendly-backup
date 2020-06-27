package org.secuso.privacyfriendlybackup.database.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.secuso.privacyfriendlybackup.database.room.model.StoredBackupMetaData

@Dao
interface BackupMetaDataDao {
    @Query("SELECT * FROM StoredBackupMetaData")
    suspend fun getAll() : List<StoredBackupMetaData>

    @Query("SELECT * FROM StoredBackupMetaData WHERE uid = :uid")
    suspend fun getFromUid(uid : Int) : List<StoredBackupMetaData>

    @Query("SELECT * FROM StoredBackupMetaData WHERE packageName = :packageName")
    suspend fun getFromPackage(packageName: String) : List<StoredBackupMetaData>

    @Query("SELECT * FROM StoredBackupMetaData WHERE uid = :uid")
    suspend fun getFromUidLiveData(uid : Int) : LiveData<List<StoredBackupMetaData>>

    @Query("SELECT * FROM StoredBackupMetaData WHERE packageName = :packageName")
    suspend fun getFromPackageLiveData(packageName: String) : LiveData<List<StoredBackupMetaData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: StoredBackupMetaData)

    @Query("DELETE FROM StoredBackupMetaData WHERE uid = :uid")
    suspend fun deleteForUid(uid: Int)
}