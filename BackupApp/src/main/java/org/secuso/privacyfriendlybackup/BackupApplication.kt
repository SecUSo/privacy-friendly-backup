package org.secuso.privacyfriendlybackup

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BackupApplication : Application() {

    companion object {
        const val CHANNEL_ID = "org.secuso.privacyfriendlybackup.CHANNEL_ID"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = descriptionText

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}