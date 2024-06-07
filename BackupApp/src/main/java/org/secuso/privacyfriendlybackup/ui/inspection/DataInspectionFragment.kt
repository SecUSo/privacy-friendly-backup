package org.secuso.privacyfriendlybackup.ui.inspection

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import org.openintents.openpgp.OpenPgpSignatureResult
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.databinding.DataInspectionFragmentBinding
import org.secuso.privacyfriendlybackup.databinding.ItemApplicationJobBinding
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys
import org.secuso.privacyfriendlybackup.ui.backup.BackupOverviewFragment
import java.io.FileNotFoundException


class DataInspectionFragment : Fragment() {

    private var exportMenuItem : MenuItem? = null
    private var encryptionInfoMenuItem : MenuItem? = null
    private var onlyExportData : Boolean = false
    private var exportEncrypted : Boolean = false
    private var encryptionEnabled : Boolean = false
    private var showSigInfo: Boolean = false

    lateinit var dataBinding: DataInspectionFragmentBinding

    companion object {
        fun newInstance() = DataInspectionFragment()

        const val TAG : String = "DataInspectionFragment"
        const val REQUEST_CODE_CREATE_DOCUMENT : Int = 251
    }

    private lateinit var viewModel: DataInspectionViewModel

    //private lateinit var test : ActivityResultLauncher<>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dataBinding = DataInspectionFragmentBinding.inflate(layoutInflater)
        return dataBinding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_data_inspection, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        exportMenuItem = menu.findItem(R.id.action_export)
        encryptionInfoMenuItem = menu.findItem(R.id.action_encryption_info)
        val icon = exportMenuItem?.icon
        if(icon != null) {
            icon.mutate()
            icon.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.white), PorterDuff.Mode.SRC_ATOP)
            icon.alpha = 255
        }

        if(viewModel.getLoadStatus().value as LoadStatus == LoadStatus.DONE) {
            exportMenuItem?.isEnabled = true
            exportMenuItem?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                true
            }
            R.id.action_export -> {
                handleExportClicked()
                true
            }
            R.id.action_encryption_info -> {
                toggleShowSignatureInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleShowSignatureInfo() {
        showSigInfo = !showSigInfo

        val set = ConstraintSet()
        set.clone(dataBinding.dataInspection)
        set.setVisibility(R.id.data_inspection_encryption_details, if(showSigInfo) View.VISIBLE else View.GONE)
        set.applyTo(dataBinding.dataInspection)
    }

    private fun handleExportClicked() {
        showExportConfirmationDialog()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT) {
            when(resultCode) {
                AppCompatActivity.RESULT_OK -> {
                    if(data != null && data.data != null) {
                        try {
                            viewModel.exportData(data.data, exportEncrypted)
                        } catch (e : FileNotFoundException) {
                            Log.e(TAG, e.message, e)
                        }
                    }
                }
                AppCompatActivity.RESULT_CANCELED -> {
                    /* canceled */
                }
            }
            // if we only exported data.. then we are done now and exit the activity immediately
            if(onlyExportData) {
                activity?.finish()
            }
        }
    }

    private fun showExportConfirmationDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_data_export_confirmation, null)

        AlertDialog.Builder(requireActivity()).apply {
            setTitle(R.string.data_export_confirmation_dialog_title)
            setIcon(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_save_alt_24)?.apply {
                this.setTint(ContextCompat.getColor(requireActivity(), R.color.colorAccent))
            })
            if(encryptionEnabled) {
                setView(view)
            }
            setMessage(R.string.data_export_confirmation_dialog_message)

            val checkBox = view.findViewById<CheckBox>(R.id.dialog_data_export_encrypted_checkbox)
            val warning = view.findViewById<TextView>(R.id.dialog_data_export_encrypted_warning)

            setPositiveButton(R.string.data_export_confirmation_dialog_confirm) { d, _ ->
                exportEncrypted = checkBox.isChecked and encryptionEnabled

                val filename = viewModel.getFileName(exportEncrypted)
                if(TextUtils.isEmpty(filename)) {
                    d.dismiss()
                    return@setPositiveButton
                }

                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_TITLE, filename)
                    startActivityForResult(Intent.createChooser(this, ""), REQUEST_CODE_CREATE_DOCUMENT)
                }
                d.dismiss()
            }
            setNegativeButton(R.string.data_export_confirmation_dialog_cancel) { d, _ ->
                d.dismiss()
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                warning.visibility = if(isChecked) View.GONE else View.VISIBLE
            }

        }.create().apply {
            setOnShowListener {
                val checkBox = view.findViewById<CheckBox>(R.id.dialog_data_export_encrypted_checkbox)
                val warning = view.findViewById<TextView>(R.id.dialog_data_export_encrypted_warning)

                if(encryptionEnabled) {
                    warning.visibility = if (checkBox.isChecked) View.GONE else View.VISIBLE
                } else {
                    checkBox.visibility = View.GONE
                    checkBox.isChecked = false
                    warning.visibility = View.GONE
                }
            }
        }.show()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()
        viewModel = ViewModelProvider(activity)[DataInspectionViewModel::class.java]

        encryptionEnabled = PreferenceManager.getDefaultSharedPreferences(requireActivity()).getBoolean(PreferenceKeys.PREF_ENCRYPTION_ENABLE, false)

        val dataId = arguments?.getLong(DataInspectionActivity.EXTRA_DATA_ID)
        onlyExportData = arguments?.getBoolean(DataInspectionActivity.EXTRA_EXPORT_DATA, false) ?: false

        if(dataId == null) {
            // end activity if no data is present
            Toast.makeText(
                activity,
                R.string.activity_data_inspection_error_id_not_valid,
                Toast.LENGTH_SHORT
            ).show()
            activity.finish()
            return
        }

        viewModel.loadData(dataId)

        //viewModel.loadPath(currentPath)

        viewModel.getLoadStatus().observe(viewLifecycleOwner) {
            Log.d("TEST", "## Load Status Updated To ${it.name}")
            Glide.with(requireActivity()).load(it.imageRes).into(dataBinding.dataInspectionSignatureStatus)
            dataBinding.dataInspectionLoadStatusName.setText(it.descriptionRes)
            dataBinding.dataInspectionLoadStatusImage.setColorFilter(ContextCompat.getColor(requireActivity(), it.colorRes))
            when(it) {
                LoadStatus.UNKNOWN, null -> {
                    dataBinding.dataInspectionLoadStatusName.visibility = View.GONE
                }
                LoadStatus.ERROR_INVALID_JSON, LoadStatus.ERROR -> {
                    dataBinding.dataInspectionLoadStatusName.visibility = View.VISIBLE
                }
                LoadStatus.LOADING -> {
                    dataBinding.dataInspectionLoadStatusName.visibility = View.VISIBLE
                }
                LoadStatus.DECRYPTING -> {
                    dataBinding.dataInspectionLoadStatusName.visibility = View.VISIBLE
                }
                LoadStatus.DECRYPTION_ERROR -> {
                    dataBinding.dataInspectionLoadStatusName.visibility = View.VISIBLE
                }
                LoadStatus.DONE -> {
                    dataBinding.dataInspectionLoadStatusName.visibility = View.GONE

                    encryptionEnabled = encryptionEnabled and viewModel.isEncrypted

                    if(onlyExportData) {
                        handleExportClicked()
                    }

                    exportMenuItem?.isEnabled = true
                    exportMenuItem?.isVisible = true
                }
            }
        }

        viewModel.getData().observe(viewLifecycleOwner) {
            dataBinding.dataInspectionJsonList.bindJson(it)
        }

        // Color
        dataBinding.dataInspectionJsonList.setKeyColor(
            ContextCompat.getColor(
                activity,
                R.color.colorPrimary
            )
        )
        dataBinding.dataInspectionJsonList.setValueTextColor(ContextCompat.getColor(activity, R.color.green))
        dataBinding.dataInspectionJsonList.setValueNumberColor(
            ContextCompat.getColor(
                activity,
                R.color.colorAccent
            )
        )
        dataBinding.dataInspectionJsonList.setValueUrlColor(ContextCompat.getColor(activity, R.color.red))
        dataBinding.dataInspectionJsonList.setValueNullColor(
            ContextCompat.getColor(
                activity,
                R.color.orange
            )
        )
        dataBinding.dataInspectionJsonList.setBracesColor(ContextCompat.getColor(activity, R.color.black))
        //data_inspection_json_list.setTextSize()

        viewModel.getDecryptionMetaData().observe(requireActivity()) {
            encryptionInfoMenuItem?.isEnabled = true
            encryptionInfoMenuItem?.isVisible = true

            var statusText = ""
            var statusIcon = (ContextCompat.getDrawable(requireActivity(), R.drawable.ic_close_24))
            var color = ContextCompat.getColor(requireActivity(), R.color.red)
            when(it.signature?.result) {
                OpenPgpSignatureResult.RESULT_NO_SIGNATURE -> {
                    // not signed
                    statusText = requireActivity().getString(R.string.signature_result_no_signature)
                    dataBinding.dataInspectionSignatureUserId.visibility = View.GONE
                    dataBinding.dataInspectionSignatureKeyId.visibility = View.GONE
                }
                OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE -> {
                    // invalid signature
                    statusText = requireActivity().getString(R.string.signature_result_no_signature)
                }
                OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED -> {
                    // everything is fine
                    statusText = requireActivity().getString(R.string.signature_result_valid)
                    statusIcon = (ContextCompat.getDrawable(requireActivity(), R.drawable.ic_check_24))
                    color = ContextCompat.getColor(requireActivity(), R.color.green)
                }
                OpenPgpSignatureResult.RESULT_KEY_MISSING -> {
                    // no key was found for this signature verification
                    statusText = requireActivity().getString(R.string.signature_result_key_missing)
                }
                OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED -> {
                    // successfully verified signature, but with unconfirmed key
                    statusText = requireActivity().getString(R.string.signature_result_key_unconfirmed)
                }
                OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED -> {
                    // key has been revoked -> invalid signature!
                    statusText = requireActivity().getString(R.string.signature_result_key_revoked)
                }
                OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED -> {
                    // key is expired -> invalid signature!
                    statusText = requireActivity().getString(R.string.signature_result_key_expired)
                }
                OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE -> {
                    // insecure cryptographic algorithms/protocol -> invalid signature!
                    statusText = requireActivity().getString(R.string.signature_result_key_insecure)
                }
                else -> {}
            }

            statusIcon?.apply {
                mutate()
                setTint(color)
            }
            dataBinding.dataInspectionSignatureStatus.setImageDrawable(statusIcon)
            dataBinding.dataInspectionSignatureStatusText.text = statusText
            dataBinding.dataInspectionSignatureUserId.text = requireActivity().getString(R.string.data_inspection_signature_user_id, it.signature?.primaryUserId)
            dataBinding.dataInspectionSignatureKeyId.text = requireActivity().getString(R.string.data_inspection_signature_key_id, it.signature?.keyId.toString())

        }
    }
}