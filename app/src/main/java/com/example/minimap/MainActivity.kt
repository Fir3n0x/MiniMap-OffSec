 package com.example.minimap

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.testing.TestNavHostController
import com.example.minimap.data.preferences.SettingsRepository
import com.example.minimap.model.Screen
import com.example.minimap.model.WorkerManager
import com.example.minimap.model.logWorkerStatus
import com.example.minimap.ui.theme.FileViewerScreen
import com.example.minimap.ui.theme.HomeScreen
import com.example.minimap.ui.theme.ParameterViewerScreen
import com.example.minimap.ui.theme.WifiScanScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


// File to handle the main Activity of the application and launching the correct screen at launching


val autowide = FontFamily(
    Font(R.font.audiowide_regular, FontWeight.Normal)
)


class MainActivity : ComponentActivity() {

    companion object {
        private const val CHANNEL_ID = "wifi_alerts_channel"
        private const val FOREGROUND_CHANNEL_ID = "wifi_scan_foreground_channel"
        private const val NOTIF_ID = 1001
        private const val FOREGROUND_NOTIF_ID = 1002
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            schedulePeriodicWifiScan(this@MainActivity)
        } else {
            Toast.makeText(
                this,
                "Permission are required to scan Wi-Fi and to get notifications when the application is closed.",
                Toast.LENGTH_LONG
            ).show()
        }
    }



    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannels()

        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()

            LaunchedEffect(Unit) {
                if (checkPermissions(context)) {
                    schedulePeriodicWifiScan(context)
                    Log.d("debug","permission checked")
                } else {
                    requestPermissions(context)
                }
            }

            logWorkerStatus(context)

            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route){
                    HomeScreen(navController)
                }

                composable(Screen.WifiScan.route){
                    val context = LocalContext.current
                    WifiScanScreen(context, navController)
                }
                composable(Screen.FileViewer.route){
                    FileViewerScreen(navController = navController)
                }

                composable(Screen.ParameterViewer.route){
                    ParameterViewerScreen(navController = navController)
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Canal pour les alertes
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "WiFi Security Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for insecure WiFi networks"
                }
            )

            // Canal pour le service foreground
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "WiFi Scan Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background WiFi scanning service"
                }
            )
        }
    }

    private fun checkPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions(context: Context) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        requestPermissionLauncher.launch(permissions)
    }
}


fun schedulePeriodicWifiScan(context: Context) {
    val settingsRepo = SettingsRepository(context)
    CoroutineScope(Dispatchers.IO).launch {
        val enabled = settingsRepo.autoScanEnabledFlow.first()
        WorkerManager.scheduleWifiScan(context, enabled)
    }
}



@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MiniMapHomeScreenPreview() {
    val context = LocalContext.current
    val navController = TestNavHostController(context)
    HomeScreen(navController = navController)
}