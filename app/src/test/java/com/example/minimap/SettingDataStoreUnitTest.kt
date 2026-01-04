//package com.example.minimap.data.preferences
//
//import android.content.Context
//import androidx.test.core.app.ApplicationProvider
//import androidx.datastore.preferences.core.edit
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.test.runTest
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.robolectric.RobolectricTestRunner
//import org.robolectric.annotation.Config
//import kotlin.intArrayOf
//
//@RunWith(RobolectricTestRunner::class)
//@Config(sdk = [33])
//@OptIn(ExperimentalCoroutinesApi::class)
//class SettingsRepositoryUnitTest {
//
//    private val context: Context = ApplicationProvider.getApplicationContext()
//    private lateinit var repo: SettingsRepository
//
//    @Before
//    fun cleanDataStore() = runTest {
//        // Empty Datastore between 2 tests
//        context.settingsDataStore.edit { it.clear() }
//        repo = SettingsRepository(context)
//    }
//
//    @Test
//    fun `autoScanEnabledFlow is false by default`() = runTest {
//        assertFalse(repo.autoScanEnabledFlow.first())
//    }
//
//    @Test
//    fun `notificationEnabledFlow is false by default`() = runTest {
//        assertFalse(repo.notificationEnabledFlow.first())
//    }
//
//    @Test
//    fun `setAutoScanEnabled true then false`() = runTest {
//        repo.setAutoScanEnabled(true)
//        assertTrue(repo.autoScanEnabledFlow.first())
//
//        repo.setAutoScanEnabled(false)
//        assertFalse(repo.autoScanEnabledFlow.first())
//    }
//
//    @Test
//    fun `setNotificationEnabled true`() = runTest {
//        repo.setNotificationEnabled(true)
//        assertTrue(repo.notificationEnabledFlow.first())
//    }
//
//    @Test
//    fun `setVibrationEnabled persist value`() = runTest {
//        repo.setVibrationEnabled(true)
//        assertTrue(repo.vibrationEnabledFlow.first())
//    }
//
//    @Test
//    fun `setAutoSaveEnabled persist value`() = runTest {
//        repo.setAutoSaveEnabled(true)
//        assertTrue(repo.autoSaveEnabledFlow.first())
//    }
//
//    @Test
//    fun `setShowVersionEnabled persist value`() = runTest {
//        repo.setShowVersionEnabled(true)
//        assertTrue(repo.showVersionEnabledFlow.first())
//    }
//}
