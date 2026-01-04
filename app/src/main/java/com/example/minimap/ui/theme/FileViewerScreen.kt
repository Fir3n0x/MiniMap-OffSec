package com.example.minimap.ui.theme


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.minimap.autowide
import com.example.minimap.model.WifiNetworkInfo
import com.example.minimap.model.WifiSecurityLevel
import com.example.minimap.model.getColor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// File to handle saving file as .json and .csv and display their content in the application


@Composable
fun FileViewerScreen(navController: NavController) {
    val context = LocalContext.current
    val tabs = listOf("Saved Wifi", "Observed Wifi")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var jsonContent by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showDeleteAllJsonDialog by remember { mutableStateOf(false) }

    var showWifiDeleteDialog by remember { mutableStateOf(false) }
    var wifiToDelete by remember { mutableStateOf<WifiNetworkInfo?>(null) }
    var showDeleteAllWifiDialog by remember { mutableStateOf(false) }

    var showSaveCsvDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }

    // Filters in  "Observed Wifi"
    val securityFilters = listOf("SAFE", "MEDIUM", "DANGEROUS")
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    var jsonFiles by remember {
        mutableStateOf(
            context.filesDir?.listFiles { file ->
                file.extension == "json"
            }?.toList() ?: emptyList()
        )
    }


    var wifiNetworks by remember {
        mutableStateOf(readWifiNetworksFromCsv(context, "wifis_dataset.csv").toMutableList())
    }



    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Text(
                    text = "Close",
                    modifier = Modifier
                        .clickable { showDialog = false }
                        .padding(8.dp),
                    color = Color.Green
                )
            },
            title = {
                Text("WIFI Info", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        item {
                            Text(
                                text = jsonContent,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            containerColor = Color.DarkGray
        )
    }


    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                Text(
                    text = "Yes",
                    modifier = Modifier
                        .clickable {
                            fileToDelete?.delete()
                            jsonFiles = context.filesDir?.listFiles { file -> file.extension == "json" }?.toList()
                                ?: emptyList()
                            showDeleteDialog = false
                        }
                        .padding(8.dp),
                    color = Color.Red
                )
            },
            dismissButton = {
                Text(
                    text = "No",
                    modifier = Modifier
                        .clickable { showDeleteDialog = false }
                        .padding(8.dp),
                    color = Color.Green
                )
            },
            title = {
                Text("Confirm Deletion", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this file?",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            containerColor = Color.DarkGray
        )
    }


    if (showDeleteAllJsonDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllJsonDialog = false },
            confirmButton = {
                Text(
                    text = "Yes",
                    modifier = Modifier
                        .clickable {
                            // Remove all json files
                            jsonFiles.forEach { it.delete() }
                            jsonFiles = mutableListOf()

                            showDeleteAllJsonDialog = false
                        }
                        .padding(8.dp),
                    color = Color.Red
                )
            },
            dismissButton = {
                Text(
                    text = "No",
                    modifier = Modifier
                        .clickable { showDeleteAllJsonDialog = false }
                        .padding(8.dp),
                    color = Color.Green
                )
            },
            title = {
                Text("Confirm Deletion", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ALL saved Wi-Fi files?",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            containerColor = Color.DarkGray
        )
    }



    if (showWifiDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showWifiDeleteDialog = false },
            confirmButton = {
                Text(
                    text = "Yes",
                    modifier = Modifier
                        .clickable {
                            wifiToDelete?.let { wifi ->
                                val updatedList = wifiNetworks.toMutableList().also { it.remove(wifi) }
                                wifiNetworks = updatedList

                                // Update .csv file
                                val csvFile = File(context.filesDir, "wifis_dataset.csv")
                                if (csvFile.exists()) {
                                    csvFile.writeText("") // empty the file
                                    updatedList.forEach {
                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        val currentTime = it.timestamp
                                        val formattedTime = sdf.format(Date(currentTime))
                                        val line = listOf(it.ssid, it.bssid, it.rssi.toString(), it.frequency.toString(), it.capabilities, formattedTime, it.label, it.latitude, it.longitude).joinToString(";")

                                        csvFile.appendText("$line\n")
                                    }
                                }
                            }
                            showWifiDeleteDialog = false
                        }
                        .padding(8.dp),
                    color = Color.Red
                )
            },
            dismissButton = {
                Text(
                    text = "No",
                    modifier = Modifier
                        .clickable { showWifiDeleteDialog = false }
                        .padding(8.dp),
                    color = Color.Green
                )
            },
            title = {
                Text("Confirm Deletion", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this Wi-Fi entry?",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            containerColor = Color.DarkGray
        )
    }



    if (showDeleteAllWifiDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteAllWifiDialog = false },
            confirmButton = {
                Text(
                    text = "Yes",
                    modifier = Modifier
                        .clickable {
                            // Delete all wifi
                            wifiNetworks = mutableListOf()

                            // empty csv file
                            val csvFile = File(context.filesDir, "wifis_dataset.csv")
                            if (csvFile.exists()) {
                                csvFile.writeText("")
                            }

                            showDeleteAllWifiDialog = false
                        }
                        .padding(8.dp),
                    color = Color.Red
                )
            },
            dismissButton = {
                Text(
                    text = "No",
                    modifier = Modifier
                        .clickable { showDeleteAllWifiDialog = false }
                        .padding(8.dp),
                    color = Color.Green
                )
            },
            title = {
                Text("Confirm Deletion", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ALL observed Wi-Fi entries?",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            containerColor = Color.DarkGray
        )
    }


    if (showSaveCsvDialog) {
        AlertDialog(
            onDismissRequest = { showSaveCsvDialog = false },
            title = { Text("Save WiFi Data", color = Color.White) },
            text = {
                Column {
                    Text("Do you want to save ${wifiNetworks.size} WiFi networks to a CSV file?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("File will be saved as:", color = Color.Gray)
                    Text("wifi_data_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv",
                        color = Color.Green, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Default location: Downloads folder",
                        color = Color.Gray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveCsvDialog = false
                        saveCsvFile(context, wifiNetworks)
                    }
                ) {
                    Text("Save", color = Color.Green)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveCsvDialog = false }
                ) {
                    Text("Cancel", color = Color.Red)
                }
            },
            containerColor = Color.DarkGray
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {

            Box(
                modifier = Modifier
                    .clickable {
                        navController.navigate("home")
                    }
                    .padding(start = 16.dp, end = 32.dp) // space from border
            ) {
                Text(
                    text = "<",
                    color = Color.Green,
                    fontFamily = autowide,
                    fontSize = 35.sp
                )
            }

            tabs.forEachIndexed { index, title ->
                Text(
                    text = title,
                    fontFamily = autowide,
                    color = if (index == selectedTabIndex) Color.Green else Color.Gray,
                    modifier = Modifier
                        .clickable { selectedTabIndex = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Scrollable list .json
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if(selectedTabIndex == 0){

                val filteredFiles = jsonFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search JSON files") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$> Total ${filteredFiles.size}",
                        fontFamily = autowide,
                        color = Color.Green,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Delete All",
                        tint = Color.Red,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                showDeleteAllJsonDialog = true
                            }
                    )
                }

                LazyColumn (
                    modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                )
                {

                    items(filteredFiles) { file ->
                        FileItem(
                            file = file,
                            fileName = file.name,
                            onClick = {
                                try {
                                    val content = file.readText()
                                    jsonContent = content
                                    showDialog = true
                                } catch (e: Exception) {
                                    Log.e("FileViewer", "Error reading file: ${e.message}")
                                }
                            },
                            onDelete = {
                                fileToDelete = file
                                showDeleteDialog = true
                            },
                            onShare = {
                                shareJsonFile(context, file)
                            }
                        )
                    }
                }

            } else {




                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Wifi") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Security Filter
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    securityFilters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) Color.Green else Color.DarkGray, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedFilter = if (isSelected) null else filter
                                }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = filter,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                val filteredWifi = wifiNetworks.filter { wifi ->
                    val securityLevel = wifi.label
                    val matchesSearch = wifi.ssid.contains(searchQuery, ignoreCase = true)
                    val matchesFilter = when (selectedFilter) {
                        null -> true
                        "SAFE" -> securityLevel == WifiSecurityLevel.SAFE
                        "MEDIUM" -> securityLevel == WifiSecurityLevel.MEDIUM
                        "DANGEROUS" -> securityLevel == WifiSecurityLevel.DANGEROUS
                        else -> false
                    }
                    matchesSearch && matchesFilter
                }
                    .sortedByDescending { it.timestamp }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$> Total ${filteredWifi.size}",
                        fontFamily = autowide,
                        color = Color.Green,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.weight(0.1f))

                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Save .csv file",
                        tint = Color.Green,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                showSaveCsvDialog = true
                            }
                    )

                    Spacer(modifier = Modifier.weight(0.1f))

                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Delete All Wi-Fi",
                        tint = Color.Red,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                showDeleteAllWifiDialog = true
                            }
                    )
                }

                LazyColumn (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                {

                    items(filteredWifi) { wifi ->
                        WifiItem(
                            wifi = wifi,
                            onDelete = {
                                wifiToDelete = wifi
                                showWifiDeleteDialog = true
                            },
                            onLocationClick = {
                                if (wifi.latitude != 0.0 && wifi.longitude != 0.0) {
                                    openGoogleMaps(context, wifi)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(file: File, fileName: String, onClick: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1F1F1F), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = fileName,
                color = Color.White,
                fontFamily = autowide,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onClick() }
            )
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // Share button
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Share,
            contentDescription = "Share",
            tint = Color.Green,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    onShare()
                }
        )

        Spacer(modifier = Modifier.weight(0.1f))

        // Deleting button
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Close,
            contentDescription = "Delete",
            tint = Color.Red,
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    onDelete()
                }
        )
    }
}




