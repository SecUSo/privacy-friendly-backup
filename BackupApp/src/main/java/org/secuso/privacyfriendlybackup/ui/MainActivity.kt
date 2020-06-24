package org.secuso.privacyfriendlybackup.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.secuso.privacyfriendlybackup.R

/**
 * @author Christopher Beckmann
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}