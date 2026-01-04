package com.example.minimap.ui.theme

import android.content.Context
import android.util.Log
import com.example.minimap.model.WifiNetworkInfo
import com.example.minimap.model.stringToSecurityLevel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// File to handle the .csv file, including reading, writing and extracting information as WifiNetwork


fun readKnownWifiKeys(context: Context, fileName: String): MutableSet<String> {
    val file = File(context.filesDir, fileName)
    val knownKeys = mutableSetOf<String>()

    if (!file.exists()) return knownKeys

    file.forEachLine { line ->
        val parts = line.split(";")
        if (parts.size >= 5) {
            val key = "${parts[0]}:${parts[1]}"
            knownKeys.add(key)
        }
    }
    return knownKeys
}


fun readWifiNetworksFromCsv(context: Context, fileName: String): List<WifiNetworkInfo> {
    val file = File(context.filesDir, fileName)
    if (!file.exists()) return emptyList()

    return try {
        file.readLines().mapNotNull { line ->
            val parts = line.split(";")
            if (parts.size >= 9) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val ssid = parts[0]
                val bssid = parts[1]
                val rssi = parts[2].toIntOrNull() ?: -100
                val frequency = parts[3].toIntOrNull() ?: 0
                val channel = parts[4].toIntOrNull() ?: -1
                val capabilities = parts[5]
                val date = try {
                    dateFormat.parse(parts[6]) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
                val timestamp = date.time
                val label = stringToSecurityLevel(parts[7])
                val latitude = parts[8].toDoubleOrNull() ?: 0.0
                val longitude = parts[9].toDoubleOrNull() ?: 0.0

                WifiNetworkInfo(
                    ssid = ssid,
                    bssid = bssid,
                    rssi = rssi,
                    frequency = frequency,
                    channel = channel,
                    capabilities = capabilities,
                    timestamp = timestamp,
                    label = label,
                    latitude = latitude,
                    longitude = longitude
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.e("CSV", "Error reading file", e)
        emptyList()
    }
}



fun appendNewWifisToCsv(context: Context, fileName: String, newNetworks: List<WifiNetworkInfo>) {
    val file = File(context.filesDir, fileName)
    val knownKeys = readKnownWifiKeys(context, fileName)

    file.appendText("") // create if doesn't exist

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val currentTime = System.currentTimeMillis()
    val formattedTime = sdf.format(Date(currentTime))

    newNetworks.forEach { network ->
        val key = "${network.ssid}:${network.bssid}"
        if (!knownKeys.contains(key)) {
            val line = "${network.ssid};${network.bssid};${network.rssi};${network.frequency};${network.channel};${network.capabilities};${formattedTime};${network.label};${network.latitude};${network.longitude}\n"
            file.appendText(line)
            knownKeys.add(key)
        }
    }
}