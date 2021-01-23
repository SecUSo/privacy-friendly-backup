package org.secuso.privacyfriendlybackup.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.secuso.privacyfriendlybackup.data.room.model.InternalBackupData

@Dao
interface InternalBackupDataDao {
    @Query("SELECT * FROM InternalBackupData")
    suspend fun getAll() : List<InternalBackupData>

    @Query("SELECT * FROM InternalBackupData WHERE _id = :id")
    suspend fun getById(id : Long) : InternalBackupData?

    @Query("SELECT * FROM InternalBackupData WHERE file = :file")
    suspend fun getByFilename(file : String) : InternalBackupData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: InternalBackupData) : Long

    @Query("DELETE FROM InternalBackupData WHERE packageName = :packageName")
    suspend fun delete(packageName: String?)

    @Query("DELETE FROM InternalBackupData WHERE _id = :id")
    suspend fun delete(id: Long)
}