package com.kharcha.feature.transactions.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

/**
 * Data class representing extracted transaction location.
 */
data class TransactionLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val displayLabel: String,
    val hasCoordinates: Boolean = latitude != null && longitude != null,
)

/**
 * Helper to extract geo coordinates from SMS text, notes, and merchant names,
 * and open them in Google Maps.
 */
object LocationHelper {
    /**
     * Returns only an explicitly stored location. SMS text and notes are never treated as location data.
     */
    fun extractLocation(
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String? = null,
        merchantName: String? = null,
    ): TransactionLocation? {
        // 1. Explicit coordinates stored on transaction (from real-time SMS capture)
        if (latitude != null && longitude != null) {
            val label =
                locationLabel
                    ?: merchantName
                    ?: String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
            return TransactionLocation(
                latitude = latitude,
                longitude = longitude,
                displayLabel = label,
            )
        }

        // 2. Explicit location label stored on transaction
        if (!locationLabel.isNullOrBlank()) {
            return TransactionLocation(displayLabel = locationLabel)
        }

        // No stored location means there is no location action to show.
        return null
    }

    /**
     * Launches Google Maps intent for the given location.
     */
    fun openInGoogleMaps(context: Context, location: TransactionLocation): Boolean {
        return runCatching {
            val uri =
                if (location.hasCoordinates) {
                    val encodedLabel = Uri.encode(location.displayLabel)
                    val lat = location.latitude
                    val lng = location.longitude
                    Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
                } else {
                    Uri.parse("geo:0,0?q=${Uri.encode(location.displayLabel)}")
                }

            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.google.android.apps.maps")
            }

            // Verify Google Maps is installed, else use generic geo intent
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(genericIntent)
            }
            true
        }.getOrDefault(false)
    }
}
