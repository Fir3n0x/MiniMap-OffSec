package com.example.minimap.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class WifiSecurityLevelUnitTest {

    @Test
    fun `test getColor returns correct color`() {
        assertEquals(Color.Green, WifiSecurityLevel.SAFE.getColor())
        assertEquals(Color.Yellow, WifiSecurityLevel.MEDIUM.getColor())
        assertEquals(Color.Red, WifiSecurityLevel.DANGEROUS.getColor())
    }

    @Test
    fun `test getColor function returns same as enum method`() {
        assertEquals(WifiSecurityLevel.SAFE.getColor(), getColor(WifiSecurityLevel.SAFE))
        assertEquals(WifiSecurityLevel.MEDIUM.getColor(), getColor(WifiSecurityLevel.MEDIUM))
        assertEquals(WifiSecurityLevel.DANGEROUS.getColor(), getColor(WifiSecurityLevel.DANGEROUS))
    }

    @Test
    fun `test stringToSecurityLevel returns correct enum`() {
        assertEquals(WifiSecurityLevel.SAFE, stringToSecurityLevel("SAFE"))
        assertEquals(WifiSecurityLevel.MEDIUM, stringToSecurityLevel("MEDIUM"))
        assertEquals(WifiSecurityLevel.DANGEROUS, stringToSecurityLevel("DANGEROUS"))
    }

    @Test
    fun `test stringToSecurityLevel returns DANGEROUS for unknown value`() {
        assertEquals(WifiSecurityLevel.DANGEROUS, stringToSecurityLevel("UNKNOWN"))
        assertEquals(WifiSecurityLevel.DANGEROUS, stringToSecurityLevel(""))
        assertEquals(WifiSecurityLevel.DANGEROUS, stringToSecurityLevel("null"))
    }
}
