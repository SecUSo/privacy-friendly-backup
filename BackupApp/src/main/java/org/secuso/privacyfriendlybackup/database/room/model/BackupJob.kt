package org.secuso.privacyfriendlybackup.database.room.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(indices = [Index(value = ["packageName"])])
/*, foreignKeys =
[
    ForeignKey(
        onDelete = CASCADE,
        entity = BackupData::class,
        parentColumns = ["_id"],
        childColumns = ["dataId"]
    )
]
)*/
data class BackupJob(
    @PrimaryKey(autoGenerate = true)
    val _id : Int,
    val uid : Int,
    val packageName : String,
    val timestamp : Date,
    val action : BackupJobAction,
    val dataId : Int = -1
) : Parcelable
