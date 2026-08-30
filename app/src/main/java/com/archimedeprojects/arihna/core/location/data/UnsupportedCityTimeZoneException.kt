package com.archimedeprojects.arihna.core.location.data

/**
 * Internal repository signal used when a catalog row exists but the current platform cannot
 * resolve its authoritative modern IANA timezone natively or through an approved mapping.
 */
class UnsupportedCityTimeZoneException(
    val cityId: Long,
    val timeZoneId: String,
) : IllegalStateException("Unsupported timezone $timeZoneId for city $cityId")
