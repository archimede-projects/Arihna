# Arihna — Project Specification

> Source of truth for product requirements, technical constraints, architectural decisions, and implementation history.
>
> **Rule:** every requested change must update this file **before** the corresponding code is written.

## 1. Project identity

- **App name:** `Arihna`
- **Repository:** `archimede-projects/Arihna`
- **Platform:** native Android
- **Language/UI:** Kotlin + Jetpack Compose
- **Primary target device:** Samsung Galaxy S25 on a recent Android version
- **Distribution:** sideloaded APK from GitHub Releases; no Play Store publication
- **Android applicationId / namespace:** `com.archimedeprojects.arihna` — definitive technical identifier, lowercase and fixed for update continuity.
- The visible/canonical product name remains exactly **Arihna** in UI, README, releases and ordinary documentation. Do not introduce variants such as `ArihnaApp` or `Ari7naBiha`.
- The approved extended artwork may render the wordmark as `ARIHNA`; this is a visual treatment only.
- Name inspiration: **«أرحنا بها يا بلال»** (*Arihna biha ya Bilal*), referring to prayer.

## 2. Non-negotiable constraints

1. Zero monetary cost for development and normal use.
2. No credit card, paid API, paid subscription, or developer-store subscription.
3. Prefer free/open-source dependencies; free tiers are acceptable only when no card is required and core offline functionality does not depend on them.
4. Source lives in the private GitHub repository.
5. APK distribution is through GitHub Releases, not Play Store.
6. Intermediate debug APKs must also be published through GitHub Releases rather than retained only as Actions artifacts.
7. Debug APKs must use a persistent signing key so updates install over previous debug builds.
8. Debug/release keystores and credentials must never be committed; store them in GitHub Secrets.
9. Keep history understandable through descriptive commits and this specification.
10. Religious content and prayer-time behavior must never silently invent authoritative data.
11. Location behavior must never silently invent a default city, coordinates or timezone that the user did not choose or authorize.

## 3. Development workflow rule

For every requested feature/change:

1. Read the current `PROJECT_SPEC.md`.
2. Update `PROJECT_SPEC.md` first with the approved requirement/decision.
3. Commit the specification update before the corresponding implementation.
4. Implement the smallest coherent step.
5. Build/test where feasible.
6. Commit with a descriptive message.
7. Keep unresolved choices marked **Pending**.
8. Work one objective at a time; do not begin the next milestone until the current one is closed.

## 4. UX/UI and branding decisions

### 4.1 Mockup phase — CLOSED

- **Base layout:** Layout 1 — Hero Dashboard.
- Character: calm, spacious, premium but sober.
- Home: next prayer is dominant in a hero card; prayer name/time/countdown strongest; location secondary; daily action and motivational/religious content below.
- Prayer Times: exception to the Layout 1 list; use Layout 2 vertical timeline with prayer markers and current-time/progress indicator.
- Qibla: Layout 1 with large compass dominant and bearing/location/calibration secondary.
- Quran: Layout 1; Arabic text dominant, Italian translation secondary, generous reading spacing.
- Alarms: Layout 1 calm list/cards with time, label/schedule and enabled state.
- Settings: Layout 1 spacious grouped settings.

### 4.2 Official palette — CLOSED

- Arihna Green: `#0F5132`
- Arihna Gold: `#D4AF37`
- Arihna Off-white: `#FAFAF6`

Derived accessibility/dark-mode tones are allowed if visually consistent with the official palette.

### 4.3 Logo/app icon — CLOSED

Definitive mark: geometric A monogram with integrated minimal minaret, crescent and three radiating sound waves evoking the adhan.

The mark must contain no Quran verse, divine name, sacred text, or generated Arabic calligraphy.

Approved for 48×48 launcher, 24×24 notification, 16×16 favicon/badge, Android adaptive masks, light/dark contexts and monochrome themed-icon use.

Final Android assets include density launcher/round icons, adaptive foreground/background resources, API 33+ monochrome resources, notification vector and branding colors. Reference/web SVG/favicons live under `docs/branding/`.

## 5. Core feature requirements

### 5.1 Prayer times — calculation engine CLOSED

- Calculate daily Islamic prayer times from coordinates, civil date, timezone and user calculation settings.
- Core calculation is fully offline and requires no cloud/network service.
- Location acquisition is supplied by the separate Location milestone in §5.2.
- Prayer notifications/adhan reminders and recalculation scheduling are later milestones.

Selected engine:

- Adhan Kotlin Multiplatform `com.batoulapps.adhan:adhan2:0.0.7`, MIT.
- Adapter boundary: `PrayerTimeCalculator` / `AdhanPrayerTimeCalculator`.
- Arihna-owned domain types; Adhan-specific types never leak outside the adapter.
- Supported methods: MWL, Umm al-Qura, ISNA, Egyptian, Karachi, Dubai, Kuwait, Qatar, Moonsighting Committee, Singapore, Turkey.
- Asr: `STANDARD` shadow factor 1 / `HANAFI` shadow factor 2.
- High-latitude rules: `AUTOMATIC`, `MIDDLE_OF_THE_NIGHT`, `SEVENTH_OF_THE_NIGHT`, `TWILIGHT_ANGLE`; Arihna AUTO is `abs(latitude) > 48° → SEVENTH_OF_THE_NIGHT`, otherwise `MIDDLE_OF_THE_NIGHT`.
- Polar/extreme failures return controlled `Unavailable`; no fabricated times/fallbacks.
- `PrayerTimeCalculator` receives explicit `Coordinates` and `ZoneId`; DST uses `ZoneRules`, no manual +1/-1 logic.
- Per-prayer integer-minute manual offsets are supported.
- Umm al-Qura Isha: 90 minutes normally, 120 minutes in Ramadan, with Ramadan detected offline via verified `HijrahChronology.INSTANCE`.

Android API 28 HijrahChronology verification passed on 2026-08-29 in run `33245235911`. Final prayer-engine regression passed on run `33248406741`: `testDebugUnitTest`, `assembleDebug`, and `connectedDebugAndroidTest` on Android 9/API28 all succeeded. Final implementation commit: `e5987f878e253085425f9bfebf7bf714c8405de3` (`feat(prayer): implement offline prayer time calculation engine`).

