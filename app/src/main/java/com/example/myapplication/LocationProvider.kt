package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object LocationProvider {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // 1. Try to get Last Known Location (Instantly available)
            val lastTask = fusedLocationClient.lastLocation
            val lastLoc: Location? = Tasks.await(lastTask, 2, TimeUnit.SECONDS)
            if (lastLoc != null) {
                return@withContext "${lastLoc.latitude},${lastLoc.longitude}"
            }

            // 2. If Last Location is null, request a FRESH high-accuracy coordinate
            // We increase the timeout to 5 seconds to give the hardware time to lock
            val currentTask = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, // Force GPS hardware to wake up
                null
            )
            val freshLoc: Location? = Tasks.await(currentTask, 5, TimeUnit.SECONDS)

            if (freshLoc != null) {
                "${freshLoc.latitude},${freshLoc.longitude}"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}