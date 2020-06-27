package org.secuso.privacyfriendlybackup.database.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.secuso.privacyfriendlybackup.database.room.model.BackupJob
import org.secuso.privacyfriendlybackup.database.room.model.InternalBackupData

@Dao
interface InternalBackupDataDao {
    @Query("SELECT * FROM InternalBackupData")
    suspend fun getAll() : List<InternalBackupData>

    @Query("SELECT * FROM InternalBackupData WHERE _id = :id")
    suspend fun getById(id : Int) : InternalBackupData

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: InternalBackupData) : Long

    @Query("DELETE FROM InternalBackupData WHERE packageName = :packageName")
    suspend fun delete(packageName: String?)

    @Query("DELETE FROM InternalBackupData WHERE _id = :id")
    suspend fun delete(id: Int)
}