### 5.2 Location — architecture APPROVED / CURRENT MILESTONE

The Location layer supplies real or user-selected `Coordinates + ZoneId` to the existing prayer engine. `PrayerTimeCalculator` remains unchanged.

#### Device location technology

- Use Android framework `android.location.LocationManager`, with `androidx.core.location.LocationManagerCompat` where useful.
- Do not introduce Google Play Services Location/FusedLocationProvider.
- Domain terminology is `Device`, not `GPS`, because Android may use GNSS/GPS, Wi-Fi, cellular or other system providers.
- Foreground location only; no `ACCESS_BACKGROUND_LOCATION` and no foreground location service.

#### Permission policy

- Request only `android.permission.ACCESS_COARSE_LOCATION` in this milestone.
- Do not request `ACCESS_FINE_LOCATION` unless a separately approved future requirement demonstrates a real need.
- Do not request location permission automatically at app startup.
- Request it only after the user explicitly chooses the Device-location path, after a short rationale explaining that location is used to calculate prayer times, remains local to the device, and that manual city selection is available without granting permission.
- If denied, degrade gracefully and always offer manual city selection.

#### Refresh/significant-change policy

Approved constants/behavior:

- fresh-fix timeout: **20 seconds**;
- significant movement threshold: **5 km**;
- minimum foreground update interval: **15 minutes**.

Accept/update a device location when at least one is true:

1. it is the first valid fix;
2. movement from the accepted fix is at least 5 km;
3. the associated device `ZoneId` changes;
4. there is no previously usable fix.

When Arihna enters foreground while `Device` is selected, request a fresh fix and observe significant updates while foreground; stop updates when leaving foreground. Do not persist every provider callback or behave like a navigation tracker.

#### Fresh vs cached

Persist the last real device fix locally with coordinates, captured `ZoneId`, capture timestamp and optional accuracy.

- `FRESH`: obtained in the current resolution/refresh flow.
- `CACHED`: a previously observed real fix reused because a new fix is unavailable.

Cached data is never presented as current; preserve timestamp/age. No arbitrary 24h/48h expiry is imposed in this milestone. Never combine old cached coordinates with a newly changed device timezone.

#### Manual city source — GeoNames offline

Use an offline city dataset derived from **GeoNames `cities500`**, licensed **CC BY 4.0**.

- GeoNames `cities500.txt` is the upstream candidate source, not the final Arihna city set. Its historical `ca 185,000` README estimate is not a reliable current row-count contract; the unfiltered dump used in the first STEP 4 size run produced 235,549 distinct rows in Arihna's pipeline.
- Arihna must filter the upstream dump explicitly by `feature class = P` and a deny-by-default feature-code whitelist. Approved included feature codes are: `PPL`, `PPLA`, `PPLA2`, `PPLA3`, `PPLA4`, `PPLA5`, `PPLC`, `PPLG`, `PPLF`, `PPLR`, `STLMT`.
- `PPLF` and `PPLR` remain included because GeoNames defines them as current populated places (farm village / religious populated place). `STLMT` remains included because it represents a current inhabited settlement with a distinct GeoNames classification; Arihna preserves GeoNames-provided country/timezone data and does not reinterpret geopolitical status.
- Explicitly excluded categories include `PPLX` (section of a larger populated place), `PPLH`/`PPLCH` (historical), `PPLQ` (abandoned), `PPLW` (destroyed), `PPLL` (minor populated locality), and `PPLS` (aggregate/plural populated places). Any present or future feature code not in the whitelist is excluded unless separately reviewed and approved.
- Runtime manual search must not depend on GeoNames web service, Nominatim, Android Geocoder, Google Places/Maps/Geocoding or another online geocoder.
- Dataset is downloaded/preprocessed during development/data-generation, not on normal startup and not on every Gradle build.
- Version the generated asset with deterministic provenance: source retrieval/export metadata where trustworthy, source URL, license, checksum, record count and generator/script version. A hardcoded snapshot date must never be treated as authoritative provenance when `latest` URLs may have changed; exact source SHA-256 values are authoritative for the frozen STEP 4 benchmark snapshot.
- Required attribution: `GeoNames — CC BY 4.0`.

#### Offline city database — FINAL STEP 4 schema

