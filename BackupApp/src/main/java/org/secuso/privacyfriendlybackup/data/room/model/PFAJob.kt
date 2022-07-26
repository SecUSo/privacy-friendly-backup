package org.secuso.privacyfriendlybackup.data.room.model

import android.os.Parcelable
import androidx.room.*
import kotlinx.android.parcel.Parcelize
import org.secuso.privacyfriendlybackup.data.room.model.enums.PFAJobAction
import java.util.*

/**
 * This table holds the Jobs for PFAs. If the action is RESTORE, then the dataId should be set, to provide the restore data.
 */
@Parcelize
@Entity(tableName = "PFAJob", indices = [Index(value = ["packageName", "action"], unique = true)])
data class PFAJob(
    @PrimaryKey(autoGenerate = true) val _id : Int,
    val uid : Int,
    @ColumnInfo(name = "packageName") val packageName : String,
    val timestamp : Date,
    @ColumnInfo(name = "action") val action : PFAJobAction,
    @ColumnInfo(name = "dataId") var dataId : Long? = null
) : Parcelable