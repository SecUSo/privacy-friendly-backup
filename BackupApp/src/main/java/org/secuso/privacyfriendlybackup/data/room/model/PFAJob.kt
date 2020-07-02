package org.secuso.privacyfriendlybackup.data.room.model

import android.os.Parcelable
import androidx.room.*
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(indices = [Index(value = ["packageName", "action"], unique = true)])
data class PFAJob(
    @PrimaryKey(autoGenerate = true) val _id : Int,
    val uid : Int,
    @ColumnInfo(name = "packageName") val packageName : String,
    val timestamp : Date,
    @ColumnInfo(name = "action") val action : PFAJobAction,
    @ColumnInfo(name = "dataId") val dataId : Long? = null
) : Parcelable