- Precompiled **read-only SQLite** using Android platform APIs; no Room.
- The **final approved runtime-minimal schema** was validated in run `33293822757` against the exact E6 staging database from run `33292976302` (staging DB SHA-256 `e567b7eabb40994d5d9fb95209c050503cf5747a6cdd25ef73d195aeb4003877`). It preserves **224,330 cities**, **258,685 aliases**, and **391 distinct IANA timezone names** with zero logical city/alias mismatches.
- Retain only Arihna-needed fields: GeoNames id, canonical/display name, region, country, country code, latitude, longitude, IANA timezone id and population/ranking data.
- Country and region display names are normalized lookup data, not repeated per city row. The existing `country` and `admin1` tables satisfy this requirement; `city` keeps the short textual country/admin1 codes because numeric replacement yielded only about 34 KB of compressed benefit and is not worth the extra schema/join complexity.
- Store city coordinates as signed integer microdegrees (`latitude_e6`, `longitude_e6`, degrees × 1,000,000) instead of SQLite `REAL`. The generator rejects a source coordinate that cannot be represented at microdegree precision without the approved conversion; the E6 staging gate verified zero round-trip mismatches.
- Normalize repeated IANA timezone strings into `timezone(id INTEGER PRIMARY KEY, name TEXT NOT NULL, api28_compat_name TEXT)` and store only numeric `city.timezone_id`. `timezone.name` always remains the modern GeoNames/IANA id; `api28_compat_name` is nullable and may contain only an explicitly reviewed compatibility id from the API28 policy below.
- Mark each final city row with `api28_time_zone_supported INTEGER NOT NULL` (0/1). This is an auditable statement about the approved Android 9/API28 baseline, not a permanent ban: runtime always tries the modern IANA id first, so a city marked 0 becomes usable automatically on a newer platform/tzdata once its modern id is natively supported.
- Keep `population` because it is part of city-search/disambiguation ranking. SQLite `INTEGER` is already variable-length/compact.
- Final explicit runtime secondary indexes: **only** `city_lat_lon_idx(latitude_e6, longitude_e6)`. `findById` uses the `city` INTEGER PRIMARY KEY. `search` uses FTS4 then `city_alias.id` / `city.id` INTEGER PRIMARY KEY joins. `nearest` uses `city_lat_lon_idx`; run `33293822757` confirmed the query plan `SEARCH c USING INDEX city_lat_lon_idx (latitude_e6>? AND latitude_e6<?)` and does not require any removed country/admin1/population/alias index.
- Do not bundle `city_country_idx`, `city_admin1_idx`, `city_population_idx`, `city_alias_normalized_idx`, `city_alias_city_idx`, or the `(city_id, normalized_alias)` uniqueness index in the final read-only asset unless a future measured runtime requirement justifies reintroducing one.
- Alias deduplication remains mandatory during build-time staging. The final `city_alias` table preserves every already-deduplicated alias id but carries no preprocessing-only UNIQUE/secondary index. Run `33293822757` verified 258,685/258,685 aliases, zero duplicate `(city_id, normalized_alias)` pairs and zero logical alias mismatches.
- Include aliases from canonical/ASCII names and selected useful alternate names, including approved Italian/English/Arabic aliases where available. The large upstream alternate-name source is preprocessing-only and is not bundled wholesale.
- Search is local, normalized/case-insensitive, supports useful prefix/alias lookup and a bounded result set; ranking prefers exact matches, then strong prefix/alias matches, with population/region/country for disambiguation.
- `city_search` remains an FTS4 contentless table (`content=''`) with `docid = city_alias.id`; do not change this schema merely to satisfy a generic integrity command or footprint optimization.
- SQLite 3.44+ invokes virtual-table `xIntegrity` from global `PRAGMA integrity_check`; FTS4 cannot perform that inverted-index validation for this contentless configuration because original content is intentionally absent. Therefore global `PRAGMA integrity_check` is not used as the validation gate for `city_search`.
- Structural SQLite integrity checks remain mandatory for every Arihna-owned non-FTS runtime table (`country`, `admin1`, `timezone`, `city`, `city_alias`) using table-scoped `PRAGMA integrity_check(...)`. Run `33293822757` returned `ok` for all five tables.
- The contentless FTS index must instead pass functional integrity checks: `city_search` document count equals `city_alias`; no FTS docid is orphaned and no alias id is missing from FTS; known golden aliases (`Roma`, `Makkah`, `Mecca`, `New York`, `Sydney`) resolve through `MATCH` to the exact corresponding `city_alias.id`/GeoNames city. Run `33293822757` passed all of these checks.
- Query-plan/performance checks for the compact schema must exercise representative `search`, `findById`, and `nearest` SQL. Run `33293822757` confirmed FTS virtual-table use for search and `city_lat_lon_idx` use for nearest, with no obvious host-SQL regression.

#### APK-size alarm threshold — PASSED for runtime-minimal schema

Measure the real incremental APK size attributable to the generated city database after preprocessing/indexing; do not infer it from upstream ZIP or local zlib estimates.

- Filtered/full-index baseline run `33280106118`: 56,692,736-byte SQLite; 27,459,105-byte compressed APK asset; 27,459,231-byte APK increment.
- E6-only run `33292976302` used the frozen staging DB later reused by the final benchmark: 52,932,608-byte SQLite and 25,585,011-byte APK increment.
- Final runtime-minimal run `33293822757`, using exactly that same staging DB: **27,795,456-byte SQLite**, **15,008,799-byte APK-compressed asset**, baseline APK **33,937,009 bytes**, APK with GeoNames **48,945,932 bytes**, and **15,008,923-byte APK increment**.
- APK asset compression remains ZIP method 8 (DEFLATE). The workflow verified that the decompressed APK asset SHA-256 equals the generated DB SHA-256 `6383538be045a51bbab6ae2e3097f99bdc79851af525c6bbc9fed018d434ce0a`, preventing stale-asset measurement.
- Approved threshold: 20,971,520 bytes (20 MiB). Final margin below threshold: **5,962,597 bytes** (about **5.69 MiB**). `threshold_pass = true`.
- No city or alias was removed to obtain the pass. The runtime-minimal schema is therefore the approved STEP 4 city database layout; do not switch to `cities1000` or raise the threshold for this reason.
- Separate `.gz`/`.zst` precompression is not approved/needed: Android platform SQLite cannot open such a stream directly and the ordinary APK DEFLATE result already passes the gate.

#### Timezone policy

Manual city:

