package com.kharcha.feature.transactions.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale
import java.util.regex.Pattern

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
    private val COORD_PATTERN =
        Pattern.compile(
            """(?:lat(?:itude)?[:\s=]+)?([-+]?\d{1,2}\.\d{4,8})[,\s]+""" +
                """(?:lon(?:gitude)?|lng)?[:\s=]*([-+]?\d{1,3}\.\d{4,8})""",
            Pattern.CASE_INSENSITIVE,
        )

    private val AT_PLACE_PATTERN =
        Pattern.compile(
            """\b(?:at|in)\s+([A-Za-z0-9\s,.-]{3,35})\b""",
            Pattern.CASE_INSENSITIVE,
        )

    /**
     * Extracts coordinates or location text from stored coordinates/label, SMS body, or user note.
     * Returns null if no location is present (so location row is omitted for older full scan SMS).
     */
    fun extractLocation(
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String? = null,
        rawSms: String? = null,
        note: String? = null,
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

        // 3. Try finding explicit coordinates in note or SMS
        val noteCoords = extractCoordsFromText(note)
        if (noteCoords != null) {
            val label = merchantName ?: String.format(Locale.US, "%.4f, %.4f", noteCoords.first, noteCoords.second)
            return TransactionLocation(
                latitude = noteCoords.first,
                longitude = noteCoords.second,
                displayLabel = label,
            )
        }

        val smsCoords = extractCoordsFromText(rawSms)
        if (smsCoords != null) {
            val label = merchantName ?: String.format(Locale.US, "%.4f, %.4f", smsCoords.first, smsCoords.second)
            return TransactionLocation(
                latitude = smsCoords.first,
                longitude = smsCoords.second,
                displayLabel = label,
            )
        }

        // 4. Try extracting place from note (e.g. "at Indiranagar")
        if (!note.isNullOrBlank()) {
            val placeMatcher = AT_PLACE_PATTERN.matcher(note)
            if (placeMatcher.find()) {
                val place = placeMatcher.group(1)?.trim()
                if (!place.isNullOrBlank() && place.length > 2) {
                    val fullLabel = if (!merchantName.isNullOrBlank()) "$merchantName, $place" else place
                    return TransactionLocation(displayLabel = fullLabel)
                }
            }
        }

        // 5. If no location data exists, return null so Location row is NOT shown
        return null
    }

    private fun extractCoordsFromText(text: String?): Pair<Double, Double>? {
        if (text.isNullOrBlank()) return null
        val matcher = COORD_PATTERN.matcher(text)
        if (matcher.find()) {
            val latStr = matcher.group(1)
            val lngStr = matcher.group(2)
            val lat = latStr?.toDoubleOrNull()
            val lng = lngStr?.toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return Pair(lat, lng)
            }
        }
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
