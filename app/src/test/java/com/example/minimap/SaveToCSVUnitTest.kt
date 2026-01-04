package com.example.minimap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.minimap.model.WifiSecurityLevel
import com.example.minimap.model.WifiNetworkInfo
import com.example.minimap.ui.theme.appendNewWifisToCsv
import com.example.minimap.ui.theme.readKnownWifiKeys
import com.example.minimap.ui.theme.readWifiNetworksFromCsv
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.intArrayOf

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class SaveToCSVUnitTest {

    private lateinit var context: Context
    private val fileName = "test_wifi.csv"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, fileName).delete() // reset before each test
    }

    @Test
    fun testReadKnownWifiKeys_emptyFile() {
        val keys = readKnownWifiKeys(context, fileName)
        assertTrue(keys.isEmpty())
    }

    @Test
    fun testAppendAndReadKnownWifiKeys() {
        val network = WifiNetworkInfo(
            ssid = "TestSSID",
            bssid = "00:11:22:33:44:55",
            rssi = -42,
            frequency = 2412,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            timestamp = System.currentTimeMillis(),
            label = WifiSecurityLevel.SAFE,
            latitude = 48.12,
            longitude = -1.68
        )

        appendNewWifisToCsv(context, fileName, listOf(network))
        val keys = readKnownWifiKeys(context, fileName)

        assertEquals(1, keys.size)
        assertTrue(keys.contains("TestSSID:00:11:22:33:44:55"))
    }

    @Test
    fun testReadWifiNetworksFromCsv() {
        val network = WifiNetworkInfo(
            ssid = "MyNetwork",
            bssid = "AA:BB:CC:DD:EE:FF",
            rssi = -50,
            frequency = 5200,
            capabilities = "[WPA3-SAE][ESS]",
            timestamp = System.currentTimeMillis(),
            label = WifiSecurityLevel.SAFE,
            latitude = 40.7128,
            longitude = -74.0060
        )

        appendNewWifisToCsv(context, fileName, listOf(network))
        val list = readWifiNetworksFromCsv(context, fileName)

        assertEquals(1, list.size)
        val readNet = list[0]
        assertEquals("MyNetwork", readNet.ssid)
        assertEquals("AA:BB:CC:DD:EE:FF", readNet.bssid)
        assertEquals(-50, readNet.rssi)
        assertEquals(5200, readNet.frequency)
        assertEquals("[WPA3-SAE][ESS]", readNet.capabilities)
        assertEquals(WifiSecurityLevel.SAFE, readNet.label)
        assertEquals(40.7128, readNet.latitude, 0.0001)
        assertEquals(-74.0060, readNet.longitude, 0.0001)
    }

    @Test
    fun testAppendDoesNotDuplicate() {
        val network = WifiNetworkInfo(
            ssid = "DUPLICATE",
            bssid = "DE:AD:BE:EF:00:00",
            rssi = -60,
            frequency = 2437,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            timestamp = System.currentTimeMillis(),
            label = WifiSecurityLevel.SAFE,
            latitude = 0.0,
            longitude = 0.0
        )

        appendNewWifisToCsv(context, fileName, listOf(network))
        appendNewWifisToCsv(context, fileName, listOf(network)) // Try again

        val list = readWifiNetworksFromCsv(context, fileName)
        assertEquals(1, list.size) // Should not be duplicated
    }
}