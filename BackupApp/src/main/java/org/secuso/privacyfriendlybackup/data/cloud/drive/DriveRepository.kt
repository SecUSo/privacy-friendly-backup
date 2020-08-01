package org.secuso.privacyfriendlybackup.data.cloud.drive

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager
import retrofit2.Retrofit


class DriveRepository {
    val TAG = "PFA Drive"

    val googleAccountManager : GoogleAccountManager

    constructor(context: Context) {
        googleAccountManager = GoogleAccountManager(context)
    }

    fun checkPermission(context: Context) : Boolean {
        val p1 = ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS)
        val p2 = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        return p1 == PackageManager.PERMISSION_GRANTED && p2 == PackageManager.PERMISSION_GRANTED
    }

    val driveService = Retrofit.Builder()
        .baseUrl("https://")

}