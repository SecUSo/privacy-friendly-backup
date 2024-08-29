package org.secuso.privacyfriendlybackup.ui.encryption

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.secuso.privacyfriendlybackup.R
import org.secuso.privacyfriendlybackup.databinding.ActivityDisplayMenuItemBinding
import org.secuso.privacyfriendlybackup.ui.backup.BackupOverviewFragment

class EncryptionSettingsActivity : AppCompatActivity(){
    lateinit var binding: ActivityDisplayMenuItemBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayMenuItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.app_name)
    }
}