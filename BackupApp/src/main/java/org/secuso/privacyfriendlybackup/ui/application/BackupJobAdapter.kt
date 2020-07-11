package org.secuso.privacyfriendlybackup.ui.application

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import java.lang.ref.WeakReference

class BackupJobAdapter(val context: Context, callback: ManageListAdapterCallback) : ListAdapter<BackupJob, BackupJobAdapter.BackupJobViewHolder>(BackupJob.DIFFCALLBACK) {

    interface ManageListAdapterCallback {
        fun onItemClick(packageName : String, job : BackupJob)
    }

    init {
        setHasStableIds(true)
    }

    private val callback = WeakReference(callback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupJobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application_job, parent, false)
        return BackupJobViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupJobViewHolder, position: Int) {
        val data = getItem(position)

        holder.mName.text = data.action.name

        holder.mContainer.setOnClickListener {
            callback.get()?.onItemClick(data.packageName, data)
        }

        when(data.action) {
            BackupJobAction.BACKUP_ENCRYPT -> {
            }
        }

        Glide.with(context).load(data.action.image).into(holder.mImage)
    }

    class BackupJobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mName : TextView = itemView.findViewById(R.id.name)
        val mImage : ImageView = itemView.findViewById(R.id.image)
        val mContainer : ConstraintLayout = itemView.findViewById(R.id.container)
    }
}