package org.secuso.privacyfriendlybackup.ui.application

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.bumptech.glide.Glide
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.room.model.BackupJob
import org.secuso.privacyfriendlybackup.ui.application.ApplicationOverviewViewModel.*
import java.lang.ref.WeakReference
import java.util.Comparator

class ApplicationAdapter(val context : Context, adapterCallback : ManageListAdapterCallback) : RecyclerView.Adapter<ApplicationAdapter.ViewHolder>(),
    BackupJobAdapter.ManageListAdapterCallback {

    interface ManageListAdapterCallback {
        fun onItemClick(view: View, packageName : String, job : BackupJob?, menuItemId : Int?)
    }

    companion object {
        const val VIEW_TYPE_APPLICATION = 0
    }

    init {
        setHasStableIds(true)
    }

    private val LEXICOGRAPHICAL_COMPARATOR: Comparator<BackupApplicationData> =
        Comparator { a: BackupApplicationData, b: BackupApplicationData -> a.pfaInfo.label.compareTo(b.pfaInfo.label) }

    private val mComparator: Comparator<BackupApplicationData> = LEXICOGRAPHICAL_COMPARATOR

    private val sortedList : SortedList<BackupApplicationData> = SortedList<BackupApplicationData>(
        BackupApplicationData::class.java,
        object : SortedList.Callback<BackupApplicationData>() {
            override fun onInserted(position: Int, count: Int) { notifyItemRangeInserted(position, count) }
            override fun onRemoved(position: Int, count: Int) { notifyItemRangeRemoved(position, count) }
            override fun onMoved(fromPosition: Int, toPosition: Int) { notifyItemMoved(fromPosition, toPosition) }
            override fun onChanged(position: Int, count: Int) { notifyItemRangeChanged(position, count) }
            override fun compare(a: BackupApplicationData?, b: BackupApplicationData?): Int = mComparator.compare(a, b)
            override fun areContentsTheSame(oldItem: BackupApplicationData, newItem: BackupApplicationData?): Boolean = oldItem == newItem
            override fun areItemsTheSame(a: BackupApplicationData, b: BackupApplicationData): Boolean = a.pfaInfo.packageName == b.pfaInfo.packageName
        }
    )

    private val callback = WeakReference(adapterCallback)

    fun setData(data : List<BackupApplicationData>) {
        sortedList.addAll(data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_application_pfa, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        return sortedList[position].pfaInfo.packageName.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return sortedList.size()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = sortedList[position]

        val vh = holder as ApplicationViewHolder

        Glide.with(context)
            .load(data.pfaInfo.icon)
            .centerCrop()
            .into(vh.mImage)

        holder.mName.text = data.pfaInfo.label

        holder.mCard.setOnClickListener {
            val popup = PopupMenu(context, it)
            popup.menuInflater.inflate(R.menu.menu_popup_application, popup.menu)
            popup.gravity = Gravity.END
            popup.setOnMenuItemClickListener { item ->
                callback.get()?.onItemClick(it, data.pfaInfo.packageName, null, item.itemId)
                return@setOnMenuItemClickListener true
            }
            popup.menu.findItem(R.id.menu_cancel_jobs).apply {
                this.isVisible = data.jobs.isNotEmpty()
            }
            popup.menu.findItem(R.id.menu_restore_most_recent_backup).apply {
                this.isVisible = data.backups.isNotEmpty()
            }
            popup.show()
        }

        val adapter = BackupJobAdapter(context, this)
        adapter.submitList(data.jobs)
        holder.mList.adapter = adapter
        holder.mList.visibility = if(data.jobs.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_APPLICATION
    }

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class ApplicationViewHolder(itemView: View) : ViewHolder(itemView) {
        val mName : TextView = itemView.findViewById(R.id.name)
        val mCard : CardView = itemView.findViewById(R.id.card)
        val mImage : ImageView = itemView.findViewById(R.id.image)
        val mList : RecyclerView = itemView.findViewById(R.id.list)
    }

    override fun onItemClick(view: View, packageName: String, job: BackupJob, menuItemId: Int?) {
        callback.get()?.onItemClick(view, packageName, job, menuItemId)
    }
}