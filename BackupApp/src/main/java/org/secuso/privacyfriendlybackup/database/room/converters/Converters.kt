package org.secuso.privacyfriendlybackup.database.room.converters

import androidx.room.TypeConverter
import org.secuso.privacyfriendlybackup.database.room.model.BackupJobAction
import java.util.*

class Converters {
    @TypeConverter
    fun dateFromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }

    @TypeConverter
    fun backupJobActionFromString(value: String?): BackupJobAction? {
        return value?.let { BackupJobAction.valueOf(value) }
    }

    @TypeConverter
    fun backupJobActionToString(jobAction: BackupJobAction?): String? {
        return jobAction?.name
    }
}