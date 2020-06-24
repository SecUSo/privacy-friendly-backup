package org.secuso.privacyfriendlybackup.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import org.secuso.privacyfriendlybackup.api.IBackupService

/**
 * @author Christopher Beckmann
 */
class AuthenticationService : Service() {

    private val mBinder : IBackupService.Stub = object : IBackupService.Stub() {
        override fun execute(data: Intent?): Intent {
            TODO("Not yet implemented")
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder;
    }
}