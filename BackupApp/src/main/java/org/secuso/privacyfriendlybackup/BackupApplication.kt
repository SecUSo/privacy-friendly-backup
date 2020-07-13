package org.secuso.privacyfriendlybackup

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.work.*
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.worker.BackupJobManagerWorker
import java.util.concurrent.TimeUnit

class BackupApplication : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_ID = "org.secuso.privacyfriendlybackup.CHANNEL_ID"

        private const val TAG = "PFA App"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        schedulePeriodicWork()
    }

    fun schedulePeriodicWork() {
        Log.d(TAG, "schedulePeriodicWork()")
        val periodicJobManagerWork =
            PeriodicWorkRequestBuilder<BackupJobManagerWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BuildConfig.APPLICATION_ID,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicJobManagerWork
        )
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

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
}