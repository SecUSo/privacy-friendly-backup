package org.secuso.privacyfriendlybackup.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.bumptech.glide.Glide
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository.BackupData
import org.secuso.privacyfriendlybackup.data.apps.PFApplicationHelper
import java.lang.ref.WeakReference
import java.util.*

class FilterableBackupAdapter(val context : Context, adapterCallback : ManageListAdapterCallback) : RecyclerView.Adapter<FilterableBackupAdapter.ViewHolder>() {

    interface ManageListAdapterCallback {
        fun onDeleteCountChanged(count: Int)
        fun onEnableDeleteMode()
        fun onItemClick(id : Long)
    }

    private val LEXICOGRAPHICAL_COMPARATOR: Comparator<BackupData> =
        Comparator { a: BackupData, b: BackupData -> a.packageName.compareTo(b.packageName) }

    private val TIME_COMPARATOR: Comparator<BackupData> =
        Comparator { a: BackupData, b: BackupData -> a.timestamp.compareTo(b.timestamp) }

    private val mComparator: Comparator<BackupData> = TIME_COMPARATOR

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
    private val deleteList: MutableSet<BackupData> = HashSet()
    private var deleteMode = false
    private val callback = WeakReference(adapterCallback)

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
        ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backup, parent, false))

    override fun getItemId(position: Int): Long =
        sortedList[position].id

    override fun getItemCount(): Int =
        sortedList.size()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = this.sortedList[position]
        val pfaInfo = PFApplicationHelper.getDataForPackageName(context, data.packageName)

        holder.mCheckbox.visibility = if (deleteMode) View.VISIBLE else View.INVISIBLE
        holder.mCheckbox.isChecked = deleteList.contains(data)

        holder.mCard.setOnLongClickListener {
            if (deleteMode) {
                if (holder.mCheckbox.isChecked) {
                    deleteList.remove(data)
                    holder.mCheckbox.isChecked = false
                } else {
                    deleteList.add(data)
                    holder.mCheckbox.isChecked = true
                }
                notifyDeleteCount()
            } else {
                callback.get()?.onEnableDeleteMode()
            }
            false
        }

        holder.mCard.setOnClickListener {
            if (deleteMode) {
                if (holder.mCheckbox.isChecked) {
                    deleteList.remove(data)
                    holder.mCheckbox.isChecked = false
                } else {
                    deleteList.add(data)
                    holder.mCheckbox.isChecked = true
                }
                notifyDeleteCount()
            } else {
                callback.get()?.onItemClick(data.id)
            }
        }

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

    private fun notifyDeleteCount() {
        callback.get()?.onDeleteCountChanged(deleteList.size)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mName : TextView = itemView.findViewById(R.id.name)
        val mCard : CardView = itemView.findViewById(R.id.card)
        val mCheckbox : CheckBox = itemView.findViewById(R.id.checkbox)
        val mImage : ImageView = itemView.findViewById(R.id.image)
    }

    fun enableDeleteMode() {
        deleteList.clear()
        deleteMode = true
        notifyDeleteCount()
        notifyDataSetChanged()
    }

    fun selectAll() {
        for (i in 0 until sortedList.size()) {
            deleteList.add(sortedList[i])
        }
        notifyDeleteCount()
        notifyDataSetChanged()
    }

    fun deselectAll() {
        deleteList.clear()
        notifyDeleteCount()
        notifyDataSetChanged()
    }

    fun disableDeleteMode() {
        deleteMode = false
        deleteList.clear()
        notifyDeleteCount()
        notifyDataSetChanged()
    }

    fun getDeleteList() : Set<BackupData> = deleteList
}