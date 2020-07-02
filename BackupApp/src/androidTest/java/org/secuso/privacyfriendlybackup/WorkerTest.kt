package org.secuso.privacyfriendlybackup

import android.text.format.DateFormat
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.TestWorkerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.secuso.privacyfriendlybackup.api.util.readString
import org.secuso.privacyfriendlybackup.api.worker.CreateBackupWorker
import org.secuso.privacyfriendlybackup.data.internal.InternalBackupDataStoreHelper
import org.secuso.privacyfriendlybackup.data.room.BackupDatabase
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker
import org.secuso.privacyfriendlybackup.worker.datakeys.*
import java.util.*
import java.util.concurrent.Executors


@RunWith(AndroidJUnit4::class)
class WorkerTest {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val database = BackupDatabase.getInstance(appContext)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("org.secuso.privacyfriendlybackup", appContext.packageName)
    }

    @Test
    fun testWorker() {
        runWorker<CreateBackupWorker>()
    }

    @Test
    fun runEncryptionWorker() {
        val data = "testData".byteInputStream()
        var dataId : Long

        val packageName = "org.secuso.example"

        val date = Date()
        val dateString: CharSequence = DateFormat.format("yyyy_MM_dd", date.time)
        val fileName = "${packageName}_${dateString}.backup"

        runBlocking {
            dataId = InternalBackupDataStoreHelper.storeBackupData(appContext, 0, packageName, data)
        }

        val worker : EncryptionWorker = TestListenableWorkerBuilder<EncryptionWorker>(appContext).apply {
            setInputData(workDataOf(
                DATA_OPENPGP_PROVIDER to "org.sufficientlysecure.keychain",
                DATA_ID to dataId,
                DATA_ENCRYPT to true
            ))
        }.build()

        // run worker
        runBlocking {
            val result = worker.startWork().await()
        }

        // wait for user interaction
        runBlocking {
            delay(1000 * 30)
            val (istream, resultData) = InternalBackupDataStoreHelper.getInternalData(appContext, fileName)
            Log.d("PFABackup", "Data:"+istream?.readString())
            assertTrue(resultData.encrypted)
        }

    }

    inline fun <reified T : Worker> runWorker() {
        runBlocking {
            val executor = Executors.newSingleThreadExecutor()
            val worker = TestWorkerBuilder<T>(appContext, executor).build()

            val result = worker.startWork().await()

            assertEquals(Result.success(Data.EMPTY), result)
        }
    }

    inline fun <reified T : ListenableWorker> runCoroutineWorker(applySettings: (tlwb: TestListenableWorkerBuilder<T>) -> Unit) {
        val worker : T = TestListenableWorkerBuilder<T>(appContext).apply {
            applySettings(this)
        }.build()

        runBlocking {
            val result = worker.startWork().await()

            assertEquals(Result.success(Data.EMPTY), result)
        }
    }
}