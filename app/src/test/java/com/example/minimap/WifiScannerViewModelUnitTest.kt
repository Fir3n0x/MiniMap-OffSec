package com.example.minimap.model

import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.mockk.*
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WifiScannerViewModelUnitTest {

    private lateinit var context: Context
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var task: Task<Location>

    @Before
    fun setUp() {
        // initial mocks
        context = mockk(relaxed = true)
        fusedClient = mockk(relaxed = true)
        task = mockk(relaxed = true)

        // intercept static calls to LocationServices
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(context) } returns fusedClient

        // by default
        every { fusedClient.lastLocation } returns task
    }

    @Test
    fun `initLocationClient initialize the client`() {
        val viewModel = WifiScannerViewModel()

        // Act
        viewModel.initLocationClient(context)

        // Assert
        verify(exactly = 1) { LocationServices.getFusedLocationProviderClient(context) }
    }

    @Test
    fun `fetchLastLocation calls callback with position`() {
        val viewModel = WifiScannerViewModel()
        viewModel.initLocationClient(context)   // injection mocked client

        // Fictive location
        val fakeLocation = mockk<Location>(relaxed = true)

        val slotListener = slot<OnSuccessListener<Location>>()
        every { task.addOnSuccessListener(capture(slotListener)) } answers {
            slotListener.captured.onSuccess(fakeLocation)
            task
        }

        var callbackLocation: Location? = null

        // Act
        viewModel.fetchLastLocation { loc -> callbackLocation = loc }

        // Assert
        assertNotNull("Callback has to receive a location", callbackLocation)
        verify { fusedClient.lastLocation }
        verify { task.addOnSuccessListener(any()) }
    }
}
