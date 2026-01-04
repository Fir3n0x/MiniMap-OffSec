package com.example.minimap.model

import androidx.compose.ui.graphics.Color


// File to handle different security level in order to classify wifi with machine learning /assets/wifi_classifier.tflite


enum class WifiSecurityLevel {
    SAFE, MEDIUM, DANGEROUS;

    fun getColor(): Color {
        return when (this) {
            SAFE -> Color.Green
            MEDIUM -> Color.Yellow
            DANGEROUS -> Color.Red
        }
    }
}



fun getColor(securityLevel: WifiSecurityLevel): Color {
    return securityLevel.getColor()
}


fun stringToSecurityLevel(securityLevel: String): WifiSecurityLevel {
    return when(securityLevel) {
        "SAFE" -> WifiSecurityLevel.SAFE
        "MEDIUM" -> WifiSecurityLevel.MEDIUM
        "DANGEROUS" -> WifiSecurityLevel.DANGEROUS
        else -> {
            WifiSecurityLevel.DANGEROUS
        }
    }
}