- The modern IANA timezone id comes directly from the GeoNames record and remains authoritative data in `timezone.name`; never overwrite it with a compatibility name.
- Runtime resolution is capability-first: call `ZoneId.of(modernId)` first. Only if that exact modern id is unavailable may Arihna use one of the four explicit compatibility mappings below. There is no generic alias, longitude, fixed-offset, system-default, or “closest timezone” fallback.
- **Category A — approved direct compatibility:** `Europe/Kyiv → Europe/Kiev` is the official IANA rename/Link introduced in tzdb 2022b. `America/Ciudad_Juarez → America/Ojinaga` is not an IANA Link: it is a compatibility rule specific to the Android 9 baseline tzdata 2018e, where `America/Ojinaga` still covered Juárez and projected the same Mountain/US-DST rules (`-07/-06`) that modern `America/Ciudad_Juarez` uses.
- **Category B — approved peer equivalence (not IANA Links):** `America/Coyhaique → America/Punta_Arenas` and `Asia/Qostanay → Asia/Aqtobe`. On 2026-08-30 these pairs were compared automatically every 6 hours from `2026-01-01T00:00Z` through (exclusive) `2101-01-01T00:00Z`, **109,572 instants per pair**, with zero offset discrepancies. `America/Punta_Arenas` in tzdata 2018e is permanent UTC-03 from 2016-12-04; modern Coyhaique is permanent UTC-03. `Asia/Aqtobe` in the API28 baseline is permanent UTC+05 for the relevant horizon; modern Qostanay is UTC+05 after Kazakhstan's 2024 change. Revalidate these peer equivalences whenever the bundled dataset/tzdb baseline or minSdk policy changes materially.
- **Category C — controlled unsupported baseline:** `America/Nuuk` has no safe API28 mapping. Although `America/Godthab` is the old name, tzdata 2018e projects `-03/-02`, while modern Nuuk uses `-02/-01` after Greenland's 2023 rule change, so mapping it would be wrong by one hour. Do not implement a custom timezone-rule engine for this exception.
- The exhaustive frozen dataset scan found exactly **one residual unsupported timezone id (`America/Nuuk`) affecting 17 of 224,330 cities (about 0.0076%)** after applying the four approved mappings. Those 17 rows remain fully present and searchable; they are marked `api28_time_zone_supported=0` and must not materialize as a successful selectable `ManualCity` on API28. Selecting one returns `LocationResolutionState.Unavailable(LocationFailure.UNSUPPORTED_TIME_ZONE, ...)` with no fallback and no crash.
- Exact Category C city list for the frozen STEP 4 dataset (GeoNames id — name — region when available): `3424901 Aasiaat — Qeqertalik`; `3423146 Ilulissat — Avannaata`; `3422683 Kangaatsiaq`; `3419714 Kangerlussuaq — Qeqqata`; `3421982 Maniitsoq — Qeqqata`; `3421765 Nanortalik — Kujalleq`; `3421719 Narsaq — Kujalleq`; `3421319 Nuuk — Sermersooq`; `3421193 Paamiut — Sermersooq`; `3420846 Qaqortoq — Kujalleq`; `3420768 Qasigiannguit — Qeqertalik`; `3420635 Qeqertarsuaq — Qeqertalik`; `3420636 Qeqertarsuaq — Sermersooq`; `3419842 Sisimiut — Qeqqata`; `3424607 Tasiilaq — Sermersooq`; `3418910 Upernavik — Avannaata`; `3426193 Uummannaq — Avannaata`. All are GeoNames rows whose timezone id is `America/Nuuk`; Arihna preserves that source assignment rather than reinterpreting Greenland geography.
- Search/nearest must remain able to return a lightweight city candidate for Category C records so they stay discoverable by name/position. Successful `ManualCity` materialization remains reserved for cities whose modern timezone resolves natively or via an approved compatibility mapping.
- The modern IANA id must remain available for persistence/audit even when API28 resolves a verified compatibility id. A future platform whose tzdata recognizes the modern id uses it directly, so the compatibility mapping naturally becomes unnecessary.

Device:

- A fresh device fix captures the device's current `ZoneId` at acceptance time.
- Timezone change is independently significant even when movement is below 5 km.
- Cached fixes retain their captured timezone.

#### Domain models

Keep user preference separate from resolved runtime state. Approved conceptual Arihna-owned models:

```text
LocationPreference
- Unset
- Device
- Manual(ManualCitySnapshot)

LocationSource
- Device(capturedAt, accuracyMeters?)
- Manual(cityId)

ManualCity
- id
- name
- regionName?
- countryName
- countryCode
- coordinates
- modernTimeZoneId
- zoneId

CitySearchResult
- id/name/region/country/countryCode/coordinates/displayName
- timeZoneSupported on the current runtime

ManualCitySnapshot
- persisted serializable snapshot of selected city identity/display/coordinates/modern timezone id

SelectedLocation
- source
- coordinates
- zoneId
- displayName

DeviceLocationFix
- coordinates
- zoneId
- capturedAt
- accuracyMeters?

LocationFreshness
- FRESH
- CACHED

LocationPermissionState
- NotRequested
- Granted
- Denied(canRequestAgain)

LocationResolutionState
- Unconfigured
- Resolving
- Ready(location, freshness?)
- PermissionDenied(canRequestAgain, cachedLocation?)
- LocationServicesDisabled(cachedLocation?)
- Unavailable(reason, cachedLocation?)

LocationFailure
- TIMEOUT
- NO_PROVIDER
- INVALID_FIX
- CITY_NOT_FOUND
- CITY_DATASET_UNAVAILABLE
- UNSUPPORTED_TIME_ZONE
- PERSISTENCE_ERROR
```

Persist a manual-city snapshot rather than only GeoNames id so later dataset changes do not silently erase the user's selection. GeoNames id remains available for reconciliation.

#### Testable boundaries

Conceptual boundaries:

```text
DeviceLocationDataSource
- getCurrentLocation()
- observeSignificantUpdates()

CityRepository
- search(query)
- findById(id)
- nearest(coordinates)

LocationPreferencesRepository
- observe preference
- observe cached device fix
- select device
- select manual city
- save device fix
```

A coordinator/repository combines user preference, permission state, Android `LocationManager`, cached fix, city repository and timezone into `LocationResolutionState`.

The prayer engine consumes only:

```text
SelectedLocation.coordinates
SelectedLocation.zoneId
```

No change to `PrayerTimeCalculator` is authorized.

#### Persistence — Preferences DataStore

Introduce **AndroidX Preferences DataStore `1.2.1`**.

- No SharedPreferences for new Location persistence.
- No Proto DataStore for this simple state.
- No Room.
- Persist mode `UNSET | DEVICE | MANUAL`.
- For Manual persist city snapshot: id, name/region/country metadata, coordinates, **modern IANA timezone id**. Snapshot restoration uses the same capability-first explicit compatibility resolver as repository materialization; do not persist a compatibility id in place of the source id.
- Persist last real Device fix: coordinates, captured timezone id, timestamp, optional accuracy.
- Persist only minimal permission-flow metadata if needed; Android remains authority on current permission state.
- All data remains local; no upload/cloud sync.
- Malformed/incomplete persisted data produces controlled unconfigured/error state, never arbitrary reconstructed defaults.

#### Error/fallback policy

**First launch / no selection**

- `LocationPreference.Unset` / `LocationResolutionState.Unconfigured`.
- No prayer calculation with invented coordinates.
- Functional UI asks `Use current/device location` or `Choose a city`.

**Permission denied**

