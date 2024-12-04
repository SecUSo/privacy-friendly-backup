package org.secuso.privacyfriendlybackup.data.room.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "InternalBackupData", indices = [Index(value = ["packageName"])])
data class InternalBackupData(
    @PrimaryKey(autoGenerate = true)
    val _id : Long = 0,
    val packageName : String,
    val timestamp : Date,
    val file : String,
    val encrypted : Boolean
) : Parcelable