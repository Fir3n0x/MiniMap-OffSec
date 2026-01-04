package com.example.minimap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.minimap.model.WifiNetworkInfo
import com.example.minimap.model.WifiSecurityLevel
import com.example.minimap.ui.theme.exportNetworksToJson
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportDataInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val sampleNetworks = listOf(
        WifiNetworkInfo("SSID1", "BSSID1", -50, 2400, "WPA2", 123456789, WifiSecurityLevel.SAFE),
        WifiNetworkInfo("SSID2", "BSSID2", -60, 5200, "WEP", 987654321, WifiSecurityLevel.DANGEROUS)
    )

    @Test
    fun export_creates_file_with_valid_json() {
        val fileName = "test_export.json"
        val file = File(context.filesDir, fileName)
        if (file.exists()) file.delete()

        exportNetworksToJson(context, sampleNetworks, fileName)

        Assert.assertTrue("File should exist", file.exists())

        val content = file.readText()
        val decoded = Json.Default.decodeFromString<List<WifiNetworkInfo>>(content)
        Assert.assertEquals(sampleNetworks, decoded)

        // Cleanup
        file.delete()
    }
}