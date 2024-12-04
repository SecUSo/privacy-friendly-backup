package org.secuso.privacyfriendlybackup.ui.main

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.secuso.privacyfriendlybackup.BuildConfig
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.databinding.FragmentAboutBinding
import org.secuso.privacyfriendlybackup.ui.common.BaseFragment

class AboutFragment : BaseFragment() {
    lateinit var binding: FragmentAboutBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAboutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.githubURL.movementMethod = LinkMovementMethod.getInstance()
        binding.secusoWebsite.movementMethod = LinkMovementMethod.getInstance()
        binding.textFieldVersion.text = requireActivity().getString(R.string.version_number, BuildConfig.VERSION_NAME)
    }


    override fun onBackPressed() {
        activity?.finish()
    }
}