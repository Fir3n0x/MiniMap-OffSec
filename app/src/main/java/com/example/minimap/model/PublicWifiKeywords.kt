package com.example.minimap.model

import android.content.Context
import android.util.Log
import java.io.IOException

// File to generate different public ssid wifi name to detect if an ssid is public

object PublicWifiKeywords {
    private var keywords: List<String>? = null

    fun loadKeywords(context: Context): List<String> {
        if (keywords == null) {
            keywords = try {
                context.assets.open("public_wifi_keywords.txt")
                    .bufferedReader()
                    .use { it.readLines() }
                    .filter { it.isNotBlank() }
            } catch (e: Exception) {
                Log.e("PublicWifiKeywords", "Error loading keywords", e)
                getDefaultKeywords()
            }
        }
        return keywords!!
    }

    private fun getDefaultKeywords(): List<String> {
        return listOf(
            "eduroam", "citywifi", "public", "free",
            "guest", "hotspot", "wifi-public", "municipal"
        )
    }
}



object PublicWifiDetector {
    private var patterns: List<Pair<PatternType, String>>? = null

    enum class PatternType { SIMPLE, REGEX }

    @Throws(IOException::class)
    fun loadPatterns(context: Context): List<Pair<PatternType, String>> {
        return context.assets.open("public_wifi_patterns.txt")
            .bufferedReader()
            .useLines { lines ->
                lines.filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                    .map { line ->
                        val parts = line.split(":", limit = 2)
                        when (parts[0]) {
                            "simple" -> PatternType.SIMPLE to parts[1]
                            "regex" -> PatternType.REGEX to parts[1]
                            else -> throw IllegalArgumentException("Invalid pattern type: ${parts[0]}")
                        }
                    }
                    .toList()
            }
    }

    fun isPublicWifi(ssid: String, context: Context): Boolean {
        if (patterns == null) {
            patterns = try {
                loadPatterns(context)
            } catch (e: Exception) {
                Log.e("PublicWifiDetector", "Error loading patterns, using defaults", e)
                getDefaultPatterns()
            }
        }

        return patterns!!.any { (type, pattern) ->
            when (type) {
                PatternType.SIMPLE -> ssid.contains(pattern, ignoreCase = true)
                PatternType.REGEX -> Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(ssid)
            }
        }
    }

    private fun getDefaultPatterns(): List<Pair<PatternType, String>> {
        return listOf(
            PatternType.SIMPLE to "eduroam",
            PatternType.SIMPLE to "citywifi",
            PatternType.REGEX to "(?i)(public|free)[\\s_-]?wifi"
        )
    }
}