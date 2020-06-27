package org.secuso.privacyfriendlybackup.database.room

import android.content.Context
import androidx.room.*
import org.secuso.privacyfriendlybackup.database.room.converters.Converters
import org.secuso.privacyfriendlybackup.database.room.dao.BackupMetaDataDao
import org.secuso.privacyfriendlybackup.database.room.dao.BackupJobDao
import org.secuso.privacyfriendlybackup.database.room.dao.InternalBackupDataDao
import org.secuso.privacyfriendlybackup.database.room.model.InternalBackupData
import org.secuso.privacyfriendlybackup.database.room.model.StoredBackupMetaData
import org.secuso.privacyfriendlybackup.database.room.model.BackupJob

@Database(entities = [StoredBackupMetaData::class, BackupJob::class, InternalBackupData::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class BackupDatabase : RoomDatabase() {

    abstract fun backupMetaDataDao(): BackupMetaDataDao
    abstract fun backupJobDao(): BackupJobDao
    abstract fun internalBackupDataDao(): InternalBackupDataDao

    companion object {
        val DB_NAME = "BackupDatabase.db"

        private var INSTANCE: BackupDatabase? = null

        fun getInstance(context: Context): BackupDatabase {
            val tempInstance =
                INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                INSTANCE =
                    buildDatabase(
                        context
                    )
                return INSTANCE!!
            }
        }

        private fun buildDatabase(context: Context): BackupDatabase {
            return Room.databaseBuilder(context, BackupDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}