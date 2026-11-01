package com.kharcha

enum class LocationPermissionIssue {
    FOREGROUND_ACCESS,
    BACKGROUND_ACCESS,
}

internal fun locationPermissionIssue(
    sdkInt: Int,
    hasForegroundAccess: Boolean,
    hasBackgroundAccess: Boolean,
): LocationPermissionIssue? =
    when {
        !hasForegroundAccess -> LocationPermissionIssue.FOREGROUND_ACCESS
        sdkInt >= 29 && !hasBackgroundAccess -> LocationPermissionIssue.BACKGROUND_ACCESS
        else -> null
    }
