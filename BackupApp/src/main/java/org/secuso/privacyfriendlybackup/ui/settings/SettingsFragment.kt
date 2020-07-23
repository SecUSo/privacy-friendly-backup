package org.secuso.privacyfriendlybackup.ui.settings

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.google.android.material.appbar.AppBarLayout
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_STORAGE_TYPE
import org.secuso.privacyfriendlybackup.ui.encryption.OpenPgpAppDialogFragment
import org.secuso.privacyfriendlybackup.ui.encryption.OpenPgpAppPreference
import org.secuso.privacyfriendlybackup.ui.encryption.OpenPgpKeyPreference
import org.secuso.privacyfriendlybackup.ui.main.MainActivity

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val DIALOG_FRAGMENT_TAG = "SettingsFragment.DIALOG_FRAGMENT_TAG"

        fun newInstance() : SettingsFragment {
            return SettingsFragment()
        }
    }

    private lateinit var toolbar : Toolbar
    private lateinit var appBar : AppBarLayout

    var storageTypePref : ListPreference? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbar = requireActivity().findViewById(R.id.toolbar)
        appBar = requireActivity().findViewById(R.id.app_bar)

        setTitle(R.string.fragment_title_encryption)

        returnToNormalToolbarColor()
    }

    private fun setTitle(@StringRes stringRes: Int) {
        if(arguments?.getBoolean(MainActivity.TWOPANE) == false) {
            val title = getString(stringRes)
            toolbar.title = title
            requireActivity().actionBar?.title = title
        }
    }

    private fun playColorAnimation(@ColorInt colorFrom: Int, @ColorInt colorTo: Int, duration : Long = 250, applyAnimation : (a : ValueAnimator) -> Unit = this::defaultFadeAnimation) : Animator {
        val colorAnimation: ValueAnimator = ValueAnimator.ofArgb(colorFrom, colorTo)
        colorAnimation.duration = duration
        colorAnimation.addUpdateListener {
            applyAnimation(it)
        }
        return colorAnimation
    }

    private fun defaultFadeAnimation(a : ValueAnimator) {
        appBar.setBackgroundColor(a.animatedValue as Int)
        toolbar.setBackgroundColor(a.animatedValue as Int)
        activity?.window?.statusBarColor = a.animatedValue as Int
    }

    private fun returnToNormalToolbarColor() {
        var colorFrom = ContextCompat.getColor(requireContext(), R.color.middlegrey)
        val background = appBar.background
        if(background is ColorDrawable) {
            colorFrom = background.color
        }
        val colorTo = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        playColorAnimation(colorFrom, colorTo, duration = 350).start()
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        super.onDisplayPreferenceDialog(preference)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)

        storageTypePref = findPreference(PREF_STORAGE_TYPE)

        storageTypePref?.apply {
            entryValues = StorageType.getStorageOptions().map { it.name }.toTypedArray()
            entries = StorageType.getStorageOptions().map { requireContext().getString(it.nameResId) }.toTypedArray()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}