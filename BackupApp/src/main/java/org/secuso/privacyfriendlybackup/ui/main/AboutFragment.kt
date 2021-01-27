package org.secuso.privacyfriendlybackup.ui.main

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_about.*
import org.secuso.privacyfriendlybackup.BuildConfig
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.ui.common.BaseFragment

class AboutFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        githubURL.movementMethod = LinkMovementMethod.getInstance()
        secusoWebsite.movementMethod = LinkMovementMethod.getInstance()
        textFieldVersion.text = requireActivity().getString(R.string.version_number, BuildConfig.VERSION_NAME)
    }


    override fun onBackPressed() {
        activity?.finish()
    }
}