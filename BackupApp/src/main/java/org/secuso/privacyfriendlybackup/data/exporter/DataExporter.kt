package org.secuso.privacyfriendlybackup.data.exporter

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import java.io.IOException
import java.io.OutputStreamWriter

class DataExporter {
    companion object {
        suspend fun exportData(context: Context, uri: Uri, data: BackupDataStorageRepository.BackupData) : Boolean{
            return withContext(Dispatchers.IO) {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(
                        context.contentResolver.openFileDescriptor(uri, "w")
                    ).use { out ->
                        JsonWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { writer ->
                            writer.setIndent("  ")
                            writer.beginObject()
                            writeData(writer, data)
                            writer.endObject()
                        }
                    }
                } catch (e : IOException) {
                    return@withContext false
                }
                return@withContext true
            }
        }

        private fun writeData(writer: JsonWriter, metaData: BackupDataStorageRepository.BackupData) {
            writer.name("filename").value(metaData.filename)
            writer.name("packageName").value(metaData.packageName)
            writer.name("timestamp").value(metaData.timestamp.time)
            writer.name("encrypted").value(metaData.encrypted)
            writer.name("data").value(String(metaData.data!!))
        }
    }
}