package com.example.minimap.ui.theme


import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.minimap.data.preferences.SettingsRepository
import com.example.minimap.model.WifiClassifier
import com.example.minimap.model.WifiNetworkInfo
import com.example.minimap.model.WifiScannerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs


// File to handle logic implementation of Radar Detection Screen


// Retrieve network's frequency
fun frequencyToChannel(freq: Int): Int {
    return when {
        freq in 2412..2484 -> (freq - 2407) / 5
        freq in 5170..5895 -> (freq - 5000) / 5
        freq in 5955..7115 -> (freq - 5950) / 5 // 6 GHz
        else -> -1 // unknown
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission", "ServiceCast")
@Composable
fun WifiScanScreen(context: Context, navController: NavController) {


    val viewModel: WifiScannerViewModel = viewModel()
    val context = LocalContext.current

    // Repository variable for options
    val settingsRepo = remember { SettingsRepository(context) }
    val autoSaveEnabled by settingsRepo.autoSaveEnabledFlow.collectAsState(initial = false)

    // Initialize client location at launching
    LaunchedEffect(Unit) {
        viewModel.initLocationClient(context)
    }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var wakeLock: PowerManager.WakeLock? by remember { mutableStateOf(null) }

    // Acquire wake lock when screen is displayed
    LaunchedEffect(Unit) {
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "Minimap:WifiScanWakeLock"
        )
        wakeLock?.acquire()
        //wakeLock?.acquire(10*60*1000L /*10 minutes*/)
    }

    // Release wake lock when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        }
    }


    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val wifiNetworks = remember { mutableStateListOf<WifiNetworkInfo>() }


    var isRunning by remember { mutableStateOf(true) }


    var currentLatitude by remember { mutableStateOf(0.0) }
    var currentLongitude by remember { mutableStateOf(0.0) }


    // Initialize classifier
    val wifiClassifier = remember { WifiClassifier(context) }

    // Variable to handle new observed wifi network
    var newDiscoveredNetworks by remember { mutableStateOf<List<WifiNetworkInfo>>(emptyList()) }


    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!locationPermissionState.permission.isEmpty()) {
            locationPermissionState.launchPermissionRequest()
        }
    }


    LaunchedEffect(isRunning) {
        while (isRunning) {

            // Get current location
            viewModel.fetchLastLocation { location ->
                currentLatitude = location.latitude
                currentLongitude = location.longitude
            }

            wifiManager.startScan()
            delay(1000)
            val results = wifiManager.scanResults





            val uniqueNetworks = mutableMapOf<String, WifiNetworkInfo>()

            for(result in results){

                //Retrieve features
                val features = wifiClassifier.extractFeatures(result, context)

                //Predict security level with model
                val securityLevel = wifiClassifier.predictSecurityLevel(features)


                val ssid = result.SSID
                val rssi = result.level
                val bssid = result.BSSID
                val capabilities = result.capabilities
                val frequency = result.frequency
                val channel = frequencyToChannel(result.frequency)
                val centerFreq0 = result.centerFreq0
                val centerFreq1 = result.centerFreq1
                val timestamp = result.timestamp
                val operatorFriendlyName = result.operatorFriendlyName
                val venueName = result.venueName
                val isPasspointNetwork = result.isPasspointNetwork
                val is80211mcResponder = result.is80211mcResponder

                val timestampMillis = System.currentTimeMillis()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formatted = sdf.format(Date(timestampMillis))

                if (ssid.isBlank()) continue // ignore empty ssid

                val existing = uniqueNetworks[ssid]
                if (existing == null) {
                    // no include -> add up
                    uniqueNetworks[ssid] = WifiNetworkInfo(
                        ssid = ssid,
                        bssid = bssid,
                        rssi = rssi,
                        frequency = frequency,
                        channel = channel,
                        capabilities = capabilities,
                        timestamp = timestamp,
                        label = securityLevel,
                        timestampFormatted = formatted,
                        latitude = currentLatitude,
                        longitude = currentLongitude
                    )
                } else {
                    // already available → compare rssi
                    val diff = abs(existing.rssi - rssi)
                    if (diff > 5) {
                        // Significant difference → keep the highest (close to 0)
                        if (rssi > existing.rssi) {
                            uniqueNetworks[ssid] = WifiNetworkInfo(
                                ssid = ssid,
                                bssid = bssid,
                                rssi = rssi,
                                frequency = frequency,
                                channel = channel,
                                capabilities = capabilities,
                                timestamp = timestamp,
                                label = securityLevel,
                                timestampFormatted = formatted,
                                latitude = currentLatitude,
                                longitude = currentLongitude
                            )
                        }
                    }
                    // else, ignored bc redundancies
                }
            }


            // List known wifi from .csv
            val knownNetworks = readWifiNetworksFromCsv(context, "wifis_dataset.csv")
            val knownKeys = knownNetworks.map { "${it.ssid}:${it.bssid}" }.toSet()


            // Filter new wifi
            val currentNetworks = uniqueNetworks.values.toList()
            val trulyNewNetworks = currentNetworks.filter {
                !knownKeys.contains("${it.ssid}:${it.bssid}")
            }

            // Update new wifi
            if (trulyNewNetworks.isNotEmpty()) {
                newDiscoveredNetworks = trulyNewNetworks
            }

            if(autoSaveEnabled){
                appendNewWifisToCsv(context, "wifis_dataset.csv", uniqueNetworks.values.toList())
            }


            wifiNetworks.clear()
            wifiNetworks.addAll(uniqueNetworks.values)

            delay(2000)

        }
    }

    WifiRadarDetection(navController = navController, networks = wifiNetworks.toList(), newNetworks = newDiscoveredNetworks, isRunning = isRunning, onToggleRunning = {isRunning = !isRunning}, modifier = Modifier.fillMaxSize())
}



@Composable
@Preview(showBackground = true, backgroundColor = 0xFF000000)
fun WifiScanPreview() {

    var newDiscoveredNetworks: List<WifiNetworkInfo> = emptyList()

    val mockNetworks = listOf(
        WifiNetworkInfo("INSA_WIFI", "9", 9, 4, "4", 4),
    )

    var isRunning: Boolean = true

    WifiRadarDetection(navController = rememberNavController(), networks = mockNetworks, newNetworks = newDiscoveredNetworks, isRunning = isRunning, onToggleRunning = {isRunning = !isRunning}, modifier = Modifier.fillMaxSize())
}