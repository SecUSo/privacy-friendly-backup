package org.secuso.privacyfriendlybackup.ui.inspection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.secuso.privacyfriendlybackup.R

class DataInspectionAdapter(val context : Context) : RecyclerView.Adapter<DataInspectionAdapter.DataViewHolder>() {

    interface DataInspectionOnItemClickListener {
        fun onItemClick(data : String)
    }

    private var data : List<String> = emptyList()
    private var listener : DataInspectionOnItemClickListener? = null

    fun setData(data : List<String>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        return DataViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_data_inspection, parent, false)
        )
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        val dataLine = data.get(position)

        holder.mCard.setOnClickListener {
            listener?.onItemClick(dataLine)
        }
        holder.mName.text = dataLine
        holder.mImage.setImageDrawable(null)
    }

    override fun getItemCount(): Int = data.size

    fun setOnItemClickListener(dataInspectionOnItemClickListener: DataInspectionOnItemClickListener) {
        listener = dataInspectionOnItemClickListener
    }

    class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mName : TextView = itemView.findViewById(R.id.name)
        val mCard : CardView = itemView.findViewById(R.id.card)
        //val mCheckbox : CheckBox = itemView.findViewById(R.id.checkbox)
        val mImage : ImageView = itemView.findViewById(R.id.image)
    }
}

