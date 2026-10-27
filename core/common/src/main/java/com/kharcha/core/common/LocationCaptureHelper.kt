package com.kharcha.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.Locale

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

object LocationCaptureHelper {
    @Suppress("TooGenericExceptionCaught")
    fun hasLocationPermission(context: Context): Boolean {
        return try {
            val fine =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            val coarse =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            fine || coarse
        } catch (_: Throwable) {
            false
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun getLastKnownLocation(context: Context): CapturedLocation? {
        if (!hasLocationPermission(context)) return null
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            var bestLocation: Location? = null
            val providers = lm.getProviders(true)
            for (provider in providers) {
                val loc =
                    try {
                        lm.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    }
                if (loc != null) {
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            }
            bestLocation?.let {
                val label = String.format(Locale.US, "%.4f, %.4f", it.latitude, it.longitude)
                CapturedLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    label = label,
                )
            }
        } catch (_: Throwable) {
            null
        }
    }
}
