package com.example.minimap


import com.example.minimap.model.WifiNetworkInfo
import junit.framework.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class WifiScanScreenUnitTest {

    @Test
    fun testRssiComparisonKeepsStrongerSignal() {
        val old = WifiNetworkInfo("Freebox", "11:22", -80, 2437, "", 0)
        val new = WifiNetworkInfo("Freebox", "11:22", -50, 2437, "", 0)

        val diff = kotlin.math.abs(old.rssi - new.rssi)
        assertTrue(diff > 5)
        assertTrue(new.rssi > old.rssi)
    }

    @Test
    fun testTimestampFormattedCorrectly() {
        val timestamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formatted = sdf.format(Date(timestamp))

        assertTrue(formatted.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")))
    }


    @Test
    fun testFilterNewNetworks() {
        val knownNetworks = listOf(
            WifiNetworkInfo("Home", "AA:BB", -40, 2412, "", 0),
            WifiNetworkInfo("Office", "CC:DD", -60, 2437, "", 0)
        )
        val knownKeys = knownNetworks.map { "${it.ssid}:${it.bssid}" }.toSet()

        val scanned = listOf(
            WifiNetworkInfo("Home", "AA:BB", -40, 2412, "", 0),
            WifiNetworkInfo("Cafe", "EE:FF", -50, 2412, "", 0)
        )

        val new = scanned.filter { !knownKeys.contains("${it.ssid}:${it.bssid}") }

        assertEquals(1, new.size)
        assertEquals("Cafe", new.first().ssid)
    }


    @Test
    fun testIgnoreEmptySsid() {
        val scannedNetworks = listOf(
            WifiNetworkInfo("", "00:00", -70, 2412, "", 0),
            WifiNetworkInfo("ValidSSID", "11:11", -60, 2412, "", 0)
        )

        val filtered = scannedNetworks.filter { it.ssid.isNotBlank() }
        assertEquals(1, filtered.size)
        assertEquals("ValidSSID", filtered[0].ssid)
    }


    @Test
    fun testUniqueNetworksBySsid() {
        val networks = listOf(
            WifiNetworkInfo("Wifi1", "AA:BB", -45, 2412, "", 0),
            WifiNetworkInfo("Wifi1", "AA:BB", -48, 2412, "", 0), // Slightly weaker
            WifiNetworkInfo("Wifi2", "CC:DD", -30, 2437, "", 0)
        )

        val uniqueMap = mutableMapOf<String, WifiNetworkInfo>()
        for (network in networks) {
            val key = "${network.ssid}:${network.bssid}"
            val existing = uniqueMap[key]
            if (existing == null || network.rssi > existing.rssi) {
                uniqueMap[key] = network
            }
        }

        assertEquals(2, uniqueMap.size)
        assertEquals(-45, uniqueMap["Wifi1:AA:BB"]?.rssi)
    }





}