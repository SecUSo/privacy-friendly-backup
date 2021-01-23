package org.secuso.privacyfriendlybackup.util

import android.text.format.DateFormat
import java.util.*

object BackupDataUtil {

    fun getFileName(date: Date, packageName: String, encrypted : Boolean = false) : String {
        val sb = StringBuilder()
        sb.append(packageName).append('_')
        sb.append(DateFormat.format("yyyy_MM_dd_HHmmss", date.time))
        if(encrypted) {
            sb.append('_').append("encrypted")
        }
        sb.append(".backup")
        return sb.toString()
    }
}

