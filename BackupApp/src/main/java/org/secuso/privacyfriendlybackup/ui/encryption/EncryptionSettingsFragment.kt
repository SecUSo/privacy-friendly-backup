package org.secuso.privacyfriendlybackup.ui.encryption

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.appbar.AppBarLayout
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_CRYPTO_PROVIDER
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_ENABLE
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_KEY
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_PASSPHRASE
import org.secuso.privacyfriendlybackup.ui.main.MainActivity


class EncryptionSettingsFragment : PreferenceFragmentCompat() {

    companion object {
        // TODO: Add Dialog or warning box that tells the user to tur encryption on
        const val DIALOG_FRAGMENT_TAG = "EncryptionSettingsFragment.DIALOG_FRAGMENT_TAG"
        const val REQUEST_CODE_POST_NOTIFICATION = 1

        fun newInstance() : EncryptionSettingsFragment {
            return EncryptionSettingsFragment()
        }
    }

    protected lateinit var toolbar : Toolbar
    protected lateinit var appBar : AppBarLayout

    var enablePref : SwitchPreferenceCompat? = null
    var providerPref : OpenPgpAppPreference? = null
    var passphrasePref : EditTextPreference? = null
    var keyPref : OpenPgpKeyPreference? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbar = requireActivity().findViewById(R.id.toolbar)
        appBar = requireActivity().findViewById(R.id.app_bar)

        setTitle(R.string.fragment_title_encryption)

        returnToNormalToolbarColor()
    }

    protected fun setTitle(@StringRes stringRes: Int) {
        if(arguments?.getBoolean(MainActivity.TWOPANE) == false) {
            val title = getString(stringRes)
            toolbar.title = title
            requireActivity().actionBar?.title = title
        }
    }

    protected fun playColorAnimation(@ColorInt colorFrom: Int, @ColorInt colorTo: Int, duration : Long = 250, applyAnimation : (a : ValueAnimator) -> Unit = this::defaultFadeAnimation) : Animator {
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

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val f : DialogFragment
        if(preference is OpenPgpAppPreference) {
            f = OpenPgpAppDialogFragment.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
            return
        }

        super.onDisplayPreferenceDialog(preference)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_encryption, rootKey)

        enablePref = findPreference(PREF_ENCRYPTION_ENABLE)
        providerPref = findPreference(PREF_ENCRYPTION_CRYPTO_PROVIDER)
        passphrasePref = findPreference(PREF_ENCRYPTION_PASSPHRASE)
        keyPref = findPreference(PREF_ENCRYPTION_KEY)

        enablePref?.apply {

        }

        passphrasePref?.apply {
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                if(it.text.isNullOrEmpty()) {
                    "No passphrase set"
                } else {
                    "*".repeat(it.text!!.length)
                }
            }
        }

        providerPref?.apply {
            setOnPreferenceChangeListener { preference, newValue ->
                val provider = newValue as String?
                if(provider.isNullOrEmpty()) {
                    enablePref?.isChecked = false
                    enablePref?.isEnabled = false
                    enablePref?.summary = "Choose a valid provider first"
                } else {
                    enablePref?.isEnabled = true
                    enablePref?.setSummaryOff(R.string.pref_encryption_enable_off)
                    keyPref?.setOpenPgpProvider(newValue)
                }
                true
            }
        }

        providerPref?.callChangeListener(providerPref!!.value)
        keyPref?.setOpenPgpProvider(providerPref?.value)
        //keyPref?.setDefaultUserId("Backup <test@example.com>")

//        keyPref?.apply {
//            this.onPreferenceChangeListener = android.preference.Preference.OnPreferenceChangeListener { preference, newValue ->
//                true
//            }
//        }

        val encryptionEnabled : SwitchPreferenceCompat? = findPreference(PREF_ENCRYPTION_ENABLE)
        encryptionEnabled?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.equals(true)) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATION)
            }
            return@setOnPreferenceChangeListener true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(keyPref != null) {
            if (keyPref!!.handleOnActivityResult(requestCode, resultCode, data)) {
                // handled by OpenPgpKeyPreference
                return
            }
        }
    }
}