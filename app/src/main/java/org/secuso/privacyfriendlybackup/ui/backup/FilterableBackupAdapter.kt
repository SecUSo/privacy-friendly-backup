package org.secuso.privacyfriendlybackup.ui.backup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.bumptech.glide.Glide
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository.BackupData
import org.secuso.privacyfriendlybackup.data.apps.PFApplicationHelper
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.ui.common.Mode
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class FilterableBackupAdapter(val context : Context, adapterCallback : ManageListAdapterCallback) : RecyclerView.Adapter<FilterableBackupAdapter.ViewHolder>() {

    interface ManageListAdapterCallback {
        fun onSelectionCountChanged(count: Int)
        fun onEnableMode(mode: Mode)
        fun onItemClick(id : Long, backupData: BackupData, view: View)
    }

    private val LEXICOGRAPHICAL_COMPARATOR: Comparator<BackupData> =
        Comparator { a: BackupData, b: BackupData -> a.packageName.compareTo(b.packageName) }

    private val TIME_COMPARATOR: Comparator<BackupData> =
        Comparator { a: BackupData, b: BackupData -> a.timestamp.compareTo(b.timestamp) }
    private val TIME_COMPARATOR_DESC: Comparator<BackupData> =
        Comparator { a: BackupData, b: BackupData -> b.timestamp.compareTo(a.timestamp) }

    private val mComparator: Comparator<BackupData> = TIME_COMPARATOR_DESC

    val completeData : MutableList<BackupData> = mutableListOf()
    val sortedList : SortedList<BackupData> = SortedList<BackupData>(
        BackupData::class.java,
        object : SortedList.Callback<BackupData>() {
            override fun onInserted(position: Int, count: Int) { notifyItemRangeInserted(position, count) }
            override fun onRemoved(position: Int, count: Int) { notifyItemRangeRemoved(position, count) }
            override fun onMoved(fromPosition: Int, toPosition: Int) { notifyItemMoved(fromPosition, toPosition) }
            override fun onChanged(position: Int, count: Int) { notifyItemRangeChanged(position, count) }
            override fun compare(a: BackupData?, b: BackupData?): Int = mComparator.compare(a, b)
            override fun areContentsTheSame(oldItem: BackupData, newItem: BackupData?): Boolean = oldItem == newItem
            override fun areItemsTheSame(a: BackupData, b: BackupData): Boolean = a.id == b.id
        }
    )
    private val selectionList: MutableSet<BackupData> = HashSet()
    private val callback = WeakReference(adapterCallback)
    private var mode = Mode.NORMAL
    private val selectionActive : Boolean
        get() = Mode.DELETE.isActiveIn(mode) or Mode.EXPORT.isActiveIn(mode)

    init {
        setHasStableIds(true)
    }

    fun setData(data : List<BackupData>) {
        completeData.clear()
        completeData.addAll(data)
    }

    fun setFilteredData(data : List<BackupData>) {
        sortedList.beginBatchedUpdates()
        sortedList.clear()
        sortedList.addAll(data)
        sortedList.endBatchedUpdates()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backup, parent, false)
        )

    override fun getItemId(position: Int): Long =
        sortedList[position].id

    override fun getItemCount(): Int =
        sortedList.size()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = this.sortedList[position]
        val pfaInfo = PFApplicationHelper.getDataForPackageName(context, data.packageName)

        holder.mCheckbox.visibility = if (selectionActive) View.VISIBLE else View.INVISIBLE
        holder.mCheckbox.isChecked = selectionList.contains(data)

        holder.mCard.setOnLongClickListener {
            if (selectionActive) {
                if (holder.mCheckbox.isChecked) {
                    selectionList.remove(data)
                    holder.mCheckbox.isChecked = false
                } else {
                    selectionList.add(data)
                    holder.mCheckbox.isChecked = true
                }
                notifySelectionCount()
            } else {
                callback.get()?.onEnableMode(Mode.DELETE)
            }
            false
        }

        holder.mCard.setOnClickListener {
            if (selectionActive) {
                if (holder.mCheckbox.isChecked) {
                    selectionList.remove(data)
                    holder.mCheckbox.isChecked = false
                } else {
                    selectionList.add(data)
                    holder.mCheckbox.isChecked = true
                }
                notifySelectionCount()
            } else {
                callback.get()?.onItemClick(data.id, data, it)
            }
        }

        val icon = when(data.storageType) {
            StorageType.EXTERNAL -> if(data.encrypted) R.drawable.ic_baseline_phonelink_lock_24 else R.drawable.ic_baseline_smartphone_24
            StorageType.CLOUD -> if(data.encrypted) R.drawable.ic_cloud_24 else R.drawable.ic_cloud_24
        }

        Glide.with(context)
            .load(icon)
            .centerCrop()
            .into(holder.mStorageImage)
        holder.mStorageImage.setColorFilter(ContextCompat.getColor(context, R.color.darkblue))

        val dateString = SimpleDateFormat.getDateTimeInstance().format(data.timestamp)
        holder.mDate.text = dateString

        if(pfaInfo != null) {
            Glide.with(context)
                .load(pfaInfo.icon)
                .centerCrop()
                .into(holder.mImage)

            holder.mImage.visibility = View.VISIBLE
            holder.mName.text = pfaInfo.label
        } else {
            holder.mImage.visibility = View.GONE
            holder.mName.text = data.packageName
        }
    }

    private fun notifySelectionCount() {
        callback.get()?.onSelectionCountChanged(selectionList.size)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mName : TextView = itemView.findViewById(R.id.name)
        val mCard : CardView = itemView.findViewById(R.id.card)
        val mCheckbox : CheckBox = itemView.findViewById(R.id.checkbox)
        val mImage : ImageView = itemView.findViewById(R.id.image)
        val mDate : TextView = itemView.findViewById(R.id.date)
        val mStorageImage : ImageView = itemView.findViewById(R.id.storageImage)
    }

    fun enableMode(_mode: Mode) {
        mode = mode.activate(_mode)
        notifySelectionCount()
        notifyDataSetChanged()
    }

    fun selectAll() {
        for (i in 0 until sortedList.size()) {
            selectionList.add(sortedList[i])
        }
        notifySelectionCount()
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectionList.clear()
        notifySelectionCount()
        notifyDataSetChanged()
    }

    fun disableMode(_mode: Mode) {
        mode = mode.deactivate(_mode)
        notifySelectionCount()
        notifyDataSetChanged()
    }

    fun getSelectionList() : Set<BackupData> = selectionList

    fun selectItem(data: BackupData) {
        if(selectionActive) {
            selectionList.add(data)
            notifySelectionCount()
            notifyDataSetChanged()
        }
    }
}