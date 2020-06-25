package org.secuso.privacyfriendlybackup

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.secuso.privacyfriendlybackup.api.IPFAService
import org.secuso.privacyfriendlybackup.api.pfa.PfaApi
import org.secuso.privacyfriendlybackup.api.pfa.PfaError
import org.secuso.privacyfriendlybackup.backupapi.PfaApiConnection

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

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

        val listener = object : PfaApiConnection.IPfaApiListener {
            override fun onBound(service: IPFAService?) {
                println("Bound service successfully.")
                connection!!.send(PfaApi.ACTION_CONNECT)
            }
            override fun onError(error: PfaError) {
                println("Error: ${error.errorMessage}")
                assertFalse("Error: ${error.errorMessage}", true)
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
    }

    @Test
    fun bindToServiceSendError() {
        var testEnd = false
        var connection : PfaApiConnection? = null

        val listener = object : PfaApiConnection.IPfaApiListener {
            override fun onBound(service: IPFAService?) {
                println("Bound service successfully.")
                connection!!.send("UNKNOWN")
            }
            override fun onError(error: PfaError) {
                println("Error: ${error.errorMessage}")
                assert(true)
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
    }
}