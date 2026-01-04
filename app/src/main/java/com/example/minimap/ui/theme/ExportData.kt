package com.example.minimap.ui.theme

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.minimap.model.WifiNetworkInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream


// File to export the scan as json (option on the top right corner of scan screen)


@Composable
fun ExportDialog(
    networks: List<WifiNetworkInfo>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("wifi_${System.currentTimeMillis()}.json") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save WiFi Data") },
        text = {
            Column {
                Text("Enter file name:")
                TextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onConfirm(fileName)
                        onDismiss()
                        Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun exportNetworksToJson(context: Context, networks: List<WifiNetworkInfo>, fileName: String) {
    try {
        val jsonData = Json { prettyPrint = true }.encodeToString(networks)
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(jsonData.toByteArray())
        }
        Log.d("Export", "Saved to: ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e("Export", "Error saving file", e)
        Toast.makeText(context, "Error saving file", Toast.LENGTH_SHORT).show()
    }
}