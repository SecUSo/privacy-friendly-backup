package org.secuso.privacyfriendlybackup.data.room.model

import android.os.Parcelable
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import kotlinx.android.parcel.Parcelize
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import java.util.*

/**
 * This table holds the Jobs for Backups.
 */
@Parcelize
@Entity(indices = [
    Index(value = ["packageName", "action"], unique = true),
    Index(value = ["dataId"])
], foreignKeys = [
    ForeignKey(
        onDelete = CASCADE,
        entity = InternalBackupData::class,
        parentColumns = ["_id"],
        childColumns = ["dataId"]
    )
])
data class BackupJob(
    @PrimaryKey(autoGenerate = true) var _id : Long = 0,
    @ColumnInfo(name = "packageName") var packageName : String,
    var timestamp : Date,
    @ColumnInfo(name = "action") var action : BackupJobAction,
    @ColumnInfo(name = "dataId") var dataId : Long? = null,
    var nextJob : Long? = null,
    var active : Boolean = false,
    var location: String? = null
) : Parcelable
