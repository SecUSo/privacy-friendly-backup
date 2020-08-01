package org.secuso.privacyfriendlybackup.data.room.model.enums

import androidx.annotation.StringRes
import org.secuso.privacyfriendlybackup.R

enum class StorageType(
    @StringRes val nameResId : Int
) {
    EXTERNAL(R.string.storage_type_external),
    CLOUD(R.string.storage_type_drive);

    companion object {
        @JvmStatic
        fun getStorageOptions() = listOf(EXTERNAL)
    }
}