package com.archimedeprojects.arihna.core.location.data.sqlite

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.icu.lang.UCharacter
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.UnsupportedCityTimeZoneException
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.location.model.VerifiedTimeZoneCompatibility
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class SQLiteCityRepository(context: Context) : CityRepository {
    private val bundledDatabase = BundledCityDatabase(context)

    override suspend fun search(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        val normalized = normalizeSearchQuery(query)
        if (normalized.isEmpty()) return@withContext emptyList()

        val matchExpression = normalized
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(separator = " ") { token -> "$token*" }
        if (matchExpression.isEmpty()) return@withContext emptyList()

        val database = bundledDatabase.openReadOnly()
        database.rawQuery(
            SEARCH_SQL,
            arrayOf(
                normalized,
                normalized,
                normalized,
                matchExpression,
                SEARCH_LIMIT.toString(),
            ),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.readSearchResult())
            }
        }
    }

    override suspend fun findById(id: Long): ManualCity? = withContext(Dispatchers.IO) {
        if (id <= 0L) return@withContext null
        findByIdBlocking(bundledDatabase.openReadOnly(), id)
    }

    override suspend fun nearest(coordinates: Coordinates): CitySearchResult? = withContext(Dispatchers.IO) {
        if (!coordinates.isValid) return@withContext null

        val database = bundledDatabase.openReadOnly()
        val nearestId = findNearestId(database, coordinates) ?: return@withContext null
        findSearchResultByIdBlocking(database, nearestId)
    }

    private fun findByIdBlocking(database: SQLiteDatabase, id: Long): ManualCity? =
        database.rawQuery(FIND_BY_ID_SQL, arrayOf(id.toString())).use { cursor ->
            if (cursor.moveToFirst()) cursor.readManualCity() else null
        }

    private fun findSearchResultByIdBlocking(database: SQLiteDatabase, id: Long): CitySearchResult? =
        database.rawQuery(FIND_BY_ID_SQL, arrayOf(id.toString())).use { cursor ->
            if (cursor.moveToFirst()) cursor.readSearchResult() else null
        }

    private fun findNearestId(database: SQLiteDatabase, origin: Coordinates): Long? {
        var fallbackId: Long? = null
        var fallbackDistanceKm = Double.POSITIVE_INFINITY

        for (radiusKm in NEAREST_RADII_KM) {
            val bounds = boundingBox(origin, radiusKm)
            val query = nearestCandidateQuery(bounds)
            val args = bounds.selectionArgs()
            var bestId: Long? = null
            var bestDistanceKm = Double.POSITIVE_INFINITY

            database.rawQuery(query, args).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val candidate = Coordinates(
                        latitude = cursor.getLong(1).toDouble() / E6,
                        longitude = cursor.getLong(2).toDouble() / E6,
                    )
                    val distanceKm = haversineKilometers(origin, candidate)
                    if (distanceKm < bestDistanceKm) {
                        bestDistanceKm = distanceKm
                        bestId = id
                    }
                }
            }

            if (bestId != null && bestDistanceKm < fallbackDistanceKm) {
                fallbackId = bestId
                fallbackDistanceKm = bestDistanceKm
            }
            // The bounding box conservatively contains the whole radius circle.
            // Once a candidate is actually within that radius, no point outside
            // the box can be closer, so the result is globally nearest.
            if (bestId != null && bestDistanceKm <= radiusKm) return bestId
        }

        return fallbackId
    }

    private fun boundingBox(origin: Coordinates, radiusKm: Double): BoundingBox {
        // 109 km/degree is deliberately conservative: it yields a box at least
        // as large as the corresponding great-circle radius at all latitudes.
        val latitudeDelta = radiusKm / CONSERVATIVE_KM_PER_DEGREE
        val minLatitude = max(-90.0, origin.latitude - latitudeDelta)
        val maxLatitude = min(90.0, origin.latitude + latitudeDelta)
        val minLatitudeE6 = floor(minLatitude * E6).toLong()
        val maxLatitudeE6 = ceil(maxLatitude * E6).toLong()

        if (minLatitude <= -90.0 || maxLatitude >= 90.0) {
            return BoundingBox(minLatitudeE6, maxLatitudeE6, LongitudeBounds.Full)
        }

        val outerLatitude = min(89.999_999, max(abs(minLatitude), abs(maxLatitude)))
        val longitudeScale = cos(Math.toRadians(outerLatitude))
        val longitudeDelta = if (longitudeScale <= 1e-9) {
            180.0
        } else {
            min(180.0, radiusKm / (CONSERVATIVE_KM_PER_DEGREE * longitudeScale))
        }
        if (longitudeDelta >= 180.0) {
            return BoundingBox(minLatitudeE6, maxLatitudeE6, LongitudeBounds.Full)
        }

        val rawMinLongitude = origin.longitude - longitudeDelta
        val rawMaxLongitude = origin.longitude + longitudeDelta
        return when {
            rawMinLongitude < -180.0 -> BoundingBox(
                minLatitudeE6,
                maxLatitudeE6,
                LongitudeBounds.Wrapped(
                    lowerFromE6 = floor((rawMinLongitude + 360.0) * E6).toLong(),
                    upperToE6 = ceil(rawMaxLongitude * E6).toLong(),
                ),
            )

            rawMaxLongitude > 180.0 -> BoundingBox(
                minLatitudeE6,
                maxLatitudeE6,
                LongitudeBounds.Wrapped(
                    lowerFromE6 = floor(rawMinLongitude * E6).toLong(),
                    upperToE6 = ceil((rawMaxLongitude - 360.0) * E6).toLong(),
                ),
            )

            else -> BoundingBox(
                minLatitudeE6,
                maxLatitudeE6,
                LongitudeBounds.Range(
                    minE6 = floor(rawMinLongitude * E6).toLong(),
                    maxE6 = ceil(rawMaxLongitude * E6).toLong(),
                ),
            )
        }
    }

    private fun nearestCandidateQuery(bounds: BoundingBox): String = when (bounds.longitude) {
        LongitudeBounds.Full -> NEAREST_CANDIDATES_FULL_LONGITUDE_SQL
        is LongitudeBounds.Range -> NEAREST_CANDIDATES_RANGE_SQL
        is LongitudeBounds.Wrapped -> NEAREST_CANDIDATES_WRAPPED_SQL
    }

    private fun BoundingBox.selectionArgs(): Array<String> = when (val longitudeBounds = longitude) {
        LongitudeBounds.Full -> arrayOf(minLatitudeE6.toString(), maxLatitudeE6.toString())
        is LongitudeBounds.Range -> arrayOf(
            minLatitudeE6.toString(),
            maxLatitudeE6.toString(),
            longitudeBounds.minE6.toString(),
            longitudeBounds.maxE6.toString(),
        )

        is LongitudeBounds.Wrapped -> arrayOf(
            minLatitudeE6.toString(),
            maxLatitudeE6.toString(),
            longitudeBounds.lowerFromE6.toString(),
            longitudeBounds.upperToE6.toString(),
        )
    }

    private fun Cursor.readSearchResult(): CitySearchResult {
        val modernTimeZoneId = getString(TIMEZONE_NAME_INDEX)
        val compatibilityId = if (isNull(TIMEZONE_COMPAT_INDEX)) null else getString(TIMEZONE_COMPAT_INDEX)
        val zoneSupported = VerifiedTimeZoneCompatibility.resolveOrNull(
            modernId = modernTimeZoneId,
            databaseCompatibilityId = compatibilityId,
        ) != null

        return CitySearchResult(
            id = getLong(ID_INDEX),
            name = getString(NAME_INDEX),
            regionName = if (isNull(REGION_INDEX)) null else getString(REGION_INDEX),
            countryName = getString(COUNTRY_NAME_INDEX),
            countryCode = getString(COUNTRY_CODE_INDEX),
            coordinates = Coordinates(
                latitude = getLong(LATITUDE_INDEX).toDouble() / E6,
                longitude = getLong(LONGITUDE_INDEX).toDouble() / E6,
            ),
            timeZoneId = modernTimeZoneId,
            timeZoneSupported = zoneSupported,
        )
    }

    private fun Cursor.readManualCity(): ManualCity {
        val cityId = getLong(ID_INDEX)
        val modernTimeZoneId = getString(TIMEZONE_NAME_INDEX)
        val compatibilityId = if (isNull(TIMEZONE_COMPAT_INDEX)) null else getString(TIMEZONE_COMPAT_INDEX)
        val zoneId = VerifiedTimeZoneCompatibility.resolveOrNull(
            modernId = modernTimeZoneId,
            databaseCompatibilityId = compatibilityId,
        ) ?: throw UnsupportedCityTimeZoneException(cityId, modernTimeZoneId)

        return ManualCity(
            id = cityId,
            name = getString(NAME_INDEX),
            regionName = if (isNull(REGION_INDEX)) null else getString(REGION_INDEX),
            countryName = getString(COUNTRY_NAME_INDEX),
            countryCode = getString(COUNTRY_CODE_INDEX),
            coordinates = Coordinates(
                latitude = getLong(LATITUDE_INDEX).toDouble() / E6,
                longitude = getLong(LONGITUDE_INDEX).toDouble() / E6,
            ),
            zoneId = zoneId,
            timeZoneId = modernTimeZoneId,
        )
    }

    private fun normalizeSearchQuery(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD)
        val folded = UCharacter.foldCase(decomposed, true)
        val result = StringBuilder(folded.length)
        var index = 0
        var previousWasSpace = true

        while (index < folded.length) {
            val codePoint = folded.codePointAt(index)
            index += Character.charCount(codePoint)
            val type = Character.getType(codePoint)
            val combiningMark =
                type == Character.NON_SPACING_MARK.toInt() ||
                    type == Character.COMBINING_SPACING_MARK.toInt() ||
                    type == Character.ENCLOSING_MARK.toInt()
            if (combiningMark) continue

            val wordCharacter = Character.isLetterOrDigit(codePoint) || codePoint == '_'.code
            if (wordCharacter) {
                result.appendCodePoint(codePoint)
                previousWasSpace = false
            } else if (!previousWasSpace && result.isNotEmpty()) {
                result.append(' ')
                previousWasSpace = true
            }
        }

        return result.toString().trim()
    }

    private fun haversineKilometers(a: Coordinates, b: Coordinates): Double {
        val latitude1 = Math.toRadians(a.latitude)
        val latitude2 = Math.toRadians(b.latitude)
        val deltaLatitude = latitude2 - latitude1
        val deltaLongitude = Math.toRadians(b.longitude - a.longitude)
        val sinLatitude = sin(deltaLatitude / 2.0)
        val sinLongitude = sin(deltaLongitude / 2.0)
        val haversine =
            sinLatitude * sinLatitude +
                cos(latitude1) * cos(latitude2) * sinLongitude * sinLongitude
        val angularDistance = 2.0 * kotlin.math.asin(sqrt(haversine.coerceIn(0.0, 1.0)))
        return EARTH_RADIUS_KM * angularDistance
    }

    private data class BoundingBox(
        val minLatitudeE6: Long,
        val maxLatitudeE6: Long,
        val longitude: LongitudeBounds,
    )

    private sealed interface LongitudeBounds {
        data object Full : LongitudeBounds
        data class Range(val minE6: Long, val maxE6: Long) : LongitudeBounds
        data class Wrapped(val lowerFromE6: Long, val upperToE6: Long) : LongitudeBounds
    }

    companion object {
        private const val E6 = 1_000_000.0
        private const val SEARCH_LIMIT = 30
        private const val CONSERVATIVE_KM_PER_DEGREE = 109.0
        private const val EARTH_RADIUS_KM = 6_371.0088
        private val NEAREST_RADII_KM =
            doubleArrayOf(25.0, 100.0, 500.0, 2_000.0, 5_000.0, 10_000.0, 20_100.0)

        private const val ID_INDEX = 0
        private const val NAME_INDEX = 1
        private const val REGION_INDEX = 2
        private const val COUNTRY_NAME_INDEX = 3
        private const val COUNTRY_CODE_INDEX = 4
        private const val LATITUDE_INDEX = 5
        private const val LONGITUDE_INDEX = 6
        private const val TIMEZONE_NAME_INDEX = 7
        private const val TIMEZONE_COMPAT_INDEX = 8

        private const val CITY_SELECT = """
            SELECT c.id,
                   c.name,
                   adm.name AS region_name,
                   co.name AS country_name,
                   co.code AS country_code,
                   c.latitude_e6,
                   c.longitude_e6,
                   tz.name AS timezone_name,
                   tz.api28_compat_name AS timezone_compat_name,
                   c.api28_time_zone_supported,
                   c.population
            FROM city AS c
            JOIN country AS co ON co.code = c.country_code
            LEFT JOIN admin1 AS adm ON adm.code = c.admin1_code
            JOIN timezone AS tz ON tz.id = c.timezone_id
        """

        private const val FIND_BY_ID_SQL = CITY_SELECT + " WHERE c.id = ? LIMIT 1"

        private const val SEARCH_SQL = """
            SELECT c.id,
                   c.name,
                   adm.name AS region_name,
                   co.name AS country_name,
                   co.code AS country_code,
                   c.latitude_e6,
                   c.longitude_e6,
                   tz.name AS timezone_name,
                   tz.api28_compat_name AS timezone_compat_name,
                   c.api28_time_zone_supported,
                   c.population,
                   MIN(
                       CASE
                           WHEN alias.normalized_alias = ? THEN 0
                           WHEN substr(alias.normalized_alias, 1, length(?)) = ? THEN 1
                           ELSE 2
                       END
                   ) AS match_rank,
                   MAX(alias.preferred) AS any_preferred
            FROM city_search AS search_index
            JOIN city_alias AS alias ON alias.id = search_index.docid
            JOIN city AS c ON c.id = alias.city_id
            JOIN country AS co ON co.code = c.country_code
            LEFT JOIN admin1 AS adm ON adm.code = c.admin1_code
            JOIN timezone AS tz ON tz.id = c.timezone_id
            WHERE city_search MATCH ?
            GROUP BY c.id
            ORDER BY match_rank ASC,
                     any_preferred DESC,
                     c.population DESC,
                     c.name COLLATE NOCASE ASC,
                     c.id ASC
            LIMIT ?
        """

        private const val NEAREST_CANDIDATES_FULL_LONGITUDE_SQL = """
            SELECT id, latitude_e6, longitude_e6
            FROM city INDEXED BY city_lat_lon_idx
            WHERE latitude_e6 BETWEEN ? AND ?
        """

        private const val NEAREST_CANDIDATES_RANGE_SQL = """
            SELECT id, latitude_e6, longitude_e6
            FROM city INDEXED BY city_lat_lon_idx
            WHERE latitude_e6 BETWEEN ? AND ?
              AND longitude_e6 BETWEEN ? AND ?
        """

        private const val NEAREST_CANDIDATES_WRAPPED_SQL = """
            SELECT id, latitude_e6, longitude_e6
            FROM city INDEXED BY city_lat_lon_idx
            WHERE latitude_e6 BETWEEN ? AND ?
              AND (longitude_e6 >= ? OR longitude_e6 <= ?)
        """
    }
}
