package com.example.minimap.model

import kotlinx.serialization.Serializable

// File including the data class WifiNetworkInfo

@Serializable
data class WifiNetworkInfo (
    val ssid : String,
    val bssid : String,
    val rssi : Int, // Signal power (Received Signal Strength Indicator) // 0 perfect signal (impossible) else, -30 / -90
    val frequency : Int,
    val capabilities : String,
    val timestamp : Long,
    val label : WifiSecurityLevel = WifiSecurityLevel.DANGEROUS,
    val timestampFormatted : String = "",
    val latitude : Double = 0.0,
    val longitude : Double = 0.0,
    val channel : Int = 0,
    val centerFreq0 : Int = 0,
    val centerFreq1 : Int = 0,
    val operatorFriendlyName : CharSequence = "",
    val venueName : CharSequence = "",
    val isPasspointNetwork : Boolean = false,
    val is80211mcResponder : Boolean = false
)