- Without cached real fix: `PermissionDenied`, no selected coordinates; offer retry/settings as appropriate plus manual city.
- With cached real fix: cached value may remain usable only as `CACHED`, while denial remains visible/actionable.

**Location Services disabled**

- With cache: keep real cached value as `CACHED` and surface disabled services.
- Without cache: no coordinates; offer enable services or manual city.

**Timeout/provider unavailable/invalid fix**

- With previous real fix: use it as `CACHED` and surface the failure.
- Without previous real fix: controlled `Unavailable`; no substitute city/coordinates.

**Manual selection**

- Manual mode wins until user explicitly returns to Device.
- Switching to Manual stops active Device updates.
- Switching to Device re-enters permission/fresh-fix flow; cached Device data follows the explicit cached policy above.
- A searchable city whose modern timezone is neither natively supported nor covered by an approved compatibility mapping on the current runtime produces `Unavailable(UNSUPPORTED_TIME_ZONE)` and is not persisted as a successful Manual selection.

#### Minimal functional UI

Only enough Compose UI to exercise Location; do not implement final Hero Dashboard.

User can:

- see active source Device vs Manual;
- choose Device location and understand permission rationale;
- search/select a manual city offline;
- see selected readable city/location;
- return from Manual to Device simply;
- understand permission denied, services disabled, timeout/cached, unsupported-timezone and unconfigured states;
- see GeoNames attribution in relevant functional/about path.

### 5.3 Qibla

- Determine Qibla bearing from current coordinates to the Kaaba.
- Use Android sensors for live compass direction and surface calibration/accuracy gracefully.
- Remain calculable offline once coordinates are available.

### 5.4 Alarms

- Custom alarms in addition to prayer-linked alarms.
- Configurable sounds.
- Prayer/custom alarm reliability must respect Android standby/doze restrictions.

### 5.5 Quran

- Arabic Quran text readable offline with surah navigation.
- Evaluate optional Italian translation for redistribution license, attribution, integrity, size and offline packaging.
- **Pending:** bundled vs optional pack vs omitted in v1.
- Religious text must come from a verified attributable source and never be silently altered.

### 5.6 Daily motivational content

Morning/evening offline-oriented content may include a practical action, reflection, Quran verse and/or authentic hadith. Content changes daily, avoids close repeats, and stores verifiable source metadata. Quran citations include surah/verse; hadith include collection/reference and authenticity/source information. Never fabricate quotations/references.

## 6. Permissions and Android behavior

### 6.1 Location — approved milestone policy

- Request only `ACCESS_COARSE_LOCATION` in the current Location milestone.
- Do not request `ACCESS_FINE_LOCATION` or `ACCESS_BACKGROUND_LOCATION`.
- Do not request permission automatically at startup; request only after explicit Device-location choice and concise rationale.
- Explain that location is used for prayer-time calculation, remains local, and manual city selection is available without permission.
- Treat denial and disabled Location Services as explicit states; never fabricate a default location.
- Any future precise/background location requires separately approved decision.

### 6.2 Notifications

Request `POST_NOTIFICATIONS` on Android 13+ only when notifications are enabled/requested. Separate channels for prayer/adhan and daily content where useful.

### 6.3 Exact alarms / standby reliability

Evaluate `AlarmManager` exact alarms including `setExactAndAllowWhileIdle`, special access restrictions, and Samsung sleeping-app/battery caveats. Use WorkManager only for deferrable/non-exact work. Avoid broad battery-optimization exemptions unless justified.

## 7. Offline/data requirements

Maximize offline operation. Quran text, optional translation packs, curated daily content, settings, alarms, prayer calculation from saved coordinates/settings and Qibla bearing should work locally once required data exists. Core prayer calculation requires no network access.

Location adds an offline GeoNames-derived catalog so manual city search, coordinates and timezone lookup work without network access. Runtime manual-location behavior must not depend on third-party geocoding.

## 8. Android technical architecture — bootstrap CLOSED

### 8.1 App architecture

- One `:app` module initially.
- Package-level separation rather than premature Gradle multi-module decomposition.
- Manual `AppContainer` DI; no Hilt/Koin for now.
- Feature-oriented packages plus core/domain/platform/data boundaries as needed.
- Lifecycle ViewModels/Flow and unidirectional Compose state when feature UI begins.

### 8.2 Approved dependencies/components

- Prayer: `com.batoulapps.adhan:adhan2:0.0.7` (MIT).
- Location persistence: `androidx.datastore:datastore-preferences:1.2.1`.
- Device location: Android `LocationManager` / AndroidX Core helpers; no Google Play Services Location dependency.
- Manual city data: precompiled read-only SQLite derived from GeoNames `cities500`, CC BY 4.0; no Room.
- Unit/instrumentation test dependencies required for verification.

WorkManager, alarms/notifications, Quran data libraries and Google/Fused location providers remain deferred/not approved here.

### 8.3 Data concepts

- `PrayerCalculationSettings`, `PrayerDay`.
- `LocationPreference`, `LocationSource`.
- `ManualCity`, `CitySearchResult`, `ManualCitySnapshot`.
- `DeviceLocationFix`, `SelectedLocation`.
- `LocationFreshness`, `LocationPermissionState`, `LocationResolutionState`, `LocationFailure`.
- `Alarm`: later milestone.
- `QuranSurah` / `QuranAyah`: later milestone.
- `DailyContent` / history: later milestone.

### 8.4 Definitive Android/build toolchain

#### Build host toolchain

- GitHub Actions / CI Gradle host JDK: **Temurin 21**.
- Gradle daemon, AGP, D8/R8 and JVM-hosted tests execute on JDK 21.
- Any current/future Arihna workflow invoking Gradle for Android build/test/package/release must provision JDK 21 unless deliberately diagnostic.
- Future automatic debug/release/tag workflow uses JDK 21 host.

#### App bytecode target

- Java `sourceCompatibility`: **17**.
- Java `targetCompatibility`: **17**.
- Kotlin `jvmTarget`: **JVM 17**.
- JDK 21 build host does not raise Arihna app bytecode target.

#### Android

- applicationId/namespace: `com.archimedeprojects.arihna`
- minSdk 28
- compileSdk 37
- targetSdk 37

#### Remaining toolchain

- AGP 9.3.1
- Gradle wrapper 9.5.0
- Kotlin 2.4.10
- Compose compiler plugin 2.4.10
- AGP built-in Kotlin; no `org.jetbrains.kotlin.android`
- Compose BOM 2026.08.00
- Kotlin DSL
- version catalog `gradle/libs.versions.toml`
- one `:app` module
- manual `AppContainer`

Adhan `CalculationMethod.class` was directly verified as class-file major 65 (Java 21); run `33247757115` proved JDK21 host works while app target remains 17.

### 8.5 Bootstrap shell — CLOSED

Contains `ArihnaApplication`, `MainActivity`, `ArihnaApp`, `AppContainer`, base theme, navigation shell and placeholder destinations. Approved branding assets are wired through resources/manifest.

`versionName = 0.1.0-bootstrap` is temporary; normalize to clean SemVer before first real public release. Pre-31 branded splash fallback remains deferred to a later UI/release milestone.

## 9. Repository structure

Single-module Android project with `app/src/main`, `app/src/test`, `app/src/androidTest`, version catalog and Gradle wrapper. Location remains inside `:app` using domain/data/platform/UI boundaries; do not create a Gradle module only for this milestone.

## 10. CI/CD requirements

All Gradle/Android workflows use Temurin JDK 21 host while Arihna bytecode target remains 17.

### Debug APK

Build signed debug APKs with persistent debug keystore reconstructed from GitHub Secrets and publish through GitHub Releases, not solely Actions artifacts. Final trigger convention pending.

### Stable release

On approved tags/releases: checkout, provision JDK21/Android tooling, test/lint, reconstruct signing key, build signed APK, checksum if practical, create/update GitHub Release and attach APK. Production signing key remains persistent and secret.

## 11. README requirements

Eventually document Arihna, supported device notes, GitHub Release install/update, Samsung sideload guidance, debug vs stable, permissions, battery/alarm caveats and third-party/religious-data attributions. GeoNames/CC BY 4.0 attribution is required when city data is integrated.

## 12. Religious-source integrity

Never present Quran/hadith from memory as authoritative app content without verification. Store provenance/source metadata. Preserve Quran text integrity and licensing. Any translation requires redistribution-compatible permission/license.

## 13. Testing expectations

### Prayer regression

Keep representative prayer golden/mapping/high-latitude/DST/Ramadan/error tests and API28 HijrahChronology regression passing.

### Location pure/domain tests — no real GPS required

Use fakes. Cover at least:

- first valid fix accepted;
- invalid coordinates rejected;
- movement <5 km ignored, movement >=5 km accepted;
- timezone change accepted even below 5 km;
- fresh → `FRESH`;
- timeout/provider unavailable with cache → `CACHED`; without cache → controlled unavailable;
- permission denied with/without cache;
- Location Services disabled with/without cache;
- manual selection yields exact stored coordinates/timezone and never uses `ZoneId.systemDefault()` for remote city;
- Device ↔ Manual switching;
- persisted manual snapshot survives restart and remains usable if later dataset lookup is missing;
- malformed snapshot → controlled error/unconfigured;
- unsupported timezone → `UNSUPPORTED_TIME_ZONE`;
- empty/no-result search never creates arbitrary city;
- city ranking/aliases including representative Italian/English/Arabic names;
- DataStore round-trip for Unset, Device, Manual snapshot and cached Device fix;
- `SelectedLocation.coordinates + zoneId` pass unchanged to existing prayer boundary.

### Android API28 Location instrumentation gate

Before Location STEP 4 closure, Android 9/API28 tests must verify:

1. generated read-only city SQLite opens;
2. known cities resolve expected timezone semantics (Rome, Makkah, New York, Sydney representatives);
3. approved aliases such as Makkah/Mecca resolve intended record;
4. every distinct bundled modern timezone id is exhaustively classified as either (a) natively resolvable by `ZoneId.of(modernId)`, (b) resolvable only through one of the four explicit reviewed compatibility mappings, or (c) explicitly marked unsupported on API28. No other unresolved id is allowed;
5. the four approved mappings are exactly `Europe/Kyiv→Europe/Kiev`, `America/Ciudad_Juarez→America/Ojinaga`, `America/Coyhaique→America/Punta_Arenas`, `Asia/Qostanay→Asia/Aqtobe`; their API28 compatibility ids resolve and produce the verified current/future offsets;
6. the residual unsupported set is exactly `America/Nuuk`, with exactly **17 city rows**, all marked `api28_time_zone_supported=0`; those rows remain searchable/nearest-discoverable but successful manual materialization/selection returns controlled `UNSUPPORTED_TIME_ZONE`;
7. no bundled row has invalid coordinates, missing timezone, invalid primary id, orphan timezone, FTS orphan or missing alias docid;
8. table-scoped non-FTS integrity and golden FTS MATCH cases remain green;
9. Preferences DataStore real read/write works on API28, including a mapped modern timezone id surviving snapshot persistence/restoration;
10. manifest permissions match policy: `ACCESS_COARSE_LOCATION` only, no FINE/BACKGROUND.

No CI test requires physical GPS movement or runner geographic position.

### Location final gate

Host JDK: Temurin 21. Run at least:

```bash
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
./gradlew :app:connectedDebugAndroidTest --stacktrace
```

All must pass, including previous prayer regression and new Location/API28 checks.

Measure APK before/after city database integration. The finalized runtime-minimal city asset passed the 20 MiB incremental threshold in run `33293822757`; preserve that schema/measurement discipline during subsequent STEP 4 work.

Future milestones add Qibla math, exact-alarm reboot/timezone/time changes, notification flows, daily-content anti-repeat, Quran integrity and final Compose UI tests.

## 14. Decision status

### Closed

- applicationId/namespace `com.archimedeprojects.arihna`.
- Bootstrap architecture/toolchain and structural build.
- JDK21 build-host policy; app bytecode remains target 17.
- UI palette/layout direction and branding assets.
- Prayer calculation engine and domain.
- 11 methods, Standard/Hanafi, high-latitude AUTO, ZoneId/ZoneRules, prayer offsets, polar controlled errors.
- API28 HijrahChronology gate and Umm al-Qura 90/120 Ramadan rule.
- Final prayer regression and implementation commit on `main`.

### Approved / current Location milestone

- Native LocationManager, no Play Services.
- Foreground Device location only.
- `ACCESS_COARSE_LOCATION` only.
- 20s timeout / 5 km / 15 min; timezone change significant.
- FRESH/CACHED, never invented defaults.
- GeoNames `cities500` offline, CC BY 4.0, filtered by the approved deny-by-default populated-place feature-code whitelist.
- Read-only SQLite; no Room.
- **Runtime-minimal city schema FINAL for STEP 4:** E6 coordinates, numeric timezone lookup, `population` retained, country/admin1 short text codes retained, only `city_lat_lon_idx` as explicit secondary runtime index, alias dedup in build-time staging, FTS4 contentless unchanged. Run `33293822757` measured a 15,008,923-byte APK increment and passed the 20 MiB gate with no city/alias removal.
- API28 timezone policy: four explicit verified compatibility mappings; exactly one residual modern id (`America/Nuuk`) affecting 17 cities is controlled `UNSUPPORTED_TIME_ZONE` on API28 while remaining in/searchable from the dataset.
- Preferences DataStore `1.2.1`.
- Arihna-owned Location models/state/errors; `PrayerTimeCalculator` unchanged.
- Minimal functional Device/Manual UI only.
- Full unit/build/API28 gate before STEP 4 closure.

### Pending

1. Arabic Quran source/packaging.
2. Italian translation inclusion/source.
3. Debug-release trigger convention.
4. Default calculation method for fresh install.
5. Default notification/adhan sound policy and audio licensing.
6. Pre-31 custom splash fallback.
7. Any future need for `ACCESS_FINE_LOCATION`.
8. Any future need for background location/foreground location service.

## 15. Explicitly out of scope unless later approved

General: Play Store, paid services/APIs, user accounts/cloud sync, ads/monetization, third-party tracking analytics, social/community.

Current Location milestone also excludes:

- `POST_NOTIFICATIONS`
- notification channels
- `AlarmManager` / exact alarms
- WorkManager
- adhan audio
- Qibla/sensors
- Quran
- custom alarms
- final Hero Dashboard/Prayer Times timeline
- Google Maps/Places/Geocoding
- Nominatim runtime
- Play Services/FusedLocationProvider
- background location
- foreground location service

## 16. Milestone sequence

1. Branding/UI decision closure — CLOSED.
2. Android bootstrap — CLOSED/build verified.
3. Prayer-time calculation — CLOSED; final commit `e5987f878e253085425f9bfebf7bf714c8405de3`; JDK21/API28 regression passed.
4. **Current: Location (Device + manual city) — architecture APPROVED.**
5. Location sequence: STEP 1 spec commit → STEP 2 pure Kotlin domain/state/policies + fake tests → STEP 3 Preferences DataStore → STEP 4 GeoNames generation/read-only SQLite + APK-size measurement + API28 timezone/data gate → STEP 5 Android LocationManager + permission/resolution → STEP 6 minimal functional UI → STEP 7 full unit/build/API28 regression → dedicated implementation commit → STOP.
6. Notifications, AlarmManager, definitive UI, Qibla, Quran and custom alarms remain separate milestones.

## 17. Change log

### 2026-08-30 — API28 timezone compatibility policy approved after exhaustive gate

Run `33294388128` proved that the runtime-minimal SQLite/search/index implementation is sound but found five modern IANA ids absent from Android 9/API28. Each was reviewed against IANA tzdb and the API28/2018e rule-set rather than blindly renaming. Approved mappings are: `Europe/Kyiv→Europe/Kiev` (official IANA rename/Link); `America/Ciudad_Juarez→America/Ojinaga` (2018e-specific legacy-rule equivalence, not a Link); `America/Coyhaique→America/Punta_Arenas` and `Asia/Qostanay→Asia/Aqtobe` (peer equivalences, not Links). The peer pairs were automatically compared every six hours over 2026-01-01 through 2100-12-31 inclusive in practice via the half-open interval ending 2101-01-01, 109,572 instants per pair, with zero discrepancies. After those mappings, the exhaustive frozen dataset has one residual unsupported id: `America/Nuuk`, 17 cities. `America/Godthab` from tzdata 2018e is not safe because its projected `-03/-02` rules diverge by one hour from modern Nuuk's `-02/-01`. The database must retain all 17 cities, mark baseline support explicitly, keep them searchable/nearest-discoverable, and convert attempted manual selection into `LocationFailure.UNSUPPORTED_TIME_ZONE`. Runtime uses modern id first and verified compatibility only when necessary; no custom tz engine, fixed-offset fallback, longitude inference, or city removal is allowed.

### 2026-08-30 — Runtime-minimal GeoNames schema finalized; APK-size gate passed

Run `33293822757` transformed the exact E6 staging DB from run `33292976302` (SHA-256 `e567b7eabb40994d5d9fb95209c050503cf5747a6cdd25ef73d195aeb4003877`) into the final runtime-minimal layout without removing any city or alias. The final database contains 224,330 cities, 258,685 aliases and 391 timezone names; logical comparison reported zero city/alias mismatches and zero duplicate alias pairs. Table-scoped integrity checks returned `ok` for `country`, `admin1`, `timezone`, `city` and `city_alias`; FTS document parity/orphan checks and golden MATCH cases for Roma, Makkah, Mecca, New York and Sydney all passed. `search` used the FTS virtual table, and `nearest` used the sole explicit secondary runtime index `city_lat_lon_idx`. Final SQLite size is 27,795,456 bytes; AAPT/ZIP method 8 compressed the asset to 15,008,799 bytes; baseline APK was 33,937,009 bytes and APK-with-city-data was 48,945,932 bytes, yielding a **15,008,923-byte incremental APK cost**, 5,962,597 bytes below the 20 MiB stop threshold. Runtime-minimal is therefore the final approved STEP 4 schema: E6 coordinates, numeric timezone lookup, population retained, country/admin1 short codes retained, runtime-only coordinate index, build-time alias dedup and unchanged FTS4 contentless search. Proceed with CityRepository, exhaustive API28 timezone validation and remaining STEP 4 regression.

### 2026-08-30 — Lossless GeoNames schema-footprint optimization approved

After the semantic feature-code filter, run `33280106118` still measured a 27,459,231-byte APK increment for 224,327 cities and 258,681 aliases. Before considering `cities1000` or a higher threshold, optimize the same dataset without removing any city or alias. Country/region display names are already normalized and remain lookup data. Store coordinates as exact integer microdegrees, normalize repeated IANA timezone strings into a numeric lookup, retain population as compact SQLite INTEGER for ranking, and remove preprocessing-only/runtime-unused indexes from the final read-only asset while preserving the coordinate index required by `nearest`. Alias uniqueness must be enforced in temporary build-time staging instead of a bundled uniqueness/index structure. Preserve FTS4 contentless validation and table-scoped integrity checks. Measure SQLite bytes, APK-compressed asset bytes and final APK increment after each significant optimization. The APK already uses DEFLATE for the SQLite asset; separate precompression is not approved unless ordinary APK compression remains insufficient. If the final lossless result is <=20 MiB, continue with CityRepository (`search`/`findById`/`nearest`), API28 timezone gate and remaining STEP 4 regression; otherwise stop again for review.

### 2026-08-30 — GeoNames populated-place feature filter approved

The unfiltered official `cities500` snapshot used in the first STEP 4 size run contained 235,549 rows and increased the APK by about 27.58 MiB. Review confirmed that raw `cities500` contains semantic categories Arihna should not expose as independent manual cities. The generator must now enforce `feature class = P` plus an explicit deny-by-default whitelist: `PPL`, `PPLA`, `PPLA2`, `PPLA3`, `PPLA4`, `PPLA5`, `PPLC`, `PPLG`, `PPLF`, `PPLR`, `STLMT`. Historical/abandoned/destroyed entries, sections of larger populated places, minor localities and aggregate populated-place records remain excluded. Official GeoNames definitions were checked before approval: `PPLF` and `PPLR` are current populated places and `STLMT` is a current settlement, so those three are retained despite not appearing in the initial narrower proposal. Re-measure the generated record count and APK increment before any further STEP 4 work; the 20 MiB stop rule remains unchanged.

### 2026-08-29 — FTS4 contentless validation gate approved

`city_search` remains FTS4 `content=''` with `docid = city_alias.id`. SQLite 3.44+ global `PRAGMA integrity_check` invokes virtual-table `xIntegrity`, but FTS4 cannot validate the inverted index against absent original content in this contentless configuration; this failure is not treated as evidence of corruption. The GeoNames generator must retain table-scoped SQLite integrity checks for non-FTS tables and replace the invalid FTS global gate with functional checks for row-count parity, bidirectional docid coverage, and golden `MATCH` cases for Roma, Makkah/Mecca, New York and Sydney. The 20 MiB APK-growth stop rule remains unchanged.

### 2026-08-29 — Location architecture approved

Before implementation, approved Android framework LocationManager/LocationManagerCompat, no Play Services, foreground-only `ACCESS_COARSE_LOCATION`, 20s timeout, 5 km significant movement, 15-minute minimum foreground interval, timezone changes as significant, fresh/cached real-fix semantics and no invented default location.

Manual city uses offline read-only SQLite generated from GeoNames `cities500` under CC BY 4.0, with canonical/ASCII and selected Italian/English/Arabic aliases and IANA timezone per record. Preferences DataStore `1.2.1` persists Device/Manual choice, manual city snapshot and last real device fix. `PrayerTimeCalculator` stays unchanged.

The generated city database's real APK impact must be measured. If it increases APK size by more than 20 MB, stop for review before proceeding; `cities1000` or explicit acceptance of larger footprint are review options, but no silent dataset switch is allowed.

### 2026-08-29 — Prayer calculation engine closed

Final run `33248406741` passed `testDebugUnitTest`, `assembleDebug`, and `connectedDebugAndroidTest` on Android 9/API28 using host JDK21. Engine promoted to `main` in `e5987f878e253085425f9bfebf7bf714c8405de3`.

### 2026-08-29 — JDK 21 build-host policy approved

All present/future Gradle-based CI uses Temurin JDK21 host while Java/Kotlin app target stays 17. Direct Adhan class inspection showed major 65; run `33247757115` proved JDK21 host works while app target remains 17 unchanged.

### 2026-08-29 — Android API 28 HijrahChronology verification passed

Run `33245235911` executed 2 passing instrumentation tests on Android 9/API28, verifying `Hijrah-umalqura` and known Gregorian dates mapping to Islamic months including Ramadan. The 90/120-minute Umm al-Qura rule was subsequently implemented and regression-tested.

### 2026-08-29 — Prayer calculation architecture approved

Approved Adhan 0.0.7/MIT adapter architecture, Arihna prayer-domain models, 11 methods, Standard/Hanafi, absolute-latitude AUTO rule, controlled polar errors, explicit ZoneId/ZoneRules, prayer offsets, Ramadan rule and comprehensive test plan.

### 2026-08-29 — Bootstrap completed

Created/build-verified the single-module Android/Compose shell. `versionName 0.1.0-bootstrap` remains temporary; pre-31 splash fallback deferred.

### 2026-08-29 — Android bootstrap matrix approved

Approved `com.archimedeprojects.arihna`, minSdk28, compile/target37, AGP9.3.1, Gradle9.5.0, Kotlin/Compose plugin2.4.10, Compose BOM2026.08.00, app target17, built-in Kotlin, Kotlin DSL, version catalog and manual AppContainer.

### 2026-08-29 — Branding and UI direction finalized

Closed palette, Hero Dashboard, Prayer Times timeline, final A/minaret/crescent/adhan-wave icon and Android branding assets.

### 2026-08-29 — Initial specification

Captured native Kotlin/Compose, zero-cost/private-GitHub/sideload distribution, prayer times, Qibla, alarms, offline Quran, optional Italian translation, daily content, permissions/exact alarms/offline behavior, UI/logo gate, GitHub Releases signing and religious-source verification requirements.
