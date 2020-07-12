package org.secuso.privacyfriendlybackup.util

import android.text.format.DateFormat
import java.util.*

object BackupDataUtil {

    fun getFileName(date: Date, packageName: String) : String {
        val date = Date()
        val dateString: CharSequence = DateFormat.format("yyyy_MM_dd_HHmmss", date.time)
        return "${packageName}_${dateString}.backup"
    }
}

