package org.secuso.privacyfriendlybackup.ui.help

import android.content.Context
import org.secuso.privacyfriendlybackup.R

class HelpDataDump(val context: Context) {

    val dataGeneral: LinkedHashMap<String, List<String>>
        get() {
            val expandableListDetail = LinkedHashMap<String, List<String>>()

            val general: MutableList<String> = ArrayList()
            general.add(context.resources.getString(R.string.help_whatis_answer))
            expandableListDetail[context.resources.getString(R.string.help_whatis)] = general

            val encryption: MutableList<String> = ArrayList()
            encryption.add(context.resources.getString(R.string.help_encryption_answer1))
            encryption.add(context.resources.getString(R.string.help_encryption_answer2))
            expandableListDetail[context.resources.getString(R.string.help_encryption)] = encryption

//            val where: MutableList<String> = ArrayList()
//            where.add(context.resources.getString(R.string.help_where_from_answer))
//            expandableListDetail[context.resources.getString(R.string.help_where_from)] = where
//
//            val radius: MutableList<String> = ArrayList()
//            radius.add(context.resources.getString(R.string.help_radius_search_text))
//            expandableListDetail[context.resources.getString(R.string.help_radius_search_title)] = radius
//
//            val privacy: MutableList<String> = ArrayList()
//            privacy.add(context.resources.getString(R.string.help_privacy_answer))
//            expandableListDetail[context.resources.getString(R.string.help_privacy_heading)] = privacy
//
//            val permissions: MutableList<String> = ArrayList()
//            permissions.add(context.resources.getString(R.string.help_permission_internet_heading))
//            permissions.add(context.resources.getString(R.string.help_permission_internet_description))
//            expandableListDetail[context.resources.getString(R.string.help_permissions_heading)] = permissions

            return expandableListDetail
        }
}