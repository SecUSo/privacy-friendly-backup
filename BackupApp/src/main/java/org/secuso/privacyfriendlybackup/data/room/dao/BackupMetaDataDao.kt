package org.secuso.privacyfriendlybackup.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData

@Dao
interface BackupMetaDataDao {
    @Query("SELECT * FROM StoredBackupMetaData")
    suspend fun getAll() : List<StoredBackupMetaData>

    @Query("SELECT * FROM StoredBackupMetaData")
    fun getAllLive() : LiveData<List<StoredBackupMetaData>>

    @Query("SELECT * FROM StoredBackupMetaData WHERE _id = :id")
    suspend fun getFromId(id : Long) : StoredBackupMetaData?

    @Query("SELECT * FROM StoredBackupMetaData WHERE packageName = :packageName")
    suspend fun getFromPackage(packageName: String) : List<StoredBackupMetaData>

    @Query("SELECT * FROM StoredBackupMetaData WHERE packageName = :packageName")
    fun getFromPackageLiveData(packageName: String) : LiveData<List<StoredBackupMetaData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: StoredBackupMetaData) : Long

    @Query("DELETE FROM StoredBackupMetaData WHERE _id = :id")
    suspend fun deleteForUid(id: Long)
}