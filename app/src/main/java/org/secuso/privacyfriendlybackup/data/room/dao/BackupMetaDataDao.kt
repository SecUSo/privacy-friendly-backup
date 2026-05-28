package org.secuso.privacyfriendlybackup.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType

@Dao
interface BackupMetaDataDao {
    @Query("SELECT * FROM StoredBackupMetaData")
    suspend fun getAll() : List<StoredBackupMetaData>

    @Query("SELECT * FROM StoredBackupMetaData")
    fun getAllLive() : LiveData<List<StoredBackupMetaData>>

    @Query("SELECT * FROM StoredBackupMetaData WHERE _id = :id")
    suspend fun getFromId(id : Long) : StoredBackupMetaData?

    @Query("SELECT * FROM StoredBackupMetaData WHERE _id IN (:ids)")
    suspend fun getFromIds(ids : List<Long>) : List<StoredBackupMetaData>

    @Query("SELECT * FROM StoredBackupMetaData WHERE packageName = :packageName")
    suspend fun getFromPackage(packageName: String) : List<StoredBackupMetaData>

    @Query("SELECT * FROM StoredBackupMetaData WHERE packageName = :packageName")
    fun getFromPackageLiveData(packageName: String) : LiveData<List<StoredBackupMetaData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: StoredBackupMetaData) : Long

    @Query("DELETE FROM StoredBackupMetaData WHERE _id = :id")
    suspend fun deleteForId(id: Long)

    @Query("DELETE FROM StoredBackupMetaData WHERE _id IN (:ids)")
    suspend fun deleteForIds(ids: List<Long>)

    @Query("SELECT * FROM StoredBackupMetaData WHERE storageService != :storageType")
    suspend fun getAllOfOtherStorageType(storageType: StorageType): List<StoredBackupMetaData>

    @Query("SELECT COUNT(*) FROM StoredBackupMetaData")
    suspend fun getTotal(): Long
}