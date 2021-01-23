package org.secuso.privacyfriendlybackup.data.importer

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.JsonReader
import android.util.JsonWriter
import android.util.MalformedJsonException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.secuso.privacyfriendlybackup.data.BackupDataStorageRepository
import org.secuso.privacyfriendlybackup.data.room.model.enums.StorageType
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*

class DataImporter {

    companion object {
        suspend fun importData(context: Context, uri: Uri) : Pair<Boolean, BackupDataStorageRepository.BackupData?> {
            return withContext(IO) {
                var backupData : BackupDataStorageRepository.BackupData? = null

                val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                val inStream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)

                try {
                    JsonReader(InputStreamReader(inStream, Charsets.UTF_8)).use { reader ->
                        reader.beginObject()
                        backupData = readData(reader)
                        reader.endObject()
                    }
                } catch (e : MalformedJsonException) {
                    return@withContext false to null
                } catch (e : IOException) {
                    return@withContext false to null
                }

                val result : Pair<Boolean, Long>

                if(backupData != null) {
                    result = BackupDataStorageRepository.getInstance(context).storeFile(context, backupData!!)
                } else {
                    return@withContext false to null
                }

                val (_,id) = result
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

        private fun readData(reader: JsonReader) : BackupDataStorageRepository.BackupData? {
            var filename : String? = null
            var packageName : String? = null
            var timestamp : Long? = null
            var encrypted : Boolean? = null
            var data : String? = null

            while (reader.hasNext()) {
                val nextName = reader.nextName()

                when(nextName) {
                    "filename" -> filename = reader.nextString()
                    "packageName" -> packageName = reader.nextString()
                    "timestamp" -> timestamp = reader.nextLong()
                    "encrypted" -> encrypted = reader.nextBoolean()
                    "data" -> data = reader.nextString()
                    else -> throw MalformedJsonException("Metadata is malformed.")
                }
            }

            if(TextUtils.isEmpty(filename)
                || TextUtils.isEmpty(packageName)
                || TextUtils.isEmpty(data)
                || timestamp == null
                || encrypted == null) {
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
    }
}