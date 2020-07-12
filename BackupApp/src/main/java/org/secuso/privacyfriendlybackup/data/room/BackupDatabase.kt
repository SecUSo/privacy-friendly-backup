package org.secuso.privacyfriendlybackup.data.room

import android.content.Context
import androidx.room.*
import org.secuso.privacyfriendlybackup.data.room.converters.Converters
import org.secuso.privacyfriendlybackup.data.room.dao.BackupJobDao
import org.secuso.privacyfriendlybackup.data.room.dao.BackupMetaDataDao
import org.secuso.privacyfriendlybackup.data.room.dao.PFAJobDao
import org.secuso.privacyfriendlybackup.data.room.dao.InternalBackupDataDao
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.InternalBackupData
import org.secuso.privacyfriendlybackup.data.room.model.StoredBackupMetaData
import org.secuso.privacyfriendlybackup.data.room.model.PFAJob

@Database(entities = [StoredBackupMetaData::class, PFAJob::class, InternalBackupData::class, BackupJob::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class BackupDatabase : RoomDatabase() {

    abstract fun backupMetaDataDao(): BackupMetaDataDao
    abstract fun pfaJobDao(): PFAJobDao
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
            ).enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}