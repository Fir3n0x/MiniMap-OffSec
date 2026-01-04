package com.example.minimap.model

// File to handle switch between different screens

sealed class Screen(val route : String){
    object Home : Screen("home")
    object WifiScan : Screen("wifi_scan")
    object FileViewer : Screen("fileViewer")
    object ParameterViewer : Screen("parameterViewer")
}