@Composable
fun WifiItem(wifi: WifiNetworkInfo, onDelete: () -> Unit, onLocationClick: () -> Unit) {

    var color = getColor(wifi.label)



    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1F1F1F), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(
                modifier = Modifier
                    .size(12.dp)
                    .padding(end = 8.dp)
            ) {
                drawCircle(color = color)
            }


            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = "SSID: ${wifi.ssid}",
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }




            Spacer(modifier = Modifier.weight(0.2f))


            // Browse Location button
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.LocationOn,
                contentDescription = "Browse Location",
                tint = Color.Green,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        onLocationClick()
                    }
            )


            Spacer(modifier = Modifier.weight(0.2f))



            // Deleting button
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = "Delete",
                tint = Color.Red,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        onDelete()
                    }
            )
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateString = dateFormat.format(Date(wifi.timestamp))

        Text(text = "BSSID: ${wifi.bssid}", color = Color.Gray, fontSize = 12.sp)
        Text(text = "RSSI: ${wifi.rssi} dBm", color = Color.Gray, fontSize = 12.sp)
        Text(text = "Frequency: ${wifi.frequency} MHz", color = Color.Gray, fontSize = 12.sp)
        Text(text = "Channel : ${wifi.channel}", color = Color.Gray, fontSize = 12.sp)
        Text(text = "Capabilities: ${wifi.capabilities}", color = Color.Gray, fontSize = 12.sp)
        Text(text = "Timestamp: $dateString", color = Color.Gray, fontSize = 12.sp)
        Text(text = "Security Level: ${wifi.label}", color = Color.LightGray, fontSize = 12.sp)
        Text(text = "Latitude: ${wifi.latitude}", color = Color.Gray, fontSize = 12.sp)
        Text(text = "Longitude: ${wifi.longitude}", color = Color.Gray, fontSize = 12.sp)
    }
}


