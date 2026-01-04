package com.example.minimap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class MainActivityUnitTest {

    @Test
    fun checkPermissions_shouldReturnTrue_whenPermissionsGranted() {
        val context = Mockito.mock(Context::class.java)

        Mockito.`when`(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        ).thenReturn(PackageManager.PERMISSION_GRANTED)

        Mockito.`when`(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ).thenReturn(PackageManager.PERMISSION_GRANTED)

        val result = MainActivity().javaClass.getDeclaredMethod("checkPermissions", Context::class.java)
            .apply { isAccessible = true }
            .invoke(MainActivity(), context) as Boolean

        assertTrue(result)
    }

    @Test
    fun checkPermissions_shouldReturnFalse_whenPermissionsDenied() {
        val context = Mockito.mock(Context::class.java)

        Mockito.`when`(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        ).thenReturn(PackageManager.PERMISSION_DENIED)

        val result = MainActivity().javaClass.getDeclaredMethod("checkPermissions", Context::class.java)
            .apply { isAccessible = true }
            .invoke(MainActivity(), context) as Boolean

        assertFalse(result)
    }
}
