package org.secuso.privacyfriendlybackup

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.secuso.privacyfriendlybackup.api.util.AuthenticationHelper
import org.secuso.privacyfriendlybackup.data.cloud.drive.DriveRepository


@RunWith(AndroidJUnit4::class)
class DriveTest {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("org.secuso.privacyfriendlybackup", appContext.packageName)
    }

    @Test
    fun driveTest() {
        //val gam = GoogleAccountManager(appContext)
        //println(gam)
    }

    fun getUidForPackageName(packageName : String) : Int {
        return appContext.packageManager.getPackageUid(packageName,0)
    }
}