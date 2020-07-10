package org.secuso.privacyfriendlybackup.ui.encryption

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_CRYPTO_PROVIDER
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_ENABLE
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_KEY
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_PASSPHRASE


class EncryptionSettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val DIALOG_FRAGMENT_TAG = "EncryptionSettingsFragment.DIALOG_FRAGMENT_TAG"

        fun newInstance() : EncryptionSettingsFragment {
            return EncryptionSettingsFragment()
        }
    }

    var enablePref : SwitchPreferenceCompat? = null
    var providerPref : OpenPgpAppPreference? = null
    var passphrasePref : EditTextPreference? = null
    var keyPref : OpenPgpKeyPreference? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val toolbar : Toolbar = requireActivity().findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.fragment_title_encryption)
        requireActivity().actionBar?.title = getString(R.string.fragment_title_encryption)
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
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
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

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
                    "*".repeat(it.text.length)
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