package com.example.minimap

import androidx.activity.ComponentActivity
import androidx.navigation.compose.rememberNavController
import com.example.minimap.model.WifiNetworkInfo
import com.example.minimap.ui.theme.WifiRadarDetection
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.minimap.model.WifiSecurityLevel


class WifiRadarDetectionInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun fakeWifiNetwork(ssid: String,
                                rssi: Int = -50,
                                bssid: String = ssid,
                                label: WifiSecurityLevel = WifiSecurityLevel.SAFE,
                                frequencies: Int = 2400,
                                capabilities: String = "WEP",
                                timestamp: Long = 3208285092854): WifiNetworkInfo {
        return WifiNetworkInfo(ssid = ssid, bssid = bssid, rssi = rssi, label = label, frequency = frequencies, capabilities = capabilities, timestamp = timestamp)
    }


    @Test
    fun title_is_displayed() {
        composeTestRule.setContent {
            WifiRadarDetection(
                navController = rememberNavController(),
                networks = emptyList(),
                newNetworks = emptyList(),
                isRunning = false,
                onToggleRunning = {}
            )
        }

        composeTestRule
            .onNodeWithText("WiFi Detection")
            .assertIsDisplayed()
    }


    @Test
    fun export_button_opens_dialog() {
        composeTestRule.setContent {
            WifiRadarDetection(
                navController = rememberNavController(),
                networks = listOf(fakeWifiNetwork("TestWifi")),
                newNetworks = emptyList(),
                isRunning = false,
                onToggleRunning = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Export")
            .performClick()

        composeTestRule
            .onNodeWithText("Export networks")
            .assertIsDisplayed()
    }

}