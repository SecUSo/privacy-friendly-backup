package org.secuso.privacyfriendlybackup.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    }


    override fun onBackPressed() {
        activity?.onBackPressed()
    }

}