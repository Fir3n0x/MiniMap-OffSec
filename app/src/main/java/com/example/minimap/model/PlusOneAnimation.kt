package com.example.minimap.model

// File to handle "+1" animation when discovering new observed wifi

// Handle "+1" animation
data class PlusOneAnimation(
    val id: String, // BSSID as unique id
    var progress: Float
)