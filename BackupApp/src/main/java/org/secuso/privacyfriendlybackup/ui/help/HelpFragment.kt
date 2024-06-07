package org.secuso.privacyfriendlybackup.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.secuso.privacyfriendlybackup.databinding.FragmentHelpBinding
import org.secuso.privacyfriendlybackup.ui.common.BaseFragment

class HelpFragment : BaseFragment() {
    lateinit var binding: FragmentHelpBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHelpBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val expandableListAdapter: ExpandableListAdapter
        val helpDataDump = HelpDataDump(requireActivity())

        val expandableListDetail = helpDataDump.dataGeneral
        val expandableListTitleGeneral: List<String> = ArrayList<String>(expandableListDetail.keys)
        expandableListAdapter = ExpandableListAdapter(requireActivity(), expandableListTitleGeneral, expandableListDetail)
        binding.generalExpandableListView.setAdapter(expandableListAdapter)
    }

    override fun onBackPressed() {
        activity?.finish()
    }

}