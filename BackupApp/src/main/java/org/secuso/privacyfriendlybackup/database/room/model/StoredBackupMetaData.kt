package org.secuso.privacyfriendlybackup.database.room.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(indices = [Index(value = ["packageName"])])
data class StoredBackupMetaData(
    @PrimaryKey(autoGenerate = true)
    val _id : Int = 0,
    val uid : Int,
    val packageName : String,
    val timestamp : Date,
    val storageService : String,
    val path : String,
    val hash : String,
    val encrypted : Boolean
) : Parcelable