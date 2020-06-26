package org.secuso.privacyfriendlybackup

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.Worker
import androidx.work.await
import androidx.work.testing.TestWorkerBuilder
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.secuso.privacyfriendlybackup.api.worker.CreateBackupWorker
import java.util.concurrent.Executors


@RunWith(AndroidJUnit4::class)
class WorkerTest {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun useAppContext() {
        // Context of the app under test.
        assertEquals("org.secuso.privacyfriendlybackup", appContext.packageName)
    }

    @Test
    fun testWorker() {
        runWorker<CreateBackupWorker>()
    }

    inline fun <reified T : Worker> runWorker() {
        runBlocking {
            val executor = Executors.newSingleThreadExecutor()
            val worker = TestWorkerBuilder<T>(appContext, executor).build()

            val result = worker.startWork().await()

            assertEquals(Result.success(Data.EMPTY), result)
        }
    }
}