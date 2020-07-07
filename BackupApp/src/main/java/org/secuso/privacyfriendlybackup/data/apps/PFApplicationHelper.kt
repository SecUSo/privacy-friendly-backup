package org.secuso.privacyfriendlybackup.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.secuso.privacyfriendlybackup.api.common.PfaApi

object PFApplicationHelper {

    private val mCache = HashMap<String, PFAInfo>()

    data class PFAInfo(
        val label: String,
        val packageName: String,
        val icon: Drawable,
        val iconResource: Int,
        val installDate: Long
    )

    fun getAvailablePFAs(context: Context) : List<PFAInfo> {
        val pm: PackageManager = context.packageManager
        val intent = Intent(PfaApi.PFA_CONNECT_ACTION)
        val resolveInfo = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val results = mutableListOf<PFAInfo>()

        for (ri in resolveInfo) {
            var installDate: Long = 0
            try {
                installDate = pm.getPackageInfo(ri.activityInfo.packageName, 0).firstInstallTime
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            results.add(PFAInfo(
                ri.activityInfo.applicationInfo.loadLabel(pm).toString(),
                ri.activityInfo.packageName,
                ri.activityInfo.applicationInfo.loadIcon(pm),
                ri.activityInfo.iconResource,
                installDate
            ))
        }

        for(result in results) {
            if(!mCache.containsKey(result.packageName)) {
                mCache[result.packageName] = result
            }
        }

        return results
    }

    fun getCachedDataForPackageName(packageName: String) : PFAInfo? =
        if(mCache.containsKey(packageName)) mCache[packageName] else null

    fun getDataForPackageName(context: Context, packageName : String) : PFAInfo? {
        if(mCache.containsKey(packageName) && mCache[packageName] != null) {
            return mCache[packageName]
        }

        var result : PFAInfo? = null
        val pm: PackageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setPackage(packageName)
        }
        val resolveInfo = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (ri in resolveInfo) {
            var installDate: Long = 0
            try {
                installDate = pm.getPackageInfo(ri.activityInfo.packageName, 0).firstInstallTime
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            result = PFAInfo(
                ri.activityInfo.applicationInfo.loadLabel(pm).toString(),
                ri.activityInfo.packageName,
                ri.activityInfo.applicationInfo.loadIcon(pm),
                ri.activityInfo.iconResource,
                installDate
            )
        }
        if(result != null) {
            mCache[packageName] = result
        }

        return result
    }

}