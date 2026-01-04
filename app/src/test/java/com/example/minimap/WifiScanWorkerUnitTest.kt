package com.example.minimap

import android.content.Context
import com.example.minimap.ui.theme.WifiScanWorker
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.every
import org.junit.Assert.assertEquals
import androidx.work.ListenableWorker.Result

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class WifiScanWorkerUnitTest {

    @Test
    fun testDoWork_withoutPermissions_returnsFailure() = runTest {
        val context = mockk<Context>(relaxed = true)
        val worker = spyk(WifiScanWorker(context, mockk(relaxed = true)))
        every { worker.hasRequiredPermissions(context) } returns false

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
    }
}
