package com.archimedeprojects.arihna.core.location.model

import java.time.DateTimeException
import java.time.ZoneId

/**
 * Finite, reviewed compatibility map for modern IANA ids absent from Android 9/API28 tzdata.
 *
 * Resolution is capability-first: the modern id is always attempted first. A compatibility
 * id is used only when the platform cannot resolve the modern id. These entries are not a
 * generic timezone fallback and must stay synchronized with PROJECT_SPEC.md/GEONAMES.md.
 */
object VerifiedTimeZoneCompatibility {
    private val compatibilityIds = mapOf(
        "Europe/Kyiv" to "Europe/Kiev",
        "America/Ciudad_Juarez" to "America/Ojinaga",
        "America/Coyhaique" to "America/Punta_Arenas",
        "Asia/Qostanay" to "Asia/Aqtobe",
    )

    fun expectedCompatibilityId(modernId: String): String? = compatibilityIds[modernId]

    fun resolveOrNull(modernId: String): ZoneId? = resolveOrNull(
        modernId = modernId,
        databaseCompatibilityId = expectedCompatibilityId(modernId),
    )

    fun resolveOrNull(
        modernId: String,
        databaseCompatibilityId: String?,
    ): ZoneId? {
        val expectedCompatibilityId = expectedCompatibilityId(modernId)
        check(databaseCompatibilityId == expectedCompatibilityId) {
            "Timezone compatibility metadata mismatch for $modernId: " +
                "database=$databaseCompatibilityId expected=$expectedCompatibilityId"
        }

        try {
            return ZoneId.of(modernId)
        } catch (_: DateTimeException) {
            // The modern id is authoritative; compatibility is consulted only because this
            // platform's tzdata is older than the source catalog.
        }

        val compatibilityId = expectedCompatibilityId ?: return null
        return try {
            ZoneId.of(compatibilityId)
        } catch (_: DateTimeException) {
            null
        }
    }

    fun approvedMappings(): Map<String, String> = compatibilityIds.toMap()
}
