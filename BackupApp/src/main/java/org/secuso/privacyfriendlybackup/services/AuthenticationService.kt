package org.secuso.privacyfriendlybackup.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class AuthenticationService : Service() {

    private val mBinder : IBinder = object : Binder() {
        // TODO: Authenticate PFA here
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder;
    }

}