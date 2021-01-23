package org.secuso.privacyfriendlybackup.ui.importbackup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.importer.DataImporter
import org.secuso.privacyfriendlybackup.ui.main.MainActivity
import java.text.SimpleDateFormat

class ImportBackupActivity : AppCompatActivity() {

    companion object {
        const val ACTION_OPEN_FILE = "ImportBackupActivity.ACTION_OPEN_FILE"
        const val REQUEST_CODE_OPEN_DOCUMENT : Int = 362
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_backup)

        when(intent?.action ) {
            ACTION_OPEN_FILE -> {
                sendOpenIntent()
            }
            Intent.ACTION_VIEW -> {
                intent.data?.also { uri ->
                    showImportConfirmationDialog(uri)
                }
            }
            else -> {
                finish()
            }
        }
    }

    private fun showImportConfirmationDialog(uri : Uri) {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.data_import_confirmation_dialog_title)
            setIcon(ContextCompat.getDrawable(this@ImportBackupActivity, R.drawable.ic_outline_info_24)?.apply {
                this.setTint(ContextCompat.getColor(this@ImportBackupActivity, R.color.colorAccent))
            })
            setMessage(R.string.data_import_confirmation_dialog_message)
            setPositiveButton(R.string.data_import_confirmation_dialog_confirm) { d, _ ->
                d.dismiss()
                import(uri)
            }
            setNegativeButton(R.string.data_import_confirmation_dialog_cacncel) { d, _ ->
                d.dismiss()
                finish()
            }
        }.create().show()
    }

    private fun sendOpenIntent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == RESULT_OK) {
            data?.data?.also { uri ->
                import(uri)
            }
        } else {
            finish()
        }
    }

    fun import(uri: Uri) {
        GlobalScope.launch(IO) {
            val (success, data) = DataImporter.importData(this@ImportBackupActivity, uri)

            withContext(Main) {
                if(success) {
                    showSuccessDialog(data!!)
                } else {
                    showErrorDialog()
                }
            }
        }
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.data_import_error_dialog_title)
            setIcon(ContextCompat.getDrawable(this@ImportBackupActivity, R.drawable.ic_baseline_error_outline_24)?.apply {
                this.setTint(ContextCompat.getColor(this@ImportBackupActivity, R.color.red))
            })
            setMessage(R.string.data_import_error_dialog_message)
            setPositiveButton(R.string.data_import_error_dialog_confirm) { _,_ ->
                Intent(this@ImportBackupActivity, MainActivity::class.java).apply {
                    //flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.SELECTED_MENU_ITEM, MainActivity.MenuItem.MENU_MAIN_BACKUP_OVERVIEW.name)
                    startActivity(this)
                }
                this@ImportBackupActivity.finish()
            }
        }.create().show()
    }

    private fun showSuccessDialog(data: BackupDataStorageRepository.BackupData) {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.data_import_success_dialog_title)
            setIcon(ContextCompat.getDrawable(this@ImportBackupActivity, R.drawable.ic_check_box_24)?.apply {
                this.setTint(ContextCompat.getColor(this@ImportBackupActivity, R.color.green))
            })
            setMessage(
                getString(
                    R.string.data_import_success_dialog_message,
                    arrayOf<String>(data.packageName, SimpleDateFormat.getDateTimeInstance().format(data.timestamp))
                )
            )
            setPositiveButton(R.string.data_import_success_dialog_confirm) { _, _ ->
                Intent(this@ImportBackupActivity, MainActivity::class.java).apply {
                    //flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(MainActivity.SELECTED_MENU_ITEM, MainActivity.MenuItem.MENU_MAIN_BACKUP_OVERVIEW.name)
                    //putExtra(MainActivity.FILTER, data.packageName)
                    putExtra(MainActivity.BACKUP_ID, data.id)
                    startActivity(this)
                }
                this@ImportBackupActivity.finish()
            }
        }.create().show()
    }
}