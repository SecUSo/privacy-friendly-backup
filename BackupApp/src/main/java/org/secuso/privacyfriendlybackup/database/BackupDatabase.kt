package org.secuso.privacyfriendlybackup.database

import android.content.Context
import androidx.room.*
import org.secuso.privacyfriendlybackup.database.converters.Converters
import org.secuso.privacyfriendlybackup.database.dao.BackupDataDao
import org.secuso.privacyfriendlybackup.database.dao.BackupJobDao
import org.secuso.privacyfriendlybackup.database.model.BackupData
import org.secuso.privacyfriendlybackup.database.model.BackupJob

@Database(entities = [BackupData::class, BackupJob::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class BackupDatabase : RoomDatabase() {

    abstract fun backupDataDao(): BackupDataDao
    abstract fun backupJobDao(): BackupJobDao

    companion object {
        val DB_NAME = "BackupDatabase.db"

        private var INSTANCE: BackupDatabase? = null

        @Synchronized
        fun getInstance(context: Context): BackupDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                INSTANCE = buildDatabase(context)
                return INSTANCE!!
            }
        }

        private fun buildDatabase(context: Context): BackupDatabase {
            return Room.databaseBuilder(context, BackupDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}