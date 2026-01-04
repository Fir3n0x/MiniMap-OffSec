package com.example.minimap.model

import android.content.Context
import android.net.wifi.ScanResult
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WifiClassifierUnitTest {

    private fun newScanResult(
        ssid: String,
        capabilities: String,
        level: Int,
        freq: Int
    ): ScanResult = ScanResult().apply {
        this.SSID = ssid
        this.capabilities = capabilities
        this.level = level
        this.frequency = freq
    }

    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    private fun mockPublicDetector(value: Boolean) {
        mockkObject(PublicWifiDetector)
        every { PublicWifiDetector.isPublicWifi(any(), any()) } returns value
    }

    @Test
    fun `extractFeatures for open network 24GHz`() {

        mockPublicDetector(false)

        val scan = newScanResult(
            ssid = "CafeWiFi",
            capabilities = "[ESS]",
            level = -55,
            freq = 2412
        )

        val classifier = WifiClassifier(ctx)
        val feat = classifier.extractFeatures(scan, ctx)

        val expected = floatArrayOf(
            1f,  // is_open
            0f,  // uses_wep
            0f,  // uses_wpa
            0f,  // uses_wpa2_ccmp
            0f,  // uses_wpa3
            0f,  // wps_enabled
            0f,  // rssi_class  (-55 dBm  → High)
            0f,  // is_5ghz
            0f,  // is_hidden
            0f   // is_public
        )

        assertArrayEquals(expected, feat, /* tolerance */ 0.001f)
    }

    @Test
    fun `extractFeatures for WEP network 5GHz signal low`() {

        mockPublicDetector(true)

        val scan = newScanResult(
            ssid = "NetworkA1234",
            capabilities = "[WEP][WPS]",
            level = -85,
            freq = 5200
        )

        val classifier = WifiClassifier(ctx)
        val feat = classifier.extractFeatures(scan, ctx)

        val expected = floatArrayOf(
            0f,  // is_open
            1f,  // uses_wep
            0f,  // uses_wpa
            0f,  // uses_wpa2_ccmp
            0f,  // uses_wpa3
            1f,  // wps_enabled
            2f,  // rssi_class  (-85 dBm → Low)
            1f,  // is_5ghz
            1f,  // is_hidden
            1f   // is_public
        )

        assertArrayEquals(expected, feat, 0.001f)
    }
}