private fun openGoogleMaps(context: Context, wifi: WifiNetworkInfo) {
    try {
        val uri = "geo:${wifi.latitude},${wifi.longitude}?q=${wifi.latitude},${wifi.longitude}(${wifi.ssid})&z=17"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // If Google Maps is not installed, open other viewer
        val uri = "geo:${wifi.latitude},${wifi.longitude}?q=${wifi.latitude},${wifi.longitude}(${wifi.ssid})&z=17"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        context.startActivity(intent)
    }
}


private fun shareJsonFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share WiFi File"))
    } catch (e: Exception) {
        Log.e("FileViewer", "Error sharing file: ${e.message}")
        Toast.makeText(context, "Error sharing file", Toast.LENGTH_SHORT).show()
    }
}

private fun saveCsvFile(context: Context, wifiNetworks: List<WifiNetworkInfo>) {
    try {
        // Create csv content
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvContent = StringBuilder()

        // Add header
        csvContent.append("SSID;BSSID;RSSI;Frequency;Capabilities;Timestamp;SecurityLevel;Latitude;Longitude\n")

        wifiNetworks.forEach { wifi ->
            val formattedTime = sdf.format(Date(wifi.timestamp))
            val line = listOf(
                wifi.ssid,
                wifi.bssid,
                wifi.rssi.toString(),
                wifi.frequency.toString(),
                wifi.capabilities,
                formattedTime,
                wifi.label,
                wifi.latitude.toString(),
                wifi.longitude.toString()
            ).joinToString(";")
            csvContent.append("$line\n")
        }

        // Create temporary file
        val tempFile = File.createTempFile(
            "wifi_${System.currentTimeMillis()}",
            ".csv",
            context.cacheDir
        ).apply {
            writeText(csvContent.toString())
        }

        // Check if file is not empty
        if (tempFile.length() == 0L) {
            throw Exception("Generated CSV file is empty")
        }

        // Create intent to share the file
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tempFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WiFi Data Export")
            putExtra(Intent.EXTRA_TITLE, "wifi_data_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start activity with selector
        context.startActivity(
            Intent.createChooser(
                intent,
                "Save WiFi Data As..."
            )
        )

    } catch (e: Exception) {
        Log.e("FileViewer", "Error saving CSV: ${e.message}")
        Toast.makeText(
            context,
            "Error: ${e.message ?: "Failed to save file"}",
            Toast.LENGTH_LONG
        ).show()
    }
}