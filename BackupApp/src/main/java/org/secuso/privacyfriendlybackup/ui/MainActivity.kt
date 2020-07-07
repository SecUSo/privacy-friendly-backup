package org.secuso.privacyfriendlybackup.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/**
 * @author Christopher Beckmann
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)


        startActivity(Intent(this, MainActivity2::class.java))
    }
}