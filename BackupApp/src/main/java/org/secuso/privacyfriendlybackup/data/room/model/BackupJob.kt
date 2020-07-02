package org.secuso.privacyfriendlybackup.data.room.model

import android.os.Parcelable
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * This table holds the Jobs for the PFAs. If the action is RESTORE, then the dataId should be set, to provide the restore data.
 */
@Parcelize
@Entity(indices = [Index(value = ["packageName", "action"], unique = true), Index(value = ["dataId"])], foreignKeys = [
    ForeignKey(onDelete = CASCADE, entity = InternalBackupData::class, parentColumns = ["_id"], childColumns = ["dataId"] )
])
data class BackupJob(
    @PrimaryKey(autoGenerate = true) val _id : Int = 0,
    @ColumnInfo(name = "packageName") val packageName : String,
    val timestamp : Date,
    @ColumnInfo(name = "action") val action : BackupJobAction,
    @ColumnInfo(name = "dataId") val dataId : Long? = null,
    val nextJob : Long? = null
) : Parcelable
