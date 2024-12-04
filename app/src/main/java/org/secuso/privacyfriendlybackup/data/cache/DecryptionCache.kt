package org.secuso.privacyfriendlybackup.data.cache

import org.secuso.privacyfriendlybackup.data.room.model.InternalBackupData

object DecryptionCache {
    private const val MAX_CACHE_SIZE : Int = 4

    private val cache = object : LinkedHashMap<String, DecryptionMetaData>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DecryptionMetaData>?): Boolean {
            return this.size > MAX_CACHE_SIZE
        }
    }

    fun writeDecryptionMetaData(data: InternalBackupData, decryptionMetaData: DecryptionMetaData) {
        cache[getKey(data)] = decryptionMetaData
    }

    fun getDecryptionMetaData(data : InternalBackupData) : DecryptionMetaData? {
        return cache[getKey(data)]
    }

    private fun getKey(data: InternalBackupData) = "${data.packageName}_${data.timestamp.time}"
}