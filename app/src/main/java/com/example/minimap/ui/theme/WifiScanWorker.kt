package com.example.minimap.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.minimap.R
import com.example.minimap.data.preferences.SettingsRepository
import com.example.minimap.model.WifiClassifier
import com.example.minimap.model.WifiNetworkInfo
import com.example.minimap.model.WifiSecurityLevel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs


// File to handle notification process related to periodic scan when the applicaiton is closed



class WifiScanWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val settingsRepo: SettingsRepository = SettingsRepository(context)
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "wifi_alerts_channel"
        private const val FOREGROUND_CHANNEL_ID = "wifi_scan_foreground_channel"
        private const val NOTIF_ID = 1001
        private const val FOREGROUND_NOTIF_ID = 1002
    }


    @SuppressLint("MissingPermission", "ServiceCast")
    override suspend fun doWork(): Result {
        Log.d("WifiScanWorker", "Worker started")

        //sendTestNotification("Worker started")


        if (!hasRequiredPermissions(context)) {
            Log.e("WifiScanWorker", "Missing permissions")
            return Result.failure()
        }

        return try {
            // Start foreground service if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setForeground(createForegroundInfo("Starting scan..."))
            }
            Log.d("debug","1")

            // Get settings
            val settingsRepo = SettingsRepository(context)
            val autoScanEnabled = settingsRepo.autoScanEnabledFlow.first()
            if (!autoScanEnabled) return Result.success()
            Log.d("debug","2")

            // Handle WiFi
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifiManager.isWifiEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // On Android 10+, we can't enable WiFi programmatically
                    Log.w("WifiScanWorker", "Can't enable WiFi on Android 10+")
                    return Result.retry()
                } else {
                    wifiManager.isWifiEnabled = true
                    delay(3000) // Wait longer for WiFi to enable
                }
            }
            Log.d("debug","3")

            // Perform scan
            updateForegroundNotification("Scanning...")
            val success = wifiManager.startScan()
            if (!success) {
                Log.w("WifiScanWorker", "Scan failed to start")
                return Result.retry()
            }
            Log.d("debug","4")

            // Wait for results
            delay(1000)
            val scanResults = wifiManager.scanResults ?: run {
                Log.w("WifiScanWorker", "No scan results")
                return Result.retry()
            }
            Log.d("debug","5")

            // Process results
            val networks = processScanResults(scanResults)
            Log.d("WifiScanWorker", "Found ${networks.size} networks")

            // Save if enabled
            if (settingsRepo.autoSaveEnabledFlow.first()) {
                saveNewNetworks(networks)
            }
            Log.d("debug","6")

            // Notify if needed
            if (settingsRepo.notificationEnabledFlow.first() && networks.any { isNetworkInsecure(it) }) {
                sendAlertNotification(networks.filter { isNetworkInsecure(it) })
            }
            Log.d("debug","7")

            Result.success()
        } catch (e: Exception) {
            Log.e("WifiScanWorker", "Error in worker", e)
            Result.retry()
        }
    }



    private fun sendTestNotification(message: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Test Worker")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e("Notification", "Error test notification", e)
        }
    }



    fun hasRequiredPermissions(context: Context): Boolean {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }.also { hasPermissions ->
            if (!hasPermissions) {
                Log.e("Permissions", "Permissions manquantes: $permissions")
            }
        }
    }




    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Alert channel
            val alertChannel = NotificationChannel(
                CHANNEL_ID,
                "WiFi Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for insecure WiFi networks"
            }

            // Foreground service channel
            val foregroundChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "WiFi Scan Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background WiFi scanning service"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(foregroundChannel)
        }
    }

    private fun createForegroundInfo(progressText: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setContentTitle("WiFi Security Scan")
            .setContentText(progressText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return ForegroundInfo(FOREGROUND_NOTIF_ID, notification)
    }

    private suspend fun updateForegroundNotification(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setForeground(createForegroundInfo(text))
        }
    }



    @SuppressLint("MissingPermission")
    private suspend fun processScanResults(results: List<ScanResult>): List<WifiNetworkInfo> {
        val wifiClassifier = WifiClassifier(context)

        // Location function by default
        fun getDefaultLocation(): Location = Location("").apply {
            latitude = 0.0
            longitude = 0.0
        }

        // Retrieve location
        val location = try {
            if (!hasLocationPermission()) {
                Log.w("Location", "Permissions manquantes")
                return emptyList()
            }

            val locationClient = LocationServices.getFusedLocationProviderClient(context)

            withTimeout(5000) {
                suspendCancellableCoroutine<Location> { continuation ->
                    val task = locationClient.lastLocation

                    task.addOnSuccessListener { loc ->
                        if (continuation.isActive) {
                            continuation.resume(loc ?: getDefaultLocation())
                        }
                    }

                    task.addOnFailureListener { e ->
                        Log.e("Location", "Erreur", e)
                        if (continuation.isActive) {
                            continuation.resume(getDefaultLocation())
                        }
                    }

                    // No need removeOnSuccessListener : Task object handle his own internal listeners
                    continuation.invokeOnCancellation {
                        // Nothing to do here
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Location", "Erreur globale", e)
            getDefaultLocation()
        }


        val uniqueNetworks = mutableMapOf<String, WifiNetworkInfo>()
        val wifiNetworks = mutableStateListOf<WifiNetworkInfo>()

        for(result in results){

            //Retrieve features
            val features = wifiClassifier.extractFeatures(result, context)

            //Predict security level with model
            val securityLevel = wifiClassifier.predictSecurityLevel(features)


            val ssid = result.SSID
            val rssi = result.level
            val bssid = result.BSSID
            val capabilities = result.capabilities
            val channel = result.channelWidth
            val frequency = result.frequency
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
                    capabilities = capabilities,
                    timestamp = timestamp,
                    label = securityLevel,
                    timestampFormatted = formatted,
                    latitude = location.latitude,
                    longitude = location.longitude
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
                            capabilities = capabilities,
                            timestamp = timestamp,
                            label = securityLevel,
                            timestampFormatted = formatted,
                            latitude = location.latitude,
                            longitude = location.longitude
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


        wifiNetworks.clear()
        wifiNetworks.addAll(uniqueNetworks.values)

        return trulyNewNetworks
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    cont.resume(location)
                }
                .addOnFailureListener { exception ->
                    cont.resume(null)
                }
        } catch (e: Exception) {
            cont.resume(null)
        }
    }



    // Verification specific location permission
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNetworkInsecure(network: WifiNetworkInfo): Boolean {
        return network.label.toString() == WifiSecurityLevel.DANGEROUS.toString()
    }

    private fun saveNewNetworks(networks: List<WifiNetworkInfo>) {
        val knownNetworks = readWifiNetworksFromCsv(context, "wifis_dataset.csv")
        val knownKeys = knownNetworks.map { "${it.ssid}:${it.bssid}" }.toSet()

        val newNetworks = networks.filterNot {
            knownKeys.contains("${it.ssid}:${it.bssid}")
        }

        if (newNetworks.isNotEmpty()) {
            appendNewWifisToCsv(context, "wifis_dataset.csv", newNetworks)
        }
    }

    fun sendAlertNotification(networks: List<WifiNetworkInfo>) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            // Create channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                createNotificationChannels()
            }

            val displayCount = 3
            val shownSsids = networks.take(displayCount).joinToString("\n") { it.ssid }
            val extraCount = networks.size - displayCount

            val detailText = if (extraCount > 0)
                "$shownSsids\n...and $extraCount others"
            else
                shownSsids


            // Notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_wifi)
                .setContentTitle("WiFi Insecure Detected - ${networks.size} network(s)")
                //.setContentText("${networks.size} insecure networks detected.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(detailText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIF_ID, notification)
            Log.d("Notification", "Notification warning send")

        } catch (e: Exception) {
            Log.e("Notification", "Error send", e)
        }
    }
}