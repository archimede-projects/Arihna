package com.archimedeprojects.arihna.core.location.data.sqlite

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.core.location.data.UnsupportedCityTimeZoneException
import com.archimedeprojects.arihna.core.location.model.VerifiedTimeZoneCompatibility
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SQLiteCityRepositoryAndroidTest {
    private lateinit var context: Context
    private lateinit var databaseProvider: BundledCityDatabase
    private lateinit var repository: SQLiteCityRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseProvider = BundledCityDatabase(context)
        repository = SQLiteCityRepository(context)
    }

    @After
    fun tearDown() {
        databaseProvider.close()
    }

    @Test
    fun bundledDatabaseOpensReadOnlyAndHasOnlyApprovedRuntimeIndex() {
        val database = databaseProvider.openReadOnly()
        assertTrue(database.isOpen)
        assertTrue(database.isReadOnly)

        val explicitIndexes = mutableListOf<String>()
        database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND sql IS NOT NULL ORDER BY name",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) explicitIndexes += cursor.getString(0)
        }
        assertEquals(listOf("city_lat_lon_idx"), explicitIndexes)

        val plan = mutableListOf<String>()
        database.rawQuery(
            """
            EXPLAIN QUERY PLAN
            SELECT id
            FROM city INDEXED BY city_lat_lon_idx
            WHERE latitude_e6 BETWEEN ? AND ?
              AND longitude_e6 BETWEEN ? AND ?
            """.trimIndent(),
            arrayOf("41600000", "42200000", "12000000", "13000000"),
        ).use { cursor ->
            while (cursor.moveToNext()) plan += cursor.getString(3)
        }
        assertTrue(plan.any { it.contains("city_lat_lon_idx") })
    }

    @Test
    fun findByIdReconstructsReadableRomeWithExactE6AndTimezone() = runBlocking {
        val rome = repository.findById(3_169_070L)
        assertNotNull(rome)
        requireNotNull(rome)
        assertEquals("Rome", rome.name)
        assertEquals("Lazio", rome.regionName)
        assertEquals("Italy", rome.countryName)
        assertEquals("IT", rome.countryCode)
        assertEquals("Rome, Lazio, Italy", rome.displayName)
        assertEquals(41.891930, rome.coordinates.latitude, 0.0)
        assertEquals(12.511330, rome.coordinates.longitude, 0.0)
        assertEquals("Europe/Rome", rome.timeZoneId)
        assertEquals(ZoneId.of("Europe/Rome"), rome.zoneId)
    }

    @Test
    fun searchResolvesGoldenAndArabicAliasesThroughFts() = runBlocking {
        val cases = listOf(
            "Roma" to 3_169_070L,
            "Makkah" to 104_515L,
            "Mecca" to 104_515L,
            "New York" to 5_128_581L,
            "Sydney" to 2_147_714L,
            "مكة" to 104_515L,
        )

        for ((query, expectedId) in cases) {
            val results = repository.search(query)
            assertTrue("Expected $expectedId for query $query, got ${results.map { it.id }}", results.isNotEmpty())
            assertEquals("Unexpected top result for $query", expectedId, results.first().id)
            assertTrue("Search must remain bounded", results.size <= 30)
        }
        assertTrue(repository.search("   ").isEmpty())
    }

    @Test
    fun nearestUsesCoordinateIndexWithoutRemovedSecondaryIndexes() = runBlocking {
        val nearest = repository.nearest(Coordinates(41.891930, 12.511330))
        assertNotNull(nearest)
        assertEquals(3_169_070L, nearest?.id)
        assertEquals("Rome, Lazio, Italy", nearest?.displayName)

        val database = databaseProvider.openReadOnly()
        val removedIndexes = setOf(
            "city_country_idx",
            "city_admin1_idx",
            "city_population_idx",
            "city_alias_normalized_idx",
            "city_alias_city_idx",
        )
        database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index'",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                assertFalse("Removed runtime index reappeared: ${cursor.getString(0)}", cursor.getString(0) in removedIndexes)
            }
        }
    }

    @Test
    fun everyBundledTimezoneIsClassifiedOnApi28WithOnlyApprovedMappings() {
        val database = databaseProvider.openReadOnly()
        val approved = VerifiedTimeZoneCompatibility.approvedMappings()
        val expectedMappings = mapOf(
            "Europe/Kyiv" to "Europe/Kiev",
            "America/Ciudad_Juarez" to "America/Ojinaga",
            "America/Coyhaique" to "America/Punta_Arenas",
            "Asia/Qostanay" to "Asia/Aqtobe",
        )
        assertEquals(expectedMappings, approved)

        val mapped = linkedMapOf<String, String>()
        val unsupported = mutableListOf<String>()
        var count = 0
        database.rawQuery(
            "SELECT name,api28_compat_name FROM timezone ORDER BY id",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val modernId = cursor.getString(0)
                val databaseCompatibilityId = if (cursor.isNull(1)) null else cursor.getString(1)
                count += 1

                val direct = try {
                    ZoneId.of(modernId)
                } catch (_: Exception) {
                    null
                }
                if (direct != null) {
                    continue
                }

                val expectedCompatibilityId = approved[modernId]
                assertEquals(
                    "Database compatibility metadata mismatch for $modernId",
                    expectedCompatibilityId,
                    databaseCompatibilityId,
                )
                if (expectedCompatibilityId == null) {
                    unsupported += modernId
                } else {
                    val resolved = VerifiedTimeZoneCompatibility.resolveOrNull(
                        modernId,
                        databaseCompatibilityId,
                    )
                    assertNotNull("Compatibility id did not resolve for $modernId", resolved)
                    mapped[modernId] = requireNotNull(resolved).id
                }
            }
        }

        assertEquals(391, count)
        assertEquals(expectedMappings, mapped)
        assertEquals(listOf("America/Nuuk"), unsupported)
    }

    @Test
    fun approvedApi28MappingsProduceVerifiedCurrentAndSeasonalOffsets() = runBlocking {
        val cases = listOf(
            OffsetCase(703_448L, "Europe/Kyiv", "Europe/Kiev", 2, 3),
            OffsetCase(4_013_708L, "America/Ciudad_Juarez", "America/Ojinaga", -7, -6),
            OffsetCase(3_894_426L, "America/Coyhaique", "America/Punta_Arenas", -3, -3),
            OffsetCase(1_519_928L, "Asia/Qostanay", "Asia/Aqtobe", 5, 5),
        )
        val winter = Instant.parse("2026-01-15T12:00:00Z")
        val summer = Instant.parse("2026-07-15T12:00:00Z")

        for (case in cases) {
            val city = repository.findById(case.cityId)
            assertNotNull("Mapped city not materialized: ${case.modernId}", city)
            requireNotNull(city)
            assertEquals(case.modernId, city.timeZoneId)
            assertEquals(case.compatibilityId, city.zoneId.id)
            assertEquals(ZoneOffset.ofHours(case.winterOffsetHours), city.zoneId.rules.getOffset(winter))
            assertEquals(ZoneOffset.ofHours(case.summerOffsetHours), city.zoneId.rules.getOffset(summer))
        }
    }

    @Test
    fun nuukRowsRemainDiscoverableButCannotMaterializeOnApi28() = runBlocking {
        val database = databaseProvider.openReadOnly()
        assertEquals(
            17L,
            scalarLong(database, "SELECT COUNT(*) FROM city WHERE api28_time_zone_supported=0"),
        )
        assertEquals(
            0L,
            scalarLong(
                database,
                """
                SELECT COUNT(*)
                FROM city AS c
                JOIN timezone AS t ON t.id=c.timezone_id
                WHERE c.api28_time_zone_supported=0 AND t.name<>'America/Nuuk'
                """.trimIndent(),
            ),
        )
        assertEquals(
            0L,
            scalarLong(
                database,
                """
                SELECT COUNT(*)
                FROM city AS c
                JOIN timezone AS t ON t.id=c.timezone_id
                WHERE t.name='America/Nuuk' AND c.api28_time_zone_supported<>0
                """.trimIndent(),
            ),
        )

        val search = repository.search("Nuuk")
        assertTrue(search.isNotEmpty())
        val nuuk = search.first { it.id == 3_421_319L }
        assertEquals("America/Nuuk", nuuk.timeZoneId)
        assertFalse(nuuk.timeZoneSupported)

        val nearest = repository.nearest(Coordinates(64.183470, -51.721570))
        assertNotNull(nearest)
        assertEquals(3_421_319L, nearest?.id)
        assertEquals("America/Nuuk", nearest?.timeZoneId)
        assertFalse(requireNotNull(nearest).timeZoneSupported)

        try {
            repository.findById(3_421_319L)
            fail("Nuuk must not materialize as ManualCity on API28")
        } catch (error: UnsupportedCityTimeZoneException) {
            assertEquals(3_421_319L, error.cityId)
            assertEquals("America/Nuuk", error.timeZoneId)
        }
    }

    @Test
    fun finalAssetHasValidRowsFunctionalFtsAndExactApi28Markers() {
        val database = databaseProvider.openReadOnly()
        assertEquals(224_330L, scalarLong(database, "SELECT COUNT(*) FROM city"))
        assertEquals(258_685L, scalarLong(database, "SELECT COUNT(*) FROM city_alias"))
        assertEquals(258_685L, scalarLong(database, "SELECT COUNT(*) FROM city_search_docsize"))
        assertEquals(17L, scalarLong(database, "SELECT COUNT(*) FROM city WHERE api28_time_zone_supported=0"))
        assertEquals(224_313L, scalarLong(database, "SELECT COUNT(*) FROM city WHERE api28_time_zone_supported=1"))
        assertEquals(
            4L,
            scalarLong(database, "SELECT COUNT(*) FROM timezone WHERE api28_compat_name IS NOT NULL"),
        )
        assertEquals(
            0L,
            scalarLong(
                database,
                """
                SELECT COUNT(*)
                FROM city_search_docsize AS f
                LEFT JOIN city_alias AS a ON a.id=f.docid
                WHERE a.id IS NULL
                """.trimIndent(),
            ),
        )
        assertEquals(
            0L,
            scalarLong(
                database,
                """
                SELECT COUNT(*)
                FROM city_alias AS a
                LEFT JOIN city_search_docsize AS f ON f.docid=a.id
                WHERE f.docid IS NULL
                """.trimIndent(),
            ),
        )
        assertEquals(
            0L,
            scalarLong(
                database,
                """
                SELECT COUNT(*) FROM city
                WHERE id <= 0
                   OR latitude_e6 < -90000000 OR latitude_e6 > 90000000
                   OR longitude_e6 < -180000000 OR longitude_e6 > 180000000
                   OR api28_time_zone_supported NOT IN (0,1)
                """.trimIndent(),
            ),
        )
        assertEquals(
            0L,
            scalarLong(
                database,
                """
                SELECT COUNT(*)
                FROM city AS c
                LEFT JOIN timezone AS t ON t.id=c.timezone_id
                WHERE t.id IS NULL OR t.name IS NULL OR t.name=''
                """.trimIndent(),
            ),
        )

        val golden = listOf(
            Triple("roma", 3_169_070L, 478_213L),
            Triple("makkah", 104_515L, 369_017L),
            Triple("mecca", 104_515L, 501_293L),
            Triple("new york", 5_128_581L, 529_841L),
            Triple("sydney", 2_147_714L, 13_127L),
        )
        for ((alias, cityId, expectedDocId) in golden) {
            val actualDocIds = mutableSetOf<Long>()
            database.rawQuery(
                """
                SELECT search_index.docid
                FROM city_search AS search_index
                JOIN city_alias AS alias_row ON alias_row.id=search_index.docid
                WHERE city_search MATCH ? AND alias_row.city_id=?
                """.trimIndent(),
                arrayOf(alias, cityId.toString()),
            ).use { cursor ->
                while (cursor.moveToNext()) actualDocIds += cursor.getLong(0)
            }
            assertTrue("Golden MATCH failed for $alias/$cityId: $actualDocIds", expectedDocId in actualDocIds)
        }
    }

    @Test
    fun manifestRequestsCoarseLocationOnly() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val requested = packageInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue(Manifest.permission.ACCESS_COARSE_LOCATION in requested)
        assertFalse(Manifest.permission.ACCESS_FINE_LOCATION in requested)
        assertFalse("android.permission.ACCESS_BACKGROUND_LOCATION" in requested)
    }

    private fun scalarLong(database: android.database.sqlite.SQLiteDatabase, sql: String): Long =
        database.rawQuery(sql, null).use { cursor ->
            assertTrue("Expected one scalar row for: $sql", cursor.moveToFirst())
            cursor.getLong(0)
        }

    private data class OffsetCase(
        val cityId: Long,
        val modernId: String,
        val compatibilityId: String,
        val winterOffsetHours: Int,
        val summerOffsetHours: Int,
    )
}
