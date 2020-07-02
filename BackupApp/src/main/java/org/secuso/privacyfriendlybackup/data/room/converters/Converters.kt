package org.secuso.privacyfriendlybackup.data.room.converters

import androidx.room.TypeConverter
import org.secuso.privacyfriendlybackup.data.room.model.PFAJobAction
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
    fun backupJobActionFromString(value: String?): PFAJobAction? {
        return value?.let { PFAJobAction.valueOf(value) }
    }

    @TypeConverter
    fun backupJobActionToString(jobAction: PFAJobAction?): String? {
        return jobAction?.name
    }
}