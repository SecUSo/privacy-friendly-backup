package org.secuso.privacyfriendlybackup

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.secuso.privacyfriendlybackup.api.IPFAService
import org.secuso.privacyfriendlybackup.api.common.PfaApi.ACTION_CONNECT
import org.secuso.privacyfriendlybackup.api.common.PfaError
import org.secuso.privacyfriendlybackup.backupapi.PfaApiConnection

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ConnectToPFATest {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("org.secuso.privacyfriendlybackup", appContext.packageName)
    }

    @Test
    fun bindToService() {
        var testEnd = false
        var connection : PfaApiConnection? = null
        var valid = false

        val listener = object : PfaApiConnection.IPfaApiListener {
            override fun onBound(service: IPFAService?) {
                println("Bound service successfully.")
                connection!!.send(ACTION_CONNECT)
            }
            override fun onError(error: PfaError) {
                println("Error: ${error.errorMessage}")
                testEnd = true
            }
            override fun onSuccess() {
                println("Command sent successfully.")
                connection!!.disconnect()
                valid = true
                testEnd = true
            }
            override fun onDisconnected() {
                println("Disconnected")
                testEnd = true
            }
        }

        connection = PfaApiConnection(appContext, "org.secuso.example", listener)
        connection.connect()

        try {
            do {
                Thread.sleep(1000)
            } while(!testEnd)
        } catch (e : InterruptedException) {
            if(connection.isBound()) {
                connection.disconnect()
            }
        }
        assertTrue(valid)
    }

    @Test
    fun bindToServiceUnknownCommand() {
        var testEnd = false
        var connection : PfaApiConnection? = null
        var valid = false

        val listener = object : PfaApiConnection.IPfaApiListener {
            override fun onBound(service: IPFAService?) {
                println("Bound service successfully.")
                connection!!.send("UNKNOWN")
            }
            override fun onError(error: PfaError) {
                println("Error: ${error.errorMessage}")
                assertEquals(error.code, PfaError.PfaErrorCode.ACTION_ERROR)
                valid = true
                testEnd = true
            }
            override fun onSuccess() {
                println("Command sent successfully.")
                connection!!.disconnect()
                testEnd = true
            }
            override fun onDisconnected() {
                println("Disconnected")
                testEnd = true
            }
        }

        connection = PfaApiConnection(appContext, "org.secuso.example", listener)
        connection.connect()

        try {
            do {
                Thread.sleep(1000)
            } while(!testEnd)
        } catch (e : InterruptedException) {
            if(connection.isBound()) {
                connection.disconnect()
            }
        }
        assertTrue(valid)
    }

    @Test
    fun bindToServiceAPIUnsupported() {
        var testEnd = false
        var connection : PfaApiConnection? = null
        var valid = false

        val listener = object : PfaApiConnection.IPfaApiListener {
            override fun onBound(service: IPFAService?) {
                println("Bound service successfully.")
                connection!!.send(ACTION_CONNECT)
            }
            override fun onError(error: PfaError) {
                println("Error: ${error.errorMessage}")
                assertEquals(error.code, PfaError.PfaErrorCode.API_VERSION_UNSUPPORTED)
                valid = true
                testEnd = true
            }
            override fun onSuccess() {
                println("Command sent successfully.")
                connection!!.disconnect()
                testEnd = true
            }
            override fun onDisconnected() {
                println("Disconnected")
                testEnd = true
            }
        }

        connection = PfaApiConnection(appContext, "org.secuso.example", listener, 0)
        connection.connect()

        try {
            do {
                Thread.sleep(1000)
            } while(!testEnd)
        } catch (e : InterruptedException) {
            if(connection.isBound()) {
                connection.disconnect()
            }
        }
        assertTrue(valid)
    }
}