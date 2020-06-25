package org.secuso.privacyfriendlybackup

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.secuso.privacyfriendlybackup.api.util.AuthenticationHelper


@RunWith(AndroidJUnit4::class)
class SignatureTest {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val uid = getUidForPackageName(appContext.packageName)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("org.secuso.privacyfriendlybackup", appContext.packageName)
    }

    @Test
    fun testSignature() {
        assertTrue(AuthenticationHelper.authenticate(appContext, uid))
    }

    @Test
    fun readSignature() {
        // 229105C04C79D58FD566CCB4B9E031CF325BDDA1 // Debug Key
        AuthenticationHelper.authenticate(appContext, uid)
        // 229105C04C79D58FD566CCB4B9E031CF325BDDA1
        AuthenticationHelper.authenticate(appContext, getUidForPackageName("org.secuso.example"))
    }

    fun getUidForPackageName(packageName : String) : Int {
        return appContext.packageManager.getPackageUid(packageName,0)
    }
}