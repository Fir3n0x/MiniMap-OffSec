package com.example.minimap.model

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.minimap.ui.theme.WifiScanWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// File to centralize the periodic scan of the application



object WorkerManager {
    fun scheduleWifiScan(context: Context, enable: Boolean) {
        val workManager = WorkManager.getInstance(context)

        if (enable) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .setRequiresDeviceIdle(false)
                .build()

            val request = PeriodicWorkRequestBuilder<WifiScanWorker>(
                15, TimeUnit.MINUTES, // Interval
                5, TimeUnit.MINUTES // Flexible frame
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "periodicWifiScan",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request
            ).also {
                Log.d("WorkerManager", "Worker programmed with ID: ${request.id}")
            }

            // Debug worker one-time instant
            val testRequest = OneTimeWorkRequestBuilder<WifiScanWorker>()
                .addTag("DEBUG_WORKER")
                .build()
            workManager.enqueue(testRequest)
        } else {
            workManager.cancelUniqueWork("periodicWifiScan")
        }
    }
}

fun logWorkerStatus(context: Context) {
    val workManager = WorkManager.getInstance(context)
    workManager.getWorkInfosForUniqueWorkLiveData("periodicWifiScan").observeForever { workInfos ->
        workInfos?.forEach { info ->
            android.util.Log.d("WorkerStatus",
                "Worker ${info.id}: ${info.state}, attempts: ${info.runAttemptCount}")
            if (info.state == WorkInfo.State.FAILED) {
                android.util.Log.e("WorkerStatus", "Failure reason: ${info.outputData}")
            }
        }
    }
}