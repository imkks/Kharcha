package com.kharcha.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

object LocationCaptureHelper {
    private const val CURRENT_LOCATION_TIMEOUT_MS = 2_500L

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
            bestLocation?.toCapturedLocation()
        } catch (_: Throwable) {
            null
        }
    }

    /** Gets a fresh fix for a live transaction, then falls back to the most accurate cached fix. */
    @Suppress("TooGenericExceptionCaught")
    suspend fun getLocationForLiveTransaction(context: Context): CapturedLocation? {
        if (!hasLocationPermission(context)) return null
        val cachedLocation = getLastKnownLocation(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return cachedLocation

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return cachedLocation
            val providers = locationManager.getProviders(true)
            val provider =
                providers.firstOrNull { it == LocationManager.NETWORK_PROVIDER }
                    ?: providers.firstOrNull { it == LocationManager.GPS_PROVIDER }
                    ?: providers.firstOrNull()
                    ?: return cachedLocation
            val currentLocation =
                withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS) {
                    requestCurrentLocation(locationManager, provider, context)
                }
            currentLocation?.toCapturedLocation() ?: cachedLocation
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            cachedLocation
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.R)
    private suspend fun requestCurrentLocation(
        locationManager: LocationManager,
        provider: String,
        context: Context,
    ): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            try {
                locationManager.getCurrentLocation(
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    private fun Location.toCapturedLocation(): CapturedLocation {
        val label = String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
        return CapturedLocation(latitude = latitude, longitude = longitude, label = label)
    }
}
