package org.secuso.privacyfriendlybackup.ui.help

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import org.secuso.privacyfriendlybackup.R
import java.util.*


/**
 * Class structure taken from tutorial at http://www.journaldev.com/9942/android-expandablelistview-example-tutorial
 * last access 27th October 2016
 */
class ExpandableListAdapter(
    private val context: Context,
    private val expandableListTitle: List<String>,
    private val expandableListDetail: HashMap<String, List<String>>

) : BaseExpandableListAdapter() {

    override fun getChild(listPosition: Int, expandedListPosition: Int): String? {
        return expandableListDetail[expandableListTitle[listPosition]]?.get(expandedListPosition)
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }

    override fun getChildView(listPosition: Int, expandedListPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        var view = convertView

        if (view == null) {
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = layoutInflater.inflate(R.layout.item_help, parent, false)
        }

        val expandedListTextView = view!!.findViewById<View>(R.id.expandedListItem) as TextView
        expandedListTextView.text = getChild(listPosition, expandedListPosition)

        return view
    }

    override fun getChildrenCount(listPosition: Int): Int = expandableListDetail[expandableListTitle[listPosition]]!!.size
    override fun getGroup(listPosition: Int): Any = expandableListTitle[listPosition]
    override fun getGroupCount(): Int = expandableListTitle.size
    override fun getGroupId(listPosition: Int): Long = listPosition.toLong()
    override fun hasStableIds(): Boolean = false
    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean = true

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (view == null) {
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = layoutInflater.inflate(R.layout.item_help_group, parent, false)
        }

        val listTitleTextView = view!!.findViewById<View>(R.id.listTitle) as TextView
        listTitleTextView.setTypeface(null, Typeface.BOLD)
        listTitleTextView.text = getGroup(listPosition) as String

        return view
    }


}