package com.example.minimap.model

import android.content.Context
import android.net.wifi.ScanResult
import android.util.Log
import androidx.compose.runtime.Composable
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


// File to load the model to classify wifi


class WifiClassifier(context: Context) {

    private val interpreter: Interpreter by lazy {
        val options = Interpreter.Options()
        options.setUseXNNPACK(true) // Accélération CPU
        Interpreter(loadModelFile(context), options)
    }


    private fun loadModelFile(context: Context): MappedByteBuffer {
        val assetDescriptor = context.assets.openFd("wifi_classifier.tflite")
        val fileInputStream = FileInputStream(assetDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetDescriptor.startOffset
        val declaredLength = assetDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predictSecurityLevel(features: FloatArray): WifiSecurityLevel {
        val input = Array(1) { features }
        val output = Array(1) { FloatArray(3) } // 3 classes: SAFE, MEDIUM, DANGEROUS

        try {
            interpreter.run(input, output)
        } catch (e: Exception) {
            Log.e("WifiClassifier", "Erreur d'inférence", e)
            WifiSecurityLevel.MEDIUM // Valeur par défaut
        }

        return when (output[0].indices.maxByOrNull { output[0][it] }) {
            2 -> WifiSecurityLevel.SAFE
            1 -> WifiSecurityLevel.MEDIUM
            else -> WifiSecurityLevel.DANGEROUS
        }
    }


    // Function to retrieve features from a ScanResult
    fun extractFeatures(scan: ScanResult, context: Context): FloatArray {
        val caps = scan.capabilities.lowercase()

        return floatArrayOf(
            // is_open
            if (caps.contains("ess") && !(caps.contains("wpa") || caps.contains("rsn"))) 1f else 0f,
            // uses_wep
            if (caps.contains("wep")) 1f else 0f,
            // uses_wpa
            if (caps.contains("tkip")) 1f else 0f,
            // uses_wpa2_ccmp
            if (caps.contains("wpa2") && caps.contains("ccmp")) 1f else 0f,
            // uses_wpa3
            if (caps.contains("sae")) 1f else 0f,
            // wps_enabled
            if (caps.contains("wps")) 1f else 0f,
            // rssi_class
            when {
                scan.level >= -60 -> 0f  // High
                scan.level <= -80 -> 2f  // Low
                else -> 1f               // Medium
            },
            // is_5ghz
            if (scan.frequency > 4000) 1f else 0f,
            // is_hidden
            if (scan.SSID.matches(Regex(".*[0-9A-Fa-f]{4}$"))) 1f else 0f,
            // is_public
            if (PublicWifiDetector.isPublicWifi(scan.SSID, context)) 1f else 0f
        )
    }
}

