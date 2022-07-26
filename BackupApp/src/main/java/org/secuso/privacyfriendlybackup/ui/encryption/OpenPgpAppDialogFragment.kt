package org.secuso.privacyfriendlybackup.ui.encryption

import android.R
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.PreferenceDialogFragmentCompat

class OpenPgpAppDialogFragment : PreferenceDialogFragmentCompat() {

    companion object {
        fun newInstance(key: String?): OpenPgpAppDialogFragment {
            val fragment = OpenPgpAppDialogFragment()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = getPgpAppPreference()

        if (positiveResult && (preference?.value != null)) {
            preference.setAndPersist(preference.value);
        }
    }

    private fun getPgpAppPreference(): OpenPgpAppPreference? {
        return preference as OpenPgpAppPreference
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {

        val preference = getPgpAppPreference()

        // do again, maybe an app has now been installed
        preference?.populateAppList()

        // Init ArrayAdapter with OpenPGP Providers
        val adapter: ListAdapter = object : ArrayAdapter<OpenPgpAppPreference.OpenPgpProviderEntry>(
            requireContext(),
            R.layout.select_dialog_singlechoice, R.id.text1, preference?.entries!!
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                // User super class to create the View
                val v = super.getView(position, convertView, parent)
                val tv = v.findViewById<View>(R.id.text1) as TextView

                // Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(
                    preference?.entries!![position].icon, null,
                    null, null
                )

                // Add margin between image and text (support various screen densities)
                val dp10 =
                    (10 * context.resources.displayMetrics.density + 0.5f).toInt()
                tv.compoundDrawablePadding = dp10
                return v
            }
        }
        builder.setSingleChoiceItems(adapter, preference.getIndexOfProviderList(preference.value),
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    if(which == DialogInterface.BUTTON_POSITIVE) {
                        return
                    }

                    val entry: OpenPgpAppPreference.OpenPgpProviderEntry = preference.entries[which]
                    if (entry.intent != null) {
                        /*
                         * Intents are called as activity
                         *
                         * Current approach is to assume the user installed the app.
                         * If he does not, the selected package is not valid.
                         *
                         * However  applications should always consider this could happen,
                         * as the user might remove the currently used OpenPGP app.
                         */
                        context!!.startActivity(entry.intent)
                        return
                    }
                    preference.value = entry.packageName

                    /*
                     * Clicking on an item simulates the positive button click, and dismisses
                     * the dialog.
                     */
                    onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                    dialog.dismiss()
                }
            })

        /*
         * The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
         * dialog instead of the user having to press 'Ok'.
         */
        builder.setPositiveButton(null, null)
    }

}