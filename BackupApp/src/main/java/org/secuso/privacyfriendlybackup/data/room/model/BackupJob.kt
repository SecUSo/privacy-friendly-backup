package org.secuso.privacyfriendlybackup.data.room.model

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.*
import kotlinx.android.parcel.Parcelize
import org.secuso.privacyfriendlybackup.data.BackupJobManager
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import java.util.*

/**
 * This table holds the Jobs for Backups.
 */
@Parcelize
@Entity(tableName = "BackupJob",
    indices = [
    Index(value = ["packageName", "action"], unique = true),
    Index(value = ["dataId"])
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
) : Parcelable {
    fun getWorkerTag() : String = BackupJobManager.getTagForJob(this)

    object DIFFCALLBACK : DiffUtil.ItemCallback<BackupJob>() {
        override fun areItemsTheSame(o: BackupJob, n: BackupJob): Boolean = o._id == n._id
        override fun areContentsTheSame(o: BackupJob, n: BackupJob): Boolean = o == n
    }
}
