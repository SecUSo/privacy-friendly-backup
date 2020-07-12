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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.data.room.model.enums.BackupJobAction
import java.lang.ref.WeakReference

class BackupJobAdapter(val context: Context, callback: ManageListAdapterCallback, val lifecycleOwner: LifecycleOwner) : ListAdapter<BackupJob, BackupJobAdapter.BackupJobViewHolder>(BackupJob.DIFFCALLBACK) {

    interface ManageListAdapterCallback {
        fun onItemClick(view: View, packageName : String, job : BackupJob, menuItemId : Int?)
    }

    init {
        setHasStableIds(true)
    }

    override fun submitList(list: List<BackupJob>?) {
        if(list == null) {
            return super.submitList(list)
        }

        val result = ArrayList<BackupJob>(list)

        result.sortBy { it.action.ordinal }

        super.submitList(result)
    }

    private val callback = WeakReference(callback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupJobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application_job, parent, false)
        return BackupJobViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackupJobViewHolder, position: Int) {
        val data = getItem(position)

        holder.mName.text = context.getString(data.action.stringResId)

        holder.mContainer.setOnClickListener {
            callback.get()?.onItemClick(it, data.packageName, data, null)
        }

        holder.mDots.visibility = if(data.nextJob != null) View.VISIBLE else View.GONE
        holder.mDivider.visibility =
            if(data.nextJob == null
                && data.action == BackupJobAction.BACKUP_STORE
                && itemCount > position + 1
            )
                View.VISIBLE
            else
                View.GONE

        Glide.with(context).load(data.action.image).into(holder.mImage)

        val colorId = if(data.active) {
            R.color.colorAccent
        } else {
            R.color.middlegrey
        }
        holder.mImage.setColorFilter(ContextCompat.getColor(context, colorId))

        WorkManager.getInstance(context).getWorkInfosByTagLiveData(data.getWorkerTag()).observe(lifecycleOwner) {
            workInfoList ->
            if(workInfoList.isNotEmpty()) {
                val workInfo = workInfoList[0]
                val colorId : Int = when (workInfo.state) {
                    WorkInfo.State.RUNNING -> R.color.colorAccent
                    WorkInfo.State.SUCCEEDED -> R.color.green
                    WorkInfo.State.BLOCKED -> R.color.darkgrey
                    WorkInfo.State.CANCELLED -> R.color.lightred
                    WorkInfo.State.ENQUEUED -> R.color.middlegrey
                    WorkInfo.State.FAILED -> R.color.red
                }
                holder.mImage.setColorFilter(ContextCompat.getColor(context, colorId))
            }
        }
    }

    class BackupJobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mName : TextView = itemView.findViewById(R.id.name)
        val mImage : ImageView = itemView.findViewById(R.id.image)
        val mContainer : ConstraintLayout = itemView.findViewById(R.id.container)
        val mDots : TextView = itemView.findViewById(R.id.nextDots)
        val mDivider : View = itemView.findViewById(R.id.divider)
    }
}