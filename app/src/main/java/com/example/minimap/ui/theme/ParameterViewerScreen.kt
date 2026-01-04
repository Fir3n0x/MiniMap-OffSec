package com.example.minimap.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import com.example.minimap.autowide
import com.example.minimap.data.preferences.SettingsKeys
import com.example.minimap.data.preferences.SettingsRepository
import com.example.minimap.model.WorkerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


// File to handle parameter screen

// Enum for tabs
private enum class ParamTab(val label: String, val icon: @Composable () -> Unit) {
    Scan(
        label = "Scan",
        icon = { Icon(Icons.Default.Search, contentDescription = "Scan", tint = Color.Green) }
    ),
    Notification(
        label = "Notification",
        icon = { Icon(Icons.Default.Notifications, contentDescription = "Notification", tint = Color.Green) }
    ),
    About(
        label = "About",
        icon = { Icon(Icons.Default.Info, contentDescription = "About", tint = Color.Green) }
    )
}

// Data class which represents an option with title, description and state
private data class ParamOption(
    val key: String,
    val title: String,
    val description: String
)

@Composable
fun ParameterViewerScreen(navController: NavController) {
    // State for current checked tab
    var selectedTab by rememberSaveable { mutableStateOf(ParamTab.Scan) }

    // For each tab, we define an option list
    val scanOptions = listOf(
        ParamOption(
            key = "autoScan",
            title = "Auto Scan",
            description = "Automatically scans networks each 15 minutes when the application is closed"
        ),
        ParamOption(
            key = "saveResults",
            title = "Auto save Wifi",
            description = "Automatically saves the results of the WiFi scan locally on the device."
        ),
        ParamOption(
            key = "vibration",
            title = "Device Vibration",
            description = "Enable device vibration when discovering a new wifi"
        )
    )
    val notificationOptions = listOf(
        ParamOption(
            key = "pushNotifications",
            title = "Notifications Push",
            description = "Show a notification when a particular wifi is detected after \"Auto Scan\". Require \"Auto Scan\" to work."
        )
    )
    val aboutOptions = listOf(
        ParamOption(
            key = "showVersion",
            title = "Show current Version",
            description = "Shows the current version of the application on the main screen."
        )
    )


    // Instance SettingsRepository one time
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }

    // Read in Flow, persisting preferences for AutoScan and Notifications
    val autoScanEnabledState by settingsRepo.autoScanEnabledFlow.collectAsState(initial = false)
    val notificationEnabledState by settingsRepo.notificationEnabledFlow.collectAsState(initial = false)
    val vibrationEnabledState by settingsRepo.vibrationEnabledFlow.collectAsState(initial = false)
    val autoSaveEnabledState by settingsRepo.autoSaveEnabledFlow.collectAsState(initial = false)
    val showVersionEnabledState by settingsRepo.showVersionEnabledFlow.collectAsState(initial = false)



    // Local states for the remaining options which are not persistent (maps key → Boolean)
    // Persist only « saveResults », « silentMode », « showVersion », « enableLogs » in memory.
    val localOptionStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            // Initialize all non-generated keys from Datastore to false
            (scanOptions + notificationOptions + aboutOptions).forEach { opt ->
                if (opt.key != SettingsKeys.AUTO_SCAN_ENABLED.name &&
                    opt.key != SettingsKeys.NOTIFICATION_ENABLED.name &&
                    opt.key != SettingsKeys.VIBRATION_ENABLED.name &&
                    opt.key != SettingsKeys.AUTO_SAVE_ENABLED.name &&
                    opt.key != SettingsKeys.SHOW_VERSION_ENABLED.name
                ) {
                    this[opt.key] = false
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ────────────────────────────────────────────────────────
        // A) Left column : button « < » + vertical tabs
        // ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(Color(0xFF222222))
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back home button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("home") }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "<",
                    color = Color.Green,
                    fontSize = 24.sp,
                    fontFamily = autowide,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loop on tabs (ParamTab.values()), visual format and selection
            ParamTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(
                            if (isSelected) Color(0xFF333333) else Color.Transparent,
                            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                        )
                        .clickable { selectedTab = tab }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (isSelected) Color.Green else Color.Gray
                        ) {
                            tab.icon()
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = autowide,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // ────────────────────────────────────────────────────────
        // B) Right column : dynamic content according to selected tab
        // ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                ParamTab.Scan -> {
                    // ==== Tab “Scan” ====
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Scan parameters",
                            color = Color.Green,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 1) Auto Scan (DataStore + WorkManager)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = autoScanEnabledState,
                                onCheckedChange = { checked ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        settingsRepo.setAutoScanEnabled(checked)
                                        WorkerManager.scheduleWifiScan(context, checked)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = scanOptions[0].title,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = scanOptions[0].description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Divider(color = Color.DarkGray, thickness = 1.dp)


                        // 2) Auto save (local state)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = autoSaveEnabledState,
                                onCheckedChange = { checked ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        settingsRepo.setAutoSaveEnabled(checked)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = scanOptions[1].title,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = scanOptions[1].description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Divider(color = Color.DarkGray, thickness = 1.dp)


                        // 3) Device vibration (local state)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = vibrationEnabledState,
                                onCheckedChange = { checked ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        settingsRepo.setVibrationEnabled(checked)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = scanOptions[2].title, // "Device Vibration"
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = scanOptions[2].description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                ParamTab.Notification -> {
                    // ==== Tab “Notification” ====
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Notification parameters",
                            color = Color.Green,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 1) Notifications Push (DataStore)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = notificationEnabledState,
                                onCheckedChange = { checked ->
                                    // Update DataStore
                                    CoroutineScope(Dispatchers.IO).launch {
                                        settingsRepo.setNotificationEnabled(checked)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = notificationOptions[0].title,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = notificationOptions[0].description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                    }
                }

                ParamTab.About -> {
                    // ==== Tab “About” ====

                    val packageInfo = LocalContext.current.packageManager
                        .getPackageInfo(LocalContext.current.packageName, 0)

                    val versionName = packageInfo.versionName

                    Column(modifier = Modifier.fillMaxSize()) {

                        Text(
                            text = "About",
                            color = Color.Green,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "MiniMap, a passive wifi scan application where you can analyse wifi network security around you and save your findings in local.\n" +
                                    "You are currently using MiniMap v$versionName.",
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .fillMaxWidth()
                        )



                        Divider(color = Color.DarkGray, thickness = 1.dp)

                        // 1) Display version (local state)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = showVersionEnabledState,
                                onCheckedChange = { checked ->
                                    // Update DataStore
                                    CoroutineScope(Dispatchers.IO).launch {
                                        settingsRepo.setShowVersionEnabled(checked)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = aboutOptions[0].title,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = aboutOptions[0].description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}