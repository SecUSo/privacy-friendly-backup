package org.secuso.privacyfriendlybackup.ui.inspection

import org.secuso.privacyfriendlybackup.R

enum class LoadStatus(val imageRes: Int, val descriptionRes: Int, val colorRes : Int) {
    UNKNOWN(R.drawable.ic_about_24, R.string.data_inspection_load_status_unknown, R.color.colorAccent),
    LOADING(R.drawable.ic_about_24, R.string.data_inspection_load_status_loading, R.color.colorAccent),
    DECRYPTING(R.drawable.ic_lock_open_24, R.string.data_inspection_load_status_decrypting, R.color.colorAccent),
    ERROR(R.drawable.ic_baseline_error_outline_24, R.string.data_inspection_load_status_error, R.color.red),
    ERROR_INVALID_JSON(R.drawable.ic_baseline_error_outline_24, R.string.data_inspection_load_status_error_invalid_json, R.color.red),
    DECRYPTION_ERROR(R.drawable.ic_baseline_no_encryption_24, R.string.data_inspection_load_status_decryption_error, R.color.red),
    DONE(R.drawable.ic_about_24, R.string.data_inspection_load_status_done, R.color.green);
}