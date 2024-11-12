package org.secuso.privacyfriendlybackup.data.importer

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.JsonReader
import android.util.MalformedJsonException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import java.io.IOException
import java.io.InputStreamReader
import java.util.Date
import java.util.LinkedList
import java.util.zip.ZipInputStream


class DataImporter {

    companion object {
        suspend fun importDataZip(context: Context, uri: Uri): List<Pair<Boolean, Long?>>? {
            return withContext(IO) {
                val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                val inStream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
                val zipInStream = ZipInputStream(inStream)
                val result: MutableList<Pair<Boolean, Long?>> = LinkedList()

                try {
                    zipInStream.use { zipInputStream ->
                        generateSequence { zipInputStream.nextEntry }
                            .filterNot { it.isDirectory }
                            .forEach { _ ->
                                var backupData: BackupDataStorageRepository.BackupData? = null
                                val isr = InputStreamReader(zipInputStream, Charsets.UTF_8)
                                val jr = JsonReader(isr)
                                jr.let { reader ->
                                    reader.beginObject()
                                    backupData = readData(reader)
                                    reader.endObject()
                                }
                                if (backupData != null) {

                                    if (!backupData!!.encrypted) {
                                        // short validation check if the json is valid
                                        if (!isValidJSON(String(backupData!!.data!!))) {
                                            result.add(false to null)
                                        }
                                    }

                                    result.add(
                                        BackupDataStorageRepository.getInstance(context).storeFile(
                                            context,
                                            backupData!!
                                        )
                                    )
                                } else {
                                    result.add(false to null)
                                }
                            }
                    }
                } catch (e: MalformedJsonException) {
                    return@withContext null
                } catch (e: IOException) {
                    return@withContext null
                }
                return@withContext result
            }
        }

        suspend fun importData(context: Context, uri: Uri): Pair<Boolean, BackupDataStorageRepository.BackupData?> {
            return withContext(IO) {
                var backupData: BackupDataStorageRepository.BackupData? = null

                val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                val inStream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)

                try {
                    JsonReader(InputStreamReader(inStream, Charsets.UTF_8)).use { reader ->
                        reader.beginObject()
                        backupData = readData(reader)
                        reader.endObject()
                    }
                } catch (e: MalformedJsonException) {
                    return@withContext false to null
                } catch (e: IOException) {
                    return@withContext false to null
                }

                val result: Pair<Boolean, Long>

                if (backupData != null) {

                    if (!backupData!!.encrypted) {
                        // short validation check if the json is valid
                        if (!isValidJSON(String(backupData!!.data!!))) {
                            return@withContext false to null
                        }
                    }

                    result = BackupDataStorageRepository.getInstance(context).storeFile(
                        context,
                        backupData!!
                    )
                } else {
                    return@withContext false to null
                }

                val (_, id) = result
                val resultBackUpData = BackupDataStorageRepository.BackupData(
                    id, // return the real database id it was imported to
                    backupData!!.filename,
                    backupData!!.packageName,
                    backupData!!.timestamp,
                    backupData!!.data,
                    backupData!!.encrypted,
                    backupData!!.storageType,
                    backupData!!.available
                )

                return@withContext true to resultBackUpData
            }
        }

        private fun readData(reader: JsonReader): BackupDataStorageRepository.BackupData? {
            var filename: String? = null
            var packageName: String? = null
            var timestamp: Long? = null
            var encrypted: Boolean? = null
            var data: String? = null

            while (reader.hasNext()) {
                val nextName = reader.nextName()

                when (nextName) {
                    "filename" -> filename = reader.nextString()
                    "packageName" -> packageName = reader.nextString()
                    "timestamp" -> timestamp = reader.nextLong()
                    "encrypted" -> encrypted = reader.nextBoolean()
                    "data" -> data = reader.nextString()
                    else -> throw MalformedJsonException("Metadata is malformed.")
                }
            }

            if (TextUtils.isEmpty(filename)
                || TextUtils.isEmpty(packageName)
                || TextUtils.isEmpty(data)
                || timestamp == null
                || encrypted == null
            ) {
                return null
            }

            return BackupDataStorageRepository.BackupData(
                filename = filename!!,
                packageName = packageName!!,
                timestamp = Date(timestamp),
                data = data!!.toByteArray(Charsets.UTF_8),
                encrypted = encrypted,
                storageType = StorageType.EXTERNAL,
                available = true
            )
        }

        fun isValidJSON(test: String?): Boolean {
            try {
                JSONObject(test)
            } catch (ex: JSONException) {
                try {
                    JSONArray(test)
                } catch (ex1: JSONException) {
                    return false
                }
            }
            return true
        }
    }
}