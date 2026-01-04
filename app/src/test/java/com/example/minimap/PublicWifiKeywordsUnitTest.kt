package com.example.minimap.model

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Before

class PublicWifiUtilsUnitTest {

    @Before
    fun setUpLog() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
    }

    private fun mockContextWithAsset(
        fileName: String,
        content: String? = null,
        throwIo: Boolean = false
    ): Context {
        val ctx = mockk<Context>(relaxed = true)
        val assetMgr = mockk<AssetManager>()

        every { ctx.assets } returns assetMgr

        if (throwIo) {
            every { assetMgr.open(fileName) } throws IOException("File not found")
        } else {
            every { assetMgr.open(fileName) } returns
                    ByteArrayInputStream(content!!.toByteArray())
        }
        return ctx
    }

    @Test
    fun `loadKeywords returns list by default if file is not found`() {
        val ctx = mockContextWithAsset(
            fileName = "public_wifi_keywords.txt",
            throwIo = true // Mimic a not found file
        )

        val list = PublicWifiKeywords.loadKeywords(ctx)

        assertTrue("has to contain keyword 'free'", "free" in list)
        assertEquals(8, list.size)
    }

    @Test
    fun `isPublicWifi detects a SSID via simple pattern et regex`() {
        val patternsFile = """
            # commentaire
            simple:airportwifi
            regex:free[_\s-]?internet
        """.trimIndent()

        val ctx = mockContextWithAsset(
            fileName = "public_wifi_patterns.txt",
            content = patternsFile
        )

        assertTrue(
            "airportwifi-guest should be detected",
            PublicWifiDetector.isPublicWifi("AirportWiFi-Guest", ctx)
        )

        assertTrue(
            "FREE internet should be detected (regex)",
            PublicWifiDetector.isPublicWifi("my_FREE-internet_net", ctx)
        )

        assertFalse(
            "Private network must not be detected",
            PublicWifiDetector.isPublicWifi("MaisonDuVoisin", ctx)
        )
    }

    @After
    fun tearDownLog() = unmockkStatic(Log::class)
}
