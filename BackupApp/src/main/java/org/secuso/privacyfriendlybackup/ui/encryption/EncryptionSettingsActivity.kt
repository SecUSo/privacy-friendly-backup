package org.secuso.privacyfriendlybackup.ui.encryption

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.main_activity.*
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.ui.backup.BackupOverviewFragment

class EncryptionSettingsActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        initResources()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    BackupOverviewFragment.newInstance()
                )
                .commitNow()
        }
    }

    private fun initResources() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.app_name)
    }
}