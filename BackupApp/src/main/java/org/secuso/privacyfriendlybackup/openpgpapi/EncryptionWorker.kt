package org.secuso.privacyfriendlybackup.openpgpapi

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class EncryptionWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        TODO("Not yet implemented")
    }

}