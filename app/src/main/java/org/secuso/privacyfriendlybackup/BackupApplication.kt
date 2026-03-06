package org.secuso.privacyfriendlybackup

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.launch
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import org.secuso.privacyfriendlybackup.worker.BackupJobManagerWorker
import java.util.concurrent.TimeUnit

class BackupApplication : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_ID = "org.secuso.privacyfriendlybackup.CHANNEL_ID"

        private const val TAG = "PFA App"
    }

    override fun onCreate() {
        super.onCreate()

        if (applicationContext != null) {
            with(ProcessLifecycleOwner.get()) {
                lifecycleScope.launch {
                    PreferenceManager.getDefaultSharedPreferences(applicationContext).apply{
                        val current = getString("pref_storage_type", null)
                        if (current == null) {
                            val total = BackupDatabase.getInstance(applicationContext)
                                .backupMetaDataDao()
                                .getTotal()
                            edit(commit = true) {
                                putString("pref_storage_type", if (total > 0) {
                                    StorageType.EXTERNAL.toString()
                                } else {
                                    StorageType.INTERNAL.toString()
                                })
                            }
                        }
                    }
                }
            }
        }

        createNotificationChannel()
        schedulePeriodicWork()
    }

    fun schedulePeriodicWork() {
        Log.d(TAG, "schedulePeriodicWork()")
        val periodicJobManagerWork =
            PeriodicWorkRequestBuilder<BackupJobManagerWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.ERROR)
            .build()
}