package org.secuso.privacyfriendlybackup.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_help.*
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.ui.common.BaseFragment

class HelpFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val expandableListAdapter: ExpandableListAdapter
        val helpDataDump = HelpDataDump(requireActivity())

        val expandableListDetail = helpDataDump.dataGeneral
        val expandableListTitleGeneral: List<String> = ArrayList<String>(expandableListDetail.keys)
        expandableListAdapter = ExpandableListAdapter(requireActivity(), expandableListTitleGeneral, expandableListDetail)
        generalExpandableListView.setAdapter(expandableListAdapter)
    }

    override fun onBackPressed() {
        activity?.onBackPressed()
    }

}