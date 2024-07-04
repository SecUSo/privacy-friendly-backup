package org.secuso.privacyfriendlybackup.data.room.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import java.util.*

@Parcelize
@Entity(tableName = "StoredBackupMetaData", indices = [Index(value = ["packageName", "timestamp"], unique = true)])
data class StoredBackupMetaData(
    @PrimaryKey(autoGenerate = true)
    val _id : Long = 0,
    val packageName : String,
    val timestamp : Date,
    val storageService : StorageType,
    val filename : String,
    val hash : String?,
    val encrypted : Boolean
) : Parcelable