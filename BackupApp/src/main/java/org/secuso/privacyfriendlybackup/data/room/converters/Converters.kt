package org.secuso.privacyfriendlybackup.data.room.converters

import androidx.room.TypeConverter
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import org.secuso.privacyfriendlybackup.data.room.model.enums.PFAJobAction
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import java.util.*

class Converters {
    @TypeConverter
    fun dateFromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun pfaJobActionFromString(value: String?): PFAJobAction? {
        return value?.let { PFAJobAction.valueOf(value) }
    }

    @TypeConverter
    fun pfaJobActionToString(jobAction: PFAJobAction?): String? {
        return jobAction?.name
    }

    @TypeConverter
    fun backupJobActionFromString(value: String?): BackupJobAction? {
        return value?.let { BackupJobAction.valueOf(value) }
    }

    @TypeConverter
    fun backupJobActionToString(jobAction: BackupJobAction?): String? {
        return jobAction?.name
    }

    @TypeConverter
    fun storageTypeFromString(value: String?): StorageType? {
        return value?.let { StorageType.valueOf(value) }
    }

    @TypeConverter
    fun storageTypeToString(type: StorageType?): String? {
        return type?.name
    }
}