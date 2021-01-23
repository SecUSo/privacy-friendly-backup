package org.secuso.privacyfriendlybackup.ui.data

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.data_inspection_fragment.*
import kotlinx.android.synthetic.main.item_application_job.*
import org.secuso.privacyfriendlybackup.R
import java.io.FileNotFoundException
import java.io.FileOutputStream


class DataInspectionFragment : Fragment() /*, DataInspectionAdapter.DataInspectionOnItemClickListener */{

    private var exportMenuItem : MenuItem? = null
    private var onlyExportData : Boolean = false
    private var exportEncrypted : Boolean = false

    companion object {
        fun newInstance() = DataInspectionFragment()

        const val TAG : String = "DataInspectionFragment"
        const val REQUEST_CODE_CREATE_DOCUMENT : Int = 251
    }

    private lateinit var viewModel: DataInspectionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.data_inspection_fragment, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_data_inspection, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        exportMenuItem = menu.findItem(R.id.action_export)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                true
            }
            R.id.action_export -> {
                // TODO: export encrypted? Introduce Dialog and let the user choose
                handleExportClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleExportClicked() {
        val filename = viewModel.getFileName(exportEncrypted)
        if(TextUtils.isEmpty(filename)) {
            return
        }

        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, filename)
            startActivityForResult(Intent.createChooser(this, ""), REQUEST_CODE_CREATE_DOCUMENT)
        }
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val dataId = arguments?.getLong(DataInspectionActivity.EXTRA_DATA_ID)
        onlyExportData = arguments?.getBoolean(DataInspectionActivity.EXTRA_EXPORT_DATA, false) ?: false
        val activity = requireActivity()

        viewModel = ViewModelProvider(activity).get(DataInspectionViewModel::class.java)

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
            Glide.with(requireActivity()).load(it.imageRes).into(image)
            name.setText(it.descriptionRes)
            image.setColorFilter(ContextCompat.getColor(requireActivity(), it.colorRes))
            when(it) {
                LoadStatus.UNKNOWN -> {
                    data_inspection_load_status.visibility = View.GONE
                }
                LoadStatus.ERROR -> {
                    data_inspection_load_status.visibility = View.VISIBLE
                }
                LoadStatus.LOADING -> {
                    data_inspection_load_status.visibility = View.VISIBLE
                }
                LoadStatus.DECRYPTING -> {
                    data_inspection_load_status.visibility = View.VISIBLE
                }
                LoadStatus.DECRYPTION_ERROR -> {
                    data_inspection_load_status.visibility = View.VISIBLE
                }
                LoadStatus.DONE -> {
                    data_inspection_load_status.visibility = View.GONE

                    if(onlyExportData) {
                        handleExportClicked()
                    }

                    exportMenuItem?.isEnabled = true
                }
            }
        }

        viewModel.getData().observe(viewLifecycleOwner) {
            data_inspection_json_list.bindJson(it)
        }

        // Color
        data_inspection_json_list.setKeyColor(
            ContextCompat.getColor(
                activity,
                R.color.colorPrimary
            )
        )
        data_inspection_json_list.setValueTextColor(ContextCompat.getColor(activity, R.color.green))
        data_inspection_json_list.setValueNumberColor(
            ContextCompat.getColor(
                activity,
                R.color.colorAccent
            )
        )
        data_inspection_json_list.setValueUrlColor(ContextCompat.getColor(activity, R.color.red))
        data_inspection_json_list.setValueNullColor(
            ContextCompat.getColor(
                activity,
                R.color.orange
            )
        )
        data_inspection_json_list.setBracesColor(ContextCompat.getColor(activity, R.color.black))
        //data_inspection_json_list.setTextSize()
    }
}