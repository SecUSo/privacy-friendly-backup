package org.secuso.privacyfriendlybackup.ui.common

import androidx.annotation.ColorRes
import org.secuso.privacyfriendlybackup.R

enum class Mode(var value: Int, @ColorRes var color: Int) {
    NORMAL(0, R.color.colorPrimary),
    SEARCH(1, R.color.colorAccent),
    DELETE(2, R.color.middlegrey),
    SEARCH_AND_DELETE(3, R.color.middleblue);

    fun isActiveIn(currentMode: Mode): Boolean {
        return currentMode.value and value == value
    }

    companion object {
        operator fun get(i: Int): Mode {
            for (mode in Mode.values()) {
                if (mode.value == i) return mode
            }
            return NORMAL
        }
    }
}