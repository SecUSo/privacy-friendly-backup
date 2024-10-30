package org.secuso.privacyfriendlybackup.data.exporter

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DataExporter {
    companion object {
        suspend fun exportDataZip(context: Context, uri: Uri, data: Set<BackupDataStorageRepository.BackupData>): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    ZipOutputStream(ParcelFileDescriptor.AutoCloseOutputStream(context.contentResolver.openFileDescriptor(uri, "w"))).apply {
                        setLevel(5)
                        setComment("PFA Backup Export")
                    }.use { zipOut ->
                        data.forEach { backupData ->
                            val zipEntry = ZipEntry(getSingleExportFileName(backupData, backupData.encrypted))
                            zipOut.putNextEntry(zipEntry)

                            val osw = OutputStreamWriter(zipOut, Charsets.UTF_8)
                            val bw = BufferedWriter(osw)
                            val jw = JsonWriter(bw)
                            jw.let { writer ->
                                writer.setIndent("  ")
                                writer.beginObject()
                                writeData(writer, backupData)
                                writer.endObject()
                            }
                            jw.flush()
                            bw.flush()
                            osw.flush()

                            zipOut.closeEntry()
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    return@withContext false
                }
                return@withContext true
            }
        }

        suspend fun exportData(context: Context, uri: Uri, data: BackupDataStorageRepository.BackupData): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    JsonWriter(
                        OutputStreamWriter(
                            ParcelFileDescriptor.AutoCloseOutputStream(context.contentResolver.openFileDescriptor(uri, "w")),
                            Charsets.UTF_8
                        )
                    ).use { writer ->
                        writer.setIndent("  ")
                        writer.beginObject()
                        writeData(writer, data)
                        writer.endObject()
                    }
                } catch (e: IOException) {
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

        /**
         * Filename for export containing multiple backups (.zip)
         */
        fun getMultipleExportFileName(encrypted: Boolean): String {
            val sb = StringBuffer()
            sb.append("PfaBackup_")
            val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.getDefault())
            sdf.format(Calendar.getInstance().time, sb, FieldPosition(SimpleDateFormat.DATE_FIELD))
            sb.append(".zip")
            return sb.toString()
        }

        /**
         * Filename for single Backup export (.backup)
         */
        fun getSingleExportFileName(backupMetaData: BackupDataStorageRepository.BackupData, encrypted: Boolean): String {
            return if (backupMetaData.encrypted && encrypted) {
                getEncryptedFilename(backupMetaData.filename)
            } else {
                getUnencryptedFilename(backupMetaData.filename)
            }
        }


        private fun getUnencryptedFilename(filename: String) =
            filename.replace("_encrypted.backup", ".backup")

        private fun getEncryptedFilename(filename: String): String {
            return if (filename.contains("_encrypted.backup", true)) {
                filename
            } else {
                filename.replace(".backup", "_encrypted.backup", true)
            }
        }
    }
}