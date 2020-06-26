package org.secuso.privacyfriendlybackup.database.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(indices = [Index(value = ["packageName"])])
data class BackupData(
    @PrimaryKey
    val _id : Int,
    val uid : Int,
    val packageName : String,
    val timestamp : Date,
    val storageService : String,
    val path : String,
    val hash : String
) : Parcelable