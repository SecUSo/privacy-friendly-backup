package org.secuso.privacyfriendlybackup.ui.inspection

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import org.secuso.privacyfriendlybackup.R


class DataInspectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATA_PATH: String = "DataInspectionActivity.EXTRA_DATA_PATH"
        const val EXTRA_DATA_ID : String = "DataInspectionActivity.EXTRA_DATA_ID"
        const val EXTRA_EXPORT_DATA : String = "DataInspectionActivity.EXTRA_EXPORT_DATA"
    }

    private lateinit var viewModel: DataInspectionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getLongExtra(EXTRA_DATA_ID, -1)
        val export = intent.getBooleanExtra(EXTRA_EXPORT_DATA, false)

        if(id == -1L) {
            Toast.makeText(
                this,
                R.string.activity_data_inspection_error_id_not_valid,
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        setContentView(R.layout.data_inspection_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[DataInspectionViewModel::class.java]

        if (savedInstanceState == null) {
            val fragment = DataInspectionFragment.newInstance()
            fragment.arguments = Bundle().apply {
                putLong(EXTRA_DATA_ID, id)
                putBoolean(EXTRA_EXPORT_DATA, export)
                putStringArray(EXTRA_DATA_PATH, emptyArray())
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitNow()
        }
    }

    override fun onBackPressed() {
        finish()
    }
}