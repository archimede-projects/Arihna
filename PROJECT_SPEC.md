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

### 5.2 Location — MILESTONE CLOSED

Location STEP 4 (GeoNames generation/read-only SQLite, APK-size measurement, CityRepository/timezone/data validation and API28 data gate) is **CLOSED** after definitive workflow run `33296099459` on implementation commit `8620772ebe0dd5b51691ce2447c46ef996cd90d1`. The run passed frozen-dataset provenance, runtime-minimal database generation, real AAPT/APK size gate, host unit regression, and Android 9/API28 instrumentation (**16/16 tests, 0 failed, 0 skipped**). The API28-classified SQLite asset is 28,020,736 bytes with SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`; its real APK increment is 15,033,263 bytes, 5,938,257 bytes below the approved 20 MiB threshold. All 224,330 cities and 258,685 aliases are preserved; the four reviewed timezone mappings and the exact 17-city `America/Nuuk` controlled-unsupported set passed the STEP 4 gate.

The **Location milestone is CLOSED** after STEP 7 final regression on exact pre-closure `main` SHA `b41dd6a4b8a29204a4cb01b0d640a44504139cfc`. Final run `33326121715` passed the complete `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest` gate with **25/25 tests, 0 failed, 0 skipped**. Complementary final data/APK run `33326126008` on the same exact SHA regenerated the frozen GeoNames runtime asset, repeated the full unit and Location+Prayer Android 9/API28 regression with **25/25 tests, 0 failed, 0 skipped**, and remeasured the GeoNames APK increment at **15,033,263 bytes**, 5,938,257 bytes below the approved 20 MiB threshold. STEP 5 remains closed on candidate `7f59c55da954347a1db5c17fe41c2cb07309184c` / run `33317622881`; STEP 6 remains closed on clean candidate `df53f71c07cd3da743604898941f6b4ef39e86aa` / run `33325240888`. No subsequent product milestone is authorized or started by this closure.

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
- STEP 5 provides the Android permission-state/request boundary used by STEP 6. The actual system prompt has no startup side effect: it is invoked only after an explicit tap on `Usa posizione attuale` and after Arihna shows the rationale.

#### STEP 6 minimal functional Device/Manual UI

- STEP 6 is a functional Location panel, **not** the definitive Home/Hero Dashboard. It is attached to the existing bootstrap navigation shell through the `Impostazioni` destination, replacing only that placeholder for this milestone.
- Show the current domain resolution state clearly: `Unconfigured`, `Resolving`, `Ready`, `PermissionDenied`, `LocationServicesDisabled`, and `Unavailable`.
- For `Ready`, show whether the active source is **Device** or **Manual**, the selected/display location name, timezone, and `FRESH`/`CACHED` when applicable. Do not expose exact coordinates as the primary UI.
- `Usa posizione attuale` always opens an Arihna rationale first. The rationale explains that approximate location is enough, location is used locally to calculate prayer times, and manual city selection works without granting the permission. Only the explicit confirmation action may launch Android `RequestPermission(ACCESS_COARSE_LOCATION)`. No permission prompt may originate from app startup, ViewModel initialization, state restoration, or foreground lifecycle callbacks.
- If the permission is already granted, confirming the rationale resolves Device immediately without launching another system prompt. If permission is denied, show an understandable state and preserve the manual-city path; permanent denial may offer a link to Android app settings.
- Manual mode provides local search through the existing `CityRepository`, bounded result rendering, explicit selection, clear active-source feedback, and Device re-selection. Unsupported API28 timezone rows remain discoverable but selecting one must surface the controlled `UNSUPPORTED_TIME_ZONE` message. Empty queries/results never invent a city.
- While the Location panel is foreground and Device is selected, restore/refresh through `LocationCoordinator` and collect `DeviceLocationDataSource.observeSignificantUpdates()`. Stop collection when the panel leaves foreground. Delivered candidates always pass through `LocationCoordinator.acceptDeviceUpdate(...)`; the UI must not duplicate the 5 km/ZoneId acceptance policy.
- UI styling uses the existing Arihna Material theme/palette (green/gold/off-white) with simple cards/controls only. No final dashboard composition, prayer timeline, map, Qibla UI, or other milestone work is included.

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

- The Android callback layer must not reimplement the 5 km acceptance rule. Its update request uses the approved 15-minute minimum interval but must not impose a 5 km provider-level `minDistance`, because doing so could suppress a legitimate `ZoneId` change below 5 km. Every delivered candidate is passed to the existing pure `LocationUpdatePolicy.shouldAccept(...)` decision.
- The 20-second fresh-fix timeout remains owned by `LocationCoordinator`/`LocationUpdatePolicy`; `DeviceLocationDataSource` supplies a cancellable current-location operation and does not maintain a second independent timeout.

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
- Runtime-minimal optimization run `33293822757`, using exactly that same staging DB: 27,795,456-byte SQLite, 15,008,799-byte APK-compressed asset, and 15,008,923-byte APK increment before the final API28 compatibility marker/mapping fields were added.
- Definitive STEP 4 closure run `33296099459` on commit `8620772ebe0dd5b51691ce2447c46ef996cd90d1`: **28,020,736-byte SQLite**, **15,033,136-byte APK-compressed asset**, baseline APK **33,986,205 bytes**, APK with GeoNames **49,019,468 bytes**, and **15,033,263-byte APK increment**. The final database/APK asset SHA-256 is `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb` and the workflow verified the decompressed APK asset matches the generated database exactly.
- Final STEP 7 remeasurement run `33326126008` on exact pre-closure `main` SHA `b41dd6a4b8a29204a4cb01b0d640a44504139cfc`: baseline APK **34,150,268 bytes**, APK with GeoNames **49,183,531 bytes**, **15,033,136-byte APK-compressed asset**, **28,020,736-byte SQLite**, and unchanged **15,033,263-byte APK increment**. Threshold remains 20,971,520 bytes; final margin is **5,938,257 bytes** and `threshold_pass = true`. Database SHA-256 remains `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`; 224,330 cities, 258,685 aliases and 391 timezone rows remain preserved.
- APK asset compression remains ZIP method 8 (DEFLATE).
- Approved threshold: 20,971,520 bytes (20 MiB). Definitive margin below threshold: **5,938,257 bytes** (about **5.66 MiB**). `threshold_pass = true`.
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

### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 5 CLOSED

This milestone connects the already-closed Prayer Engine and Location milestones without reopening or duplicating their internal logic. Location remains the sole authority that produces a valid `SelectedLocation`; the Prayer Engine remains the sole authority for prayer-time calculation from explicit `Coordinates + ZoneId + PrayerCalculationSettings + LocalDate`. The integration layer derives a UI-consumable schedule only when Location is `Ready` and never invents coordinates, timezone, calculation results or fallback prayer times.

#### Orchestration boundary

Introduce an Arihna-owned integration boundary:

```text
interface PrayerScheduleRepository {
    fun observeSchedule(): Flow<PrayerScheduleState>
    suspend fun refresh()
}
```

`PrayerScheduleRepository` observes `LocationResolutionState`, `PrayerCalculationSettings`, and a testable injected `Clock`. When Location is `Ready`, it extracts the exact `SelectedLocation.coordinates` and `SelectedLocation.zoneId`, determines the civil `LocalDate` in that selected `ZoneId`, and invokes the existing `PrayerTimeCalculator`. It must not call `ZoneId.systemDefault()` for a remote/manual city, reinterpret Location acceptance policy, inspect raw provider callbacks, or contain Adhan-specific calculation logic.

`refresh()` is an explicit/idempotent orchestration command for bootstrap/restore, foreground date re-check, tests and a future manual refresh action. It must not create polling or continuous recalculation. The implementation should derive a stable calculation key such as:

```text
PrayerScheduleInput
- coordinates
- zoneId
- PrayerCalculationSettings
- localDate
```

and deduplicate identical inputs. Flow composition should use cancellation-aware `mapLatest`/equivalent semantics so a stale calculation for an earlier selection (for example Device/Roma) cannot arrive after a newer Manual/Milano selection and overwrite current UI state.

Conceptual output models:

```text
PrayerScheduleState
- Loading
- NoLocation(locationState/reason)
- Ready(PrayerSchedule)
- CalculationUnavailable(reason, selectedLocation)

PrayerSchedule
- localDate
- selectedLocation
- settings
- today: PrayerDay
- nextPrayer: NextPrayer?
- generatedAt

NextPrayer
- prayer
- time
```

Reuse the existing closed Prayer Engine domain types (`PrayerDay`, prayer identifiers and `PrayerCalculationResult`) where they already express the same concept; do not create a parallel prayer-calculation domain.

#### Today, tomorrow and next prayer

For each distinct valid input, calculate the selected location's current local day and, when needed for next-prayer continuity, the following local day. The UI displays the current day's complete schedule. The following day exists only to determine the next prayer after the last prayer of today; no arbitrary multi-day window and no previous-day calculation are required.

Next-prayer comparison uses `Instant`/`ZonedDateTime` semantics in the selected `ZoneId`, never formatted strings or naive local-time comparison. Before the final prayer of today, choose the first future prayer today. After the final prayer, use the first prayer of tomorrow when tomorrow is available. If today's calculation is valid but tomorrow is unavailable, keep today's valid schedule and surface `nextPrayer = null`/controlled next-prayer-unavailable presentation after the last prayer instead of discarding valid times or fabricating tomorrow.

#### Prayer calculation settings and persistence

Introduce a separate settings boundary while reusing the same AndroidX Preferences DataStore already approved for Location:

```text
interface PrayerSettingsRepository {
    val settings: Flow<PrayerCalculationSettings>
    suspend fun get(): PrayerCalculationSettings
    suspend fun update(settings: PrayerCalculationSettings)
}
```

Use separate Prayer-prefixed keys in the existing DataStore; do not introduce SharedPreferences, Proto DataStore, Room, or a second persistence system. Persist method, Asr convention, high-latitude rule and per-prayer integer-minute offsets.

Until a dedicated Prayer settings UI exists, the canonical initial settings are:

- calculation method: **Muslim World League (MWL)**;
- Asr: **STANDARD**;
- high-latitude rule: **AUTOMATIC** (the already-closed Arihna policy: `abs(latitude) > 48° → SEVENTH_OF_THE_NIGHT`, otherwise `MIDDLE_OF_THE_NIGHT`);
- manual offsets: **0 minutes for every prayer**.

If no Prayer settings exist, materialize/persist this complete canonical default so a later settings screen edits the same stored value rather than replacing an implicit runtime fallback. A complete valid persisted set is restored unchanged. Partial/malformed Prayer settings must not create arbitrary mixed values; recover to the documented canonical default in a controlled, test-covered way.

**Known temporary product limitation:** MWL/Standard/Automatic is a deliberately generic bootstrap default, not a claim that MWL matches every local religious convention. An app user in Saudi Arabia, or in any region/community whose established convention differs from MWL/Standard, may see prayer times that differ slightly from local practice until Arihna provides the separately scoped settings UI for choosing calculation method, Asr convention, high-latitude rule and offsets. This is an accepted temporary limitation of this integration milestone, not a Prayer Engine defect, and must remain tracked until the configurable settings UI exists. Arihna must not silently auto-select Umm al-Qura or another method from country/city/location.

#### STEP 2 closure evidence

STEP 2 was closed only after a clean technical candidate was rebuilt directly above the STEP 2 spec-first commit and passed a new gate on its exact SHA. Definitive technical commit `ffb391846481a38cca0786be238bddae912ba862` passed workflow run `33338723836` / job `99330268698`: `testDebugUnitTest` passed, `assembleDebug` passed, and Android 9/API28 `connectedDebugAndroidTest` started and finished **35/35 tests with 0 failed and 0 skipped**. The workflow also restored and verified the frozen GeoNames asset and preserved the existing COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy checks.

The real persistence instrumentation uses `PreferenceDataStoreFactory` and passes the **same `DataStore<Preferences>` instance** to the Prayer and existing Location repositories. It verifies canonical first-use materialization, restart persistence, custom settings, positive/negative/zero offsets, dedicated recovery cases for partial records, unknown enum values and wrong stored types, and explicit isolation in both directions: Prayer reads/writes preserve existing `location.*` entries, and Location reads/writes preserve existing `prayer.*` entries. Recovery rewrites only the complete Prayer record and never calls a DataStore-wide clear.

Diagnostic history is retained for traceability but is not definitive gate evidence: the first attempt of run `33337223313` timed out in ddmlib while installing the APK and executed **0 tests**; its rerun then exposed only a JUnit test-harness signature error (`@After tearDown()` inferred a non-void return). That teardown was corrected without changing production code, and corrected development run `33338267865` passed **35/35 tests, 0 failed and 0 skipped** before the clean exact-SHA candidate received the independent definitive gate above.

No STEP 2 production change touched `PrayerTimeCalculator`, Location behavior, existing Location keys, or UI. The documented MWL/STANDARD/AUTOMATIC bootstrap-default limitation remains tracked until the separately scoped Prayer settings UI exists. STEP 3 is **NOT STARTED**.

#### STEP 3 authorization / scope

STEP 3 schedule orchestration is explicitly authorized after STEP 2 closure. Implement `PrayerScheduleRepository.observeSchedule(): Flow<PrayerScheduleState>` plus idempotent `refresh()` exactly within the already-approved orchestration contract: consume closed `LocationResolutionState`, persisted `PrayerCalculationSettings` and an injected testable `Clock`; derive `PrayerScheduleInput(coordinates, zoneId, settings, localDate)` from only `LocationResolutionState.Ready`; deduplicate identical inputs; use cancellation-aware latest-input semantics (`mapLatest`/equivalent) so stale calculations cannot overwrite newer selections; calculate today and only the following day when needed for next-prayer continuity; use selected-zone `Instant`/`ZonedDateTime` semantics and DST-aware next-local-midnight boundaries; and recalculate only for the inputs/events already enumerated in the Recalculation policy. A same-input refresh may reuse in-memory calculated days and re-derive next-prayer state without invoking the calculator.

STEP 3 tests must be pure/JVM orchestration contract tests using fake Location state and a fake/recording `PrayerTimeCalculator`; no real GPS, `LocationManager`, Android geocoder, Adhan internals or prayer-formula retesting. Cover the complete STEP 1 matrix, including all non-Ready states → zero calculator calls, exact Manual/Device coordinate+ZoneId forwarding, Device/Manual/input/settings/date changes, identical-input dedup, raw non-accepted update non-effect, bootstrap once, selected-zone midnight including DST and replacement after ZoneId change, before-first/after-last next-prayer behavior, controlled `CalculationUnavailable`, today-valid/tomorrow-unavailable preservation, and stale Roma→Milano cancellation/race protection.

No UI, `PrayerScheduleViewModel`, countdown ticker, Home panel, `PrayerTimeCalculator` implementation/formula change, Location behavior/key change, or Prayer Settings behavior/key change is authorized in STEP 3. STEP 3 is **CLOSED** on exact clean candidate `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive run `33357287019` / job `99381759479`. STEP 4 presentation/countdown was separately authorized afterward and is now **CLOSED** under the scope and evidence below.

#### STEP 4 authorization / scope

STEP 4 presentation/countdown is explicitly authorized after STEP 3 closure. Implement `PrayerScheduleViewModel` as a presentation-only boundary depending exclusively on `PrayerScheduleRepository`, an injected testable `Clock`, and a testable ticker abstraction. It exposes `StateFlow<PrayerScheduleUiState>` and must not depend directly on `LocationManager`, DataStore, `CityRepository`, Adhan, provider callbacks, or `PrayerTimeCalculator`.

The production ticker cadence is **1 second**. Each tick only recomputes the remaining duration as `max(Duration.ZERO, targetInstant - Clock.instant())` for the currently exposed `NextPrayer`; ordinary ticks must not call `PrayerScheduleRepository.refresh()` or otherwise trigger Location/Prayer calculation. When an active countdown first reaches zero, the ViewModel may call the repository's idempotent `refresh()` exactly to let STEP 3 re-derive `nextPrayer` from its already-calculated today/tomorrow in-memory data. This does not authorize a new calculation: STEP 3 remains responsible for recalculating only if its effective input/local civil date actually changed. Repeated zero ticks must be guarded so they do not spam `refresh()` while awaiting a new repository state.

`PrayerScheduleState` maps to presentation state without inventing values: Loading remains loading; `NoLocation` produces a clear location-required presentation message; `Ready` exposes location/source context, current-day schedule, calculation method, next prayer/time and countdown data for future STEP 5 rendering; `CalculationUnavailable` remains a controlled unavailable presentation. Presentation models may retain domain values needed by the future Compose layer, but no Compose UI is authorized in STEP 4.

Pure/JVM tests use a fake/recording `PrayerScheduleRepository`, controlled `Clock`, and manual/test ticker. Required coverage includes NoLocation mapping/message, Ready next-prayer/countdown mapping, countdown decrement with no repository calls, exactly-once refresh when countdown reaches zero, transition to the next prayer after the fake repository emits the advanced cached state, `CalculationUnavailable`, and proof that ticker activity alone never causes spurious refresh/calculation behavior. No change to `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or Home Compose UI is authorized. STEP 5 remains **NOT STARTED**.

#### STEP 4 closure evidence

STEP 4 presentation/countdown is **CLOSED** on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4`, built directly above the STEP 4 spec-first commit `fa16c35330c3540671eee681b352ea22df0c5d48` as one commit containing only `PrayerSchedulePresentation.kt`, `PrayerScheduleTicker.kt`, `PrayerScheduleViewModel.kt`, and `PrayerScheduleViewModelTest.kt`. Definitive exact-SHA workflow run `33360819887` / job `99391679780` checked out `5019906d0ef83a33d09f5a3295d736422c8af7a4`, restored and verified the frozen GeoNames asset, preserved the COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy checks, passed unfiltered `testDebugUnitTest` and `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **35/35 tests, 0 failed and 0 skipped**. The presentation boundary depends only on `PrayerScheduleRepository`, injected `Clock`, and a testable ticker; the production ticker cadence is one second, ordinary ticks only update `targetInstant - Clock.instant()` and never call the repository, and an expired target is guarded so `refresh()` is requested once while awaiting an advanced repository state. The focused pure/JVM ViewModel suite covers NoLocation mapping/message, Ready next-prayer/countdown, countdown decrement without refresh, exactly-once refresh at zero followed by the next repository-provided prayer, `CalculationUnavailable`, and no refresh when no next prayer exists. No Compose UI and no modification to `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or `AppContainer` wiring was introduced in STEP 4. STEP 5 was separately authorized afterward and is now **CLOSED** under the evidence below; STEP 6 remains **NOT STARTED**.

#### STEP 5 closure evidence

STEP 5 functional Home Prayer Schedule panel is **CLOSED** on clean technical commit `8b6a9c56a85e791691c41aa3d775788026b76bf0`, built directly above the STEP 4 docs-closed `main` commit `faebc922f04fe91576fe7987c593d1bdd1b46e15`. The final candidate changes only `ArihnaApp.kt`, `ArihnaNavHost.kt`, the new `HomePrayerScheduleScreen.kt`, and `HomePrayerScheduleScreenAndroidTest.kt`; no `PrayerScheduleViewModel`, `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or `AppContainer` source was changed.

Definitive exact-SHA workflow run `33362751963` / job `99397193018` checked out `8b6a9c56a85e791691c41aa3d775788026b76bf0`, restored and verified the frozen GeoNames asset, preserved the `ACCESS_COARSE_LOCATION`-only / no-FINE / no-BACKGROUND / no-Play-Services policy checks, passed unfiltered `testDebugUnitTest`, passed `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **39/39 tests, 0 failed and 0 skipped**.

The bootstrap Home now consumes the already-closed `PrayerScheduleViewModel` through composition-root wiring in `ArihnaApp`/`ArihnaNavHost` and renders only the approved functional panel scope: readable active location/source context, next prayer name/time, live countdown, the complete current-day prayer schedule, and calculation method as secondary non-editable information. `NoLocation` shows an explicit location-required message plus a CTA to the existing Location panel in `Impostazioni`; `CalculationUnavailable` and `Loading` never expose fabricated prayer values or a misleading `--:--` placeholder. The four new Compose instrumentation cases cover NoLocation + CTA + absence of prayer values, Ready location/next prayer/countdown/full-day rendering, controlled calculation-unavailable UI, and Loading with no prayer values.

Diagnostic history is retained for traceability but is not definitive gate evidence: the first clean sibling candidate `db401c9022110e6f80eb7ebaec5c354ea16e42ad` passed host unit tests and `assembleDebug` in run `33362170081`, then failed before instrumentation execution because the new test source imported two Compose-test APIs unavailable to the resolved test surface (`assertDoesNotExist` and `onNode`). Production code was unchanged; only the test assertions were corrected, and the final candidate was rebuilt again directly above `faebc922f04fe91576fe7987c593d1bdd1b46e15` before receiving the independent 39/39 exact-SHA gate above.

The clean technical commit was promoted to `main` by non-forced fast-forward only after the exact-SHA gate passed. **STEP 6 full Prayer + Location + Integration regression remains NOT STARTED and requires separate authorization; no STEP 6 work is begun by this STEP 5 closure.**

#### Recalculation policy

Recalculate only when a mathematical input changes:

1. Location first becomes `Ready` with a valid `SelectedLocation`;
2. `SelectedLocation` changes (Device↔Manual, new manual city, accepted significant Device fix, or accepted ZoneId change); Location remains the sole owner of the 5 km/ZoneId significance decision;
3. `PrayerCalculationSettings` changes;
4. the civil day changes at the next local midnight in the selected `ZoneId`;
5. app bootstrap/restore resolves a valid `Ready` location plus settings;
6. explicit `refresh()` detects changed input/date.

Do not recalculate for every countdown tick, every raw provider callback, or repeated identical `SelectedLocation`/settings emissions. A same-input `refresh()` may reuse the in-memory `PrayerDay` result and only re-derive next-prayer state.

Local midnight scheduling must be timezone/DST aware: compute the next `LocalDate.atStartOfDay(selectedZoneId)` and wait until that `Instant`; never assume a local day is exactly 24 hours. If the selected `ZoneId` changes, cancel the previous midnight timer and calculate the new boundary. Countdown updates are presentation/ViewModel arithmetic (`targetInstant - Clock.instant()`), may tick while the Home panel is visible, and must not invoke Location or the Prayer Engine.

Do not persist `PrayerDay`/calculated schedules in DataStore in this milestone. The Prayer Engine is local and cheap; persist only the real inputs (Location is already persisted by its closed milestone; Prayer settings are persisted here) and recalculate after process restart. A small in-memory cache keyed by `PrayerScheduleInput` is allowed to avoid duplicate same-session calculation.

#### End-to-end error discipline

Only `LocationResolutionState.Ready` authorizes schedule calculation. `Unconfigured`, `Resolving`, `PermissionDenied`, `LocationServicesDisabled`, and `Unavailable` do not produce a Prayer schedule; map them to a clear `PrayerScheduleState.NoLocation`/loading presentation as appropriate. The integration layer must not reinterpret a Location error or manufacture a substitute `SelectedLocation`.

If the Prayer Engine returns `PrayerCalculationResult.Unavailable` for the selected location/date/settings (including controlled polar/extreme cases), expose `PrayerScheduleState.CalculationUnavailable`. Do not substitute another city, another calculation method, yesterday's times, fixed offsets, interpolated values, or fabricated prayer times. A valid today's schedule remains valid if only tomorrow's calculation is unavailable; after today's last prayer, show that the next prayer is unavailable rather than inventing one.

#### Functional Home UI scope

Do **not** implement the definitive Hero Dashboard in this milestone. Keep the closed Location configuration panel in `Impostazioni` and add a separate minimal functional Prayer Schedule panel to the existing bootstrap Home destination. Prayer times belong on Home; Location remains where the user configures Device/Manual source.

When `PrayerScheduleState.Ready`, Home shows at minimum:

- readable active location/source context without exposing coordinates as primary UI;
- next prayer name;
- next prayer time;
- live countdown derived without Prayer recalculation;
- complete current-day prayer times;
- current calculation method as secondary informational text (not editable here).

When Location is not Ready, Home clearly states that a position is required before prayer times can be calculated and offers navigation to the existing Location configuration path. When calculation is unavailable, Home shows a controlled explanation and no fabricated times. Loading should be simple and local; no final-dashboard composition, prayer timeline design, shimmer system or broader visual milestone is authorized here.

Introduce a presentation boundary such as `PrayerScheduleViewModel` that consumes only `PrayerScheduleRepository` plus a testable clock/ticker abstraction and exposes a `StateFlow<PrayerScheduleUiState>`. It must not depend directly on `LocationManager`, DataStore, CityRepository, Adhan, or provider callbacks. When a countdown reaches zero, the presentation layer may ask the repository to refresh/advance next-prayer state from already calculated days; this is not a full prayer-time recalculation unless an input/date actually changed.

#### Package/DI direction

Keep the single `:app` module and manual `AppContainer`; no Hilt/Koin and no new Gradle module. Add an integration-oriented feature/package (name may follow the existing package conventions) containing domain/orchestration, settings persistence, presentation and functional UI boundaries. Conceptually:

```text
closed Location state ─┐
                       ├─> PrayerScheduleRepository ─> PrayerScheduleViewModel ─> Home panel
PrayerSettingsRepository┤
closed PrayerTimeCalculator ┘
```

`AppContainer` wires `PrayerSettingsRepository` and `PrayerScheduleRepository` to the existing closed Location and Prayer boundaries.

#### Test plan for this milestone

The central contract test uses fake Location state and a fake/recording `PrayerTimeCalculator`; it verifies the integration boundary without reopening internal algorithms of either closed milestone.

Pure/JVM orchestration coverage must include at least:

- `Unconfigured`, `PermissionDenied`, `LocationServicesDisabled` and `Unavailable` never invoke the calculator and expose no schedule;
- Ready Manual passes the **exact** selected city coordinates and `ZoneId` to `PrayerTimeCalculator`;
- Ready Device passes the **exact** accepted fix coordinates and captured `ZoneId`;
- Device→Manual, Manual city A→B, accepted significant Device fix and accepted ZoneId change produce new calculation inputs;
- repeated identical `SelectedLocation` does not recalculate;
- a raw/non-accepted Location update that never becomes a new `SelectedLocation` cannot trigger Prayer recalculation;
- settings change recalculates; identical settings do not;
- bootstrap with restored Ready Location calculates once;
- selected-zone local midnight changes the calculation date exactly once and does not assume a 24-hour day;
- selected ZoneId change cancels/replaces the prior midnight boundary;
- before the first prayer, next prayer is today's first prayer; after today's last prayer, next prayer uses tomorrow when available;
- `PrayerCalculationResult.Unavailable` becomes controlled `CalculationUnavailable` with no fallback;
- today valid/tomorrow unavailable keeps today's schedule and does not fabricate next prayer;
- cancellation-aware latest-input behavior prevents stale Roma/Device output from overwriting a newer Milano/Manual result.

Prayer settings DataStore tests must cover canonical first-run materialization, restart round-trip, custom settings and every offset, malformed/partial recovery to the canonical default, and proof that Prayer-prefixed keys do not alter existing Location keys.

Presentation/ViewModel tests cover state mapping, next-prayer/countdown behavior without calculator calls, and controlled errors. Minimal Compose instrumentation covers Home without Location + navigation CTA, Home Ready with location/next prayer/time/countdown/full day, calculation-unavailable UI, and absence of invented times. The existing Location panel remains regression-tested rather than reimplemented.

Final milestone regression must run unfiltered `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest`, preserving Prayer Engine and Location regressions with zero skipped tests. Recheck COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy and frozen GeoNames asset integrity; if practical, reuse the real GeoNames APK-size gate as a final non-regression check even though this milestone does not change the city database.

#### Seven-step implementation sequence

1. **STEP 1 — spec-first architecture: CLOSED by this documentation step.** Define orchestration, exact repository interfaces, settings/default policy, recalculation, error discipline, Home scope and tests before code.
2. **STEP 2 — Prayer settings persistence: CLOSED.** `PrayerSettingsRepository` is implemented on the exact same existing Preferences DataStore instance used by Location (the current `name = "location"` store remains unchanged), with Prayer-only keys `prayer.method`, `prayer.asr`, `prayer.high_latitude_rule`, and `prayer.offset.*`. First use materializes the complete canonical MWL/STANDARD/AUTOMATIC/zero-offset record; partial or malformed Prayer data is recovered atomically to that same complete canonical record without clearing or rewriting Location entries. Focused JVM coverage and real Android 9/API28 shared-DataStore instrumentation passed on exact clean technical commit `ffb391846481a38cca0786be238bddae912ba862` in definitive run `33338723836` / job `99330268698`, including **35/35 instrumentation tests, 0 failed and 0 skipped**. No UI, `PrayerTimeCalculator`, Location behavior, or existing Location keys were changed.
3. **STEP 3 — schedule orchestration: CLOSED.** Clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f`, built directly above spec-first commit `bb972d71c42f86e88f18d05144c61b91dcfa1dd3`, contains only `PrayerScheduleModels.kt`, `PrayerScheduleRepository.kt`, `DefaultPrayerScheduleRepository.kt`, and `DefaultPrayerScheduleRepositoryTest.kt`. Definitive exact-SHA run `33357287019` / job `99381759479` passed the complete host unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35 tests, 0 failed, 0 skipped**) while preserving the frozen GeoNames asset/policy checks. The pure JVM orchestration suite covers the approved 80-case matrix, including exact Device/Manual coordinate+ZoneId forwarding, input dedup, selected-zone midnight/DST, today/tomorrow continuity, controlled unavailable states, and explicit monotonic-generation protection against stale Roma→Milano results from a synchronous non-cooperative calculator. No UI, presentation/countdown, Prayer Engine, Location, or Prayer Settings behavior changed. STEP 4 was separately authorized afterward and is now **CLOSED** below.
4. **STEP 4 — presentation/countdown: CLOSED.** Clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4`, built directly above spec-first commit `fa16c35330c3540671eee681b352ea22df0c5d48`, adds only the pure presentation state, testable one-second ticker, `PrayerScheduleViewModel`, and focused JVM ViewModel tests. Definitive exact-SHA run `33360819887` / job `99391679780` passed unfiltered `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 instrumentation (**35/35 tests, 0 failed, 0 skipped**) with policy/GeoNames gates intact. Countdown ticks never call the repository before expiry; each expired target is guarded to request one idempotent `refresh()` while waiting for the repository to expose the next prayer. No Compose UI or changes to PrayerScheduleRepository, Prayer Settings, Location, Prayer Engine, or AppContainer wiring were made. STEP 5 remains **NOT STARTED** and requires separate authorization.
5. **STEP 5 — functional Home panel: CLOSED.** Clean technical commit `8b6a9c56a85e791691c41aa3d775788026b76bf0`; definitive exact-SHA run `33362751963` / job `99397193018` passed `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 instrumentation with **39/39 tests, 0 failed and 0 skipped**. The bootstrap Home renders the approved minimal Prayer Schedule panel and preserves controlled no-location/unavailable/loading states without fabricated values.
6. **STEP 6 — full regression gate: NOT STARTED.** Run Prayer + Location + Integration unit/build/API28 regressions plus permission/asset checks and final APK/data non-regression where practical.
7. **STEP 7 — documentation-only milestone closure: NOT STARTED.** After an exact tested technical SHA is green/promoted, update the specification with definitive evidence and stop.

STEP 2 Prayer settings persistence, STEP 3 schedule orchestration, STEP 4 presentation/countdown, and STEP 5 functional Home panel are **CLOSED** after their independent exact-SHA gates and promotion. STEP 6 full Prayer + Location + Integration regression remains **NOT STARTED / NEXT** and requires separate explicit authorization. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.

### 5.4 Qibla

- Determine Qibla bearing from current coordinates to the Kaaba.
- Use Android sensors for live compass direction and surface calibration/accuracy gracefully.
- Remain calculable offline once coordinates are available.

### 5.5 Alarms

- Custom alarms in addition to prayer-linked alarms.
- Configurable sounds.
- Prayer/custom alarm reliability must respect Android standby/doze restrictions.

### 5.6 Quran

- Arabic Quran text readable offline with surah navigation.
- Evaluate optional Italian translation for redistribution license, attribution, integrity, size and offline packaging.
- **Pending:** bundled vs optional pack vs omitted in v1.
- Religious text must come from a verified attributable source and never be silently altered.

### 5.7 Daily motivational content

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
- Prayer schedule integration: existing `PrayerTimeCalculator` + closed Location state, new `PrayerScheduleRepository`, and Prayer settings stored in the existing Preferences DataStore under separate keys; no new calculation engine or persistence library.

WorkManager, alarms/notifications, Quran data libraries and Google/Fused location providers remain deferred/not approved here.

### 8.3 Data concepts

- `PrayerCalculationSettings`, `PrayerDay`.
- `LocationPreference`, `LocationSource`.
- `ManualCity`, `CitySearchResult`, `ManualCitySnapshot`.
- `DeviceLocationFix`, `SelectedLocation`.
- `LocationFreshness`, `LocationPermissionState`, `LocationResolutionState`, `LocationFailure`.
- Integration: `PrayerScheduleInput`, `PrayerScheduleState`, `PrayerSchedule`, `NextPrayer`; reuse existing `PrayerCalculationSettings`, `PrayerDay`, `PrayerCalculationResult` rather than duplicating Prayer domain.
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

Single-module Android project with `app/src/main`, `app/src/test`, `app/src/androidTest`, version catalog and Gradle wrapper. Location remains inside `:app` using domain/data/platform/UI boundaries. The Prayer+Location integration also remains inside `:app` in a dedicated integration/feature package with orchestration, settings persistence, presentation and functional Home UI boundaries; do not create a Gradle module only for this milestone.

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

### Prayer Engine + Location integration tests — CURRENT MILESTONE

Do not reopen internal Prayer Engine or Location algorithms. Test their new contract through fakes/recording boundaries. Required integration coverage includes exact `SelectedLocation.coordinates + zoneId` forwarding, no calculator call for non-Ready Location states, deduplicated identical inputs, recalculation on accepted Location/settings/date changes only, timezone/DST-aware local-midnight rollover, cancellation of stale calculations, today/tomorrow next-prayer behavior, and controlled `PrayerCalculationResult.Unavailable` propagation with no fabricated fallback. Prayer settings DataStore tests cover canonical MWL/Standard/Automatic/zero-offset initialization and persistence, malformed/partial recovery, custom settings/offset round-trip and isolation from Location keys. Presentation/Compose tests cover no-location, Ready daily schedule/next prayer/countdown and unavailable states without retesting raw GPS or Adhan formulas.

Final gate for this milestone must preserve the complete existing Prayer and Location regression suites and run `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest` with zero skipped tests, plus manifest/dependency policy and GeoNames asset checks.

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

### Location STEP 5 Android gate — REQUIRED BEFORE COMMIT/PROMOTION

STEP 5 must preserve the existing host/domain regression and add Android 9/API28 instrumentation for the real `LocationManager` bridge. The gate covers the strongest behavior that the API28 emulator can demonstrate reliably: framework/AndroidX request registration, cancellation and listener/request removal; foreground update registration under the approved 15-minute policy; provider-disabled/unavailable behavior without fabricated coordinates; manifest permission policy (`COARSE` only); and integration with the existing cached/permission/services-disabled coordinator states. Current-fix mapping, captured device `ZoneId`, and real COARSE-only end-to-end delivery are additionally verified on physical hardware rather than inferred from an emulator mock path that is known not to reproduce Android's app-facing coarse-location delivery semantics.

#### Known API28 emulator/test-provider limitation for STEP 5

Diagnostic run `33303926016` on commit `411f8c17af50dc690f75a134559bf9375707d8bc` isolated the failing test boundary. The API28 emulator accepted the dynamic mock provider and mock-location app-op, `Location.isComplete` was true, `setTestProviderLocation(...)` returned normally, and `dumpsys location` stored the injected fix in both the provider state and coarse last-known state. Nevertheless, app-facing `getLastKnownLocation(...)`, `LocationManagerCompat.getCurrentLocation(...)`, and registered COARSE listener callbacks remained null even after fresh post-registration injection. The dump also showed an empty extras bundle for the injected mock. On this Android 9 framework path the coarse-location fudging/filtering logic relies on the no-GPS location payload (`Location.EXTRA_NO_GPS_LOCATION`); `setTestProviderLocation(...)` does not reliably synthesize that production-provider metadata for the test location. This is therefore classified as a **known API28 emulator/test-harness limitation**, not as evidence that the production `LocationManager` bridge fails.

The two STEP 5 bridge instrumentation cases that previously attempted to require delivery of a synthetic COARSE fix must **not** be hidden with `@Ignore` or accepted skips. They are rewritten to assert the automatically demonstrable lifecycle boundary: the request/listener is registered through the real framework/AndroidX path and cancellation removes it. End-to-end real-fix delivery is covered by physical-device evidence below. CI must still report zero skipped tests for the final STEP 5 gate.

Physical-device verification on 2026-08-30 used `arihna-step5-device-test-s25.apk` from commit `ce23a7f78695be95c7f8dfd2bcedbe22544da94c` on a Samsung Galaxy S25 with location permission granted only as approximate / `ACCESS_COARSE_LOCATION` while using the app. The unmodified production `LocationManagerDeviceLocationDataSource.getCurrentLocation()` returned `SUCCESS`, a plausible locally corresponding coarse fix with `accuracyMeters=2000.0`, captured `ZoneId=Europe/Rome`, and timestamp `2026-08-30T10:44:05.566Z`, with no timeout or error. Exact user coordinates are intentionally not recorded in this specification. This is the authoritative end-to-end proof for the user-facing current-fix path.

The Android 11/API30 cross-API experiment is **inconclusive infrastructure evidence**, not proof for or against API-level specificity of the API28 limitation. Runs `33306729251`, `33308988692`, and final run `33309369262` never reached instrumentation because of emulator/framework-readiness and shell-script execution issues; final run `33309369262` produced no `ARIHNA_STEP5_CROSS_API` payload. No claim may be made that the limitation is exclusive to API28 based on those runs.

The temporary `STEP5 Device Test` Compose hook and its release remain isolated on `location-step5-device-test` as a diagnostic/reproduction artifact. They must not be promoted into the official `location-step5` implementation candidate and do not constitute STEP 6 UI.

Required STEP 5 commands include:

```bash
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
./gradlew :app:connectedDebugAndroidTest --stacktrace
```

The tested implementation SHA is promoted/committed as the STEP 5 implementation only after these gates are green. STEP 6 must not start before that closure.

### Location STEP 6 functional UI gate — REQUIRED BEFORE STEP 6 CLOSURE

STEP 6 must keep STEP 5 production behavior unchanged and add focused UI/presentation verification. The gate requires:

1. pure/JVM tests for presentation/controller behavior with fakes where Android framework state is not required: restore mapping, Device selection, manual search/selection, unsupported timezone, timeout/unavailable messages, and source/freshness presentation;
2. Compose instrumentation tests that render the functional panel from injected/fake UI state and verify at least Unconfigured, Resolving, Ready Device, Ready Manual, PermissionDenied, LocationServicesDisabled, generic/timeout Unavailable, rationale visibility, city search results and unsupported-timezone messaging;
3. an explicit test that initial composition/restore cannot invoke the Android permission launcher, and that the launcher callback is requested only after the user taps `Usa posizione attuale` and confirms the rationale;
4. existing `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest` regressions remain green with zero skipped tests;
5. manifest policy remains COARSE-only with no FINE/BACKGROUND permission, no Play Services Location and no foreground location service.

The API28 synthetic-COARSE delivery limitation documented for STEP 5 remains unchanged; STEP 6 UI tests must use injected/fake state rather than fabricate a claim that the emulator can deliver a realistic coarse fix.

Required STEP 6 commands include:

```bash
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
./gradlew :app:connectedDebugAndroidTest --stacktrace
```

STEP 6 was closed only after an exact candidate SHA passed this gate and was promoted to the official STEP 6 branch. STEP 7 began only after that closure and has now passed the final gate below.

### Location STEP 7 final milestone regression — PASSED / MILESTONE CLOSED

STEP 7 revalidated the complete accumulated Location milestone and the prior Prayer Engine on exact pre-closure `main` SHA `b41dd6a4b8a29204a4cb01b0d640a44504139cfc`; no production-code commit was inserted merely to run the gate.

- Final full gate run `33326121715`: toolchain/permission policy and exact STEP 4 asset restore passed; `testDebugUnitTest` passed; `assembleDebug` passed; Android 9/API28 `connectedDebugAndroidTest` started and finished **25/25 tests with 0 failed and 0 skipped**.
- Complementary final GeoNames/data gate run `33326126008` on the same SHA regenerated and validated the runtime-minimal city database, reran the full host unit suite, and passed the workflow step explicitly covering **Location and Prayer regression on Android 9/API28**, again **25/25 tests with 0 failed and 0 skipped**.
- The unfiltered final suites include the accumulated Location boundaries from STEP 2 domain/coordinator/policies, STEP 3 Preferences DataStore, STEP 4 SQLite/CityRepository/timezone/data, STEP 5 `LocationManager` platform integration, and STEP 6 presentation/ViewModel/Compose UI, plus the existing Prayer regression including API28 HijrahChronology coverage.
- Final manifest/dependency policy remains `ACCESS_COARSE_LOCATION` only: no `ACCESS_FINE_LOCATION`, no `ACCESS_BACKGROUND_LOCATION`, no Play Services Location dependency and no foreground location service.
- Prayer Engine production subtree remains unchanged from final prayer implementation commit `e5987f878e253085425f9bfebf7bf714c8405de3`; the STEP 7 full unit/API28 gates passed, so the Location milestone introduced no Prayer regression.
- Final real APK-size measurement: baseline APK 34,150,268 bytes; APK with GeoNames 49,183,531 bytes; runtime SQLite 28,020,736 bytes; compressed APK asset 15,033,136 bytes; incremental cost **15,033,263 bytes** against the 20,971,520-byte threshold, leaving **5,938,257 bytes** margin. The city database SHA-256 is unchanged at `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`.

The Location milestone is therefore **CLOSED**. This documentation closure does not authorize or begin Qibla, notifications/alarms, Quran, definitive dashboard UI, or any other later milestone.

### Location STEP 4 gate — PASSED

Host JDK: Temurin 21. Required commands for the STEP 4 regression were:

```bash
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
./gradlew :app:connectedDebugAndroidTest --stacktrace
```

Definitive run `33296099459` on commit `8620772ebe0dd5b51691ce2447c46ef996cd90d1` passed the complete **STEP 4** gate. `testDebugUnitTest` and both APK builds were successful; Android 9/API28 `connectedDebugAndroidTest` finished **16/16 tests with 0 failed and 0 skipped**. The same run regenerated and validated the exact API28-classified database, passed all table-scoped integrity/FTS/golden/query-plan checks, and measured a 15,033,263-byte APK increment against the 20 MiB threshold.

This STEP 4 result did **not by itself** close the Location milestone. STEP 5 and STEP 6 later closed on their own exact-SHA gates, and STEP 7 final regression subsequently passed on `b41dd6a4b8a29204a4cb01b0d640a44504139cfc` in runs `33326121715` and `33326126008`; the Location milestone is now **CLOSED**.

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
- **Location STEP 4 (GeoNames/SQLite/data/API28 gate).** Definitive run `33296099459` on `8620772ebe0dd5b51691ce2447c46ef996cd90d1` passed unit/build/API28 regression (16/16 instrumentation tests), frozen GeoNames provenance/data integrity, runtime-minimal query/FTS gates, and the real 20 MiB APK threshold with a 15,033,263-byte increment. The final city asset SHA-256 is `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`; 224,330 cities and 258,685 aliases remain intact, including the controlled 17-city `America/Nuuk` unsupported set on API28.

### Location milestone — CLOSED

- Native `LocationManager` architecture; no Play Services.
- Foreground Device location only.
- `ACCESS_COARSE_LOCATION` only.
- 20s timeout / 5 km / 15 min; timezone change significant.
- FRESH/CACHED, never invented defaults.
- GeoNames `cities500` offline, CC BY 4.0, filtered by the approved deny-by-default populated-place feature-code whitelist.
- Read-only SQLite; no Room.
- Runtime-minimal city schema: E6 coordinates, numeric timezone lookup, `population` retained, country/admin1 short text codes retained, only `city_lat_lon_idx` as explicit secondary runtime index, alias dedup in build-time staging, FTS4 contentless unchanged.
- API28 timezone policy: four explicit verified compatibility mappings; exactly one residual modern id (`America/Nuuk`) affecting 17 cities is controlled `UNSUPPORTED_TIME_ZONE` on API28 while remaining in/searchable from the dataset.
- Preferences DataStore `1.2.1`.
- Arihna-owned Location models/state/errors; `PrayerTimeCalculator` unchanged.
- **STEP 5 — Android `LocationManager` + permission/resolution flow: CLOSED.** Exact promoted candidate `7f59c55da954347a1db5c17fe41c2cb07309184c`; definitive run `33317622881` passed `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 instrumentation (21/21, 0 failed, 0 skipped), with COARSE-only/no-FINE/no-background/no-Play-Services policy intact. Real Galaxy S25 verification independently returned a successful approximate-location current fix through the unmodified production bridge.
- **STEP 6 — minimal functional Device/Manual Compose UI: CLOSED.** Exact clean candidate `df53f71c07cd3da743604898941f6b4ef39e86aa`; definitive run `33325240888` passed toolchain/permission policy, exact STEP 4 asset restore/SHA, `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 instrumentation (**25/25, 0 failed, 0 skipped**). The functional Location panel remains scoped to `Impostazioni`, gates `ACCESS_COARSE_LOCATION` behind explicit Arihna rationale/confirmation, supports offline manual-city search/selection and controlled unsupported-timezone presentation, and does not implement the definitive Hero Dashboard.
- **STEP 7 — full Location unit/build/API28 regression and milestone closure: CLOSED.** Exact pre-closure `main` SHA `b41dd6a4b8a29204a4cb01b0d640a44504139cfc`; runs `33326121715` and `33326126008` both passed the accumulated host/API28 regression with 25/25 instrumentation tests, 0 failed and 0 skipped. The complementary data gate reconfirmed the 15,033,263-byte GeoNames APK increment below the 20 MiB threshold and the prior Prayer Engine regression remained green.

### Prayer Engine + Location integration milestone — OPEN / STEP 5 CLOSED

- Approved orchestration boundary: `PrayerScheduleRepository.observeSchedule(): Flow<PrayerScheduleState>` + idempotent `refresh()`, consuming only closed Location state, persisted Prayer settings, the existing `PrayerTimeCalculator`, and an injected testable `Clock`.
- Canonical temporary default: MWL / STANDARD / AUTOMATIC / zero offsets, materialized in the existing Preferences DataStore under separate Prayer keys. This is explicitly a known temporary convention limitation until the dedicated Prayer settings UI exists; no location-based automatic method selection is authorized.
- Recalculate only on distinct selected Location, settings or selected-zone local-date change/bootstrap; countdown is presentation-only and never a Prayer recalculation trigger.
- UI scope is a minimal functional Prayer Schedule panel on Home; the existing Location panel remains in `Impostazioni`; final Hero Dashboard is deferred.
- Error discipline: non-Ready Location means no prayer schedule; Prayer calculation unavailable remains a controlled unavailable state with no invented fallback.
- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is CLOSED** after exact-SHA gate `33338723836` on `ffb391846481a38cca0786be238bddae912ba862`. **STEP 3 schedule orchestration is CLOSED** on clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive exact-SHA run `33357287019` / job `99381759479` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). **STEP 4 presentation/countdown is CLOSED** on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4` after definitive exact-SHA run `33360819887` / job `99391679780` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). **STEP 5 functional Home panel is CLOSED** on clean technical commit `8b6a9c56a85e791691c41aa3d775788026b76bf0` after definitive exact-SHA run `33362751963` / job `99397193018` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**39/39, 0 failed, 0 skipped**). STEP 6 remains **NOT STARTED / NEXT** and requires separate authorization.

### Pending

1. Arabic Quran source/packaging.
2. Italian translation inclusion/source.
3. Debug-release trigger convention.
4. Dedicated Prayer settings UI for user selection of calculation method, Asr convention, high-latitude rule and offsets; until then the documented MWL/Standard/Automatic/zero-offset default is a known temporary limitation.
5. Default notification/adhan sound policy and audio licensing.
6. Pre-31 custom splash fallback.
7. Any future need for `ACCESS_FINE_LOCATION`.
8. Any future need for background location/foreground location service.

## 15. Explicitly out of scope unless later approved

General: Play Store, paid services/APIs, user accounts/cloud sync, ads/monetization, third-party tracking analytics, social/community.

The closed Location milestone excluded:

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

Current Prayer Engine + Location integration milestone additionally excludes:

- Qibla and sensors
- `POST_NOTIFICATIONS`, notification channels and adhan reminders
- `AlarmManager`, WorkManager and custom alarms
- adhan audio
- Quran and daily-content work
- definitive Hero Dashboard / final Prayer Times timeline
- editing Prayer calculation settings in UI (persistence/default only in this milestone)
- changes to the internal Prayer Engine formulas or closed Location acceptance/acquisition logic

## 16. Milestone sequence

1. Branding/UI decision closure — CLOSED.
2. Android bootstrap — CLOSED/build verified.
3. Prayer-time calculation — CLOSED; final commit `e5987f878e253085425f9bfebf7bf714c8405de3`; JDK21/API28 regression passed.
4. **Location (Device + manual city) — MILESTONE CLOSED.** STEP 1 through STEP 7 are closed after exact-SHA final regression on `b41dd6a4b8a29204a4cb01b0d640a44504139cfc`.
5. Location sequence/status: STEP 1 spec/architecture — **CLOSED** → STEP 2 pure Kotlin domain/state/policies + fake tests — **CLOSED** → STEP 3 Preferences DataStore — **CLOSED** → STEP 4 GeoNames generation/read-only SQLite + APK-size measurement + CityRepository/timezone/data/API28 gate — **CLOSED** → STEP 5 Android `LocationManager` + permission/resolution — **CLOSED** → STEP 6 minimal functional Device/Manual UI — **CLOSED** → STEP 7 full unit/build/API28 + Prayer/data/APK final regression — **CLOSED** → **STOP**.
6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 5 CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **CLOSED** → STEP 4 presentation/countdown — **CLOSED** → STEP 5 functional Home panel — **CLOSED** → STEP 6 full Prayer+Location+Integration regression — **NOT STARTED / NEXT** → STEP 7 docs-only closure — **NOT STARTED** → STOP. STEP 5 is closed on clean technical commit `8b6a9c56a85e791691c41aa3d775788026b76bf0` after definitive exact-SHA run `33362751963` / job `99397193018` completed **39/39 instrumentation tests, 0 failed and 0 skipped**; STEP 6 is not authorized or started by this closure.
7. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and definitive Hero Dashboard remain separate future milestones and must not begin before this integration milestone is closed.

## 17. Change log

### 2026-08-31 — Prayer Engine + Location integration STEP 5 CLOSED after exact clean-candidate gate

STEP 5 functional Home Prayer Schedule panel is **CLOSED**. Final clean technical candidate `8b6a9c56a85e791691c41aa3d775788026b76bf0` was rebuilt directly above STEP 4 docs-closed `main` commit `faebc922f04fe91576fe7987c593d1bdd1b46e15` and changes only `ArihnaApp.kt`, `ArihnaNavHost.kt`, `HomePrayerScheduleScreen.kt`, and `HomePrayerScheduleScreenAndroidTest.kt`. Definitive run `33362751963` / job `99397193018` checked out that exact SHA, restored/verified the frozen GeoNames asset, preserved the COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy gate, passed `testDebugUnitTest` and `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **39/39 tests, 0 failed and 0 skipped**. Home now renders the approved minimal location/source, next-prayer/time/countdown, complete current-day schedule, and secondary calculation method; NoLocation routes through a CTA to the existing Location settings panel, while CalculationUnavailable and Loading expose no fabricated values. The first sibling candidate `db401c9022110e6f80eb7ebaec5c354ea16e42ad` is diagnostic only: run `33362170081` failed before instrumentation execution on two unsupported Compose-test imports; only the test source was corrected before the final clean sibling was rebuilt and gated. The final candidate was promoted by non-forced fast-forward. **STEP 6 remains NOT STARTED / NEXT and requires separate authorization.**

### 2026-08-31 — Prayer Engine + Location integration STEP 4 CLOSED after exact clean-candidate gate

STEP 4 presentation/countdown is **CLOSED**. The final technical candidate `5019906d0ef83a33d09f5a3295d736422c8af7a4` was reconstructed directly above STEP 4 spec-first commit `fa16c35330c3540671eee681b352ea22df0c5d48` as one clean commit containing only `PrayerSchedulePresentation.kt`, `PrayerScheduleTicker.kt`, `PrayerScheduleViewModel.kt`, and `PrayerScheduleViewModelTest.kt`. Definitive run `33360819887` / job `99391679780` checked out that exact SHA, restored the frozen GeoNames asset with the approved SHA, preserved the COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy gate, passed unfiltered `testDebugUnitTest` and `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **35/35 tests, 0 failed and 0 skipped**. The ViewModel depends only on `PrayerScheduleRepository`, injected `Clock`, and a testable ticker; the production ticker cadence is one second, normal ticks update only the remaining duration, and a target that reaches zero is guarded so repository `refresh()` is requested once while awaiting the next cached schedule state. Focused pure/JVM tests cover NoLocation, Ready next-prayer/countdown, countdown decrement without repository calls, exactly-once zero refresh plus next-prayer transition, controlled `CalculationUnavailable`, and no refresh when no next prayer exists. No Compose/Home UI, `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or AppContainer wiring changed. The clean candidate was promoted to `main` by non-forced fast-forward after an ahead-1/behind-0 anti-divergence check. **STEP 5 remains NOT STARTED and requires separate authorization.**

### 2026-08-31 — Prayer Engine + Location integration STEP 4 presentation/countdown authorized

STEP 3 is confirmed closed on docs commit `ec1c2cfe6b6b5089ae40e2ee2e805a6901366831`. STEP 4 is explicitly authorized for presentation only: `PrayerScheduleViewModel`, `PrayerScheduleUiState`, injected `Clock`, a testable **1-second** ticker, and pure/JVM tests. Countdown ticks calculate only the duration to the repository-provided `NextPrayer` and must not touch Location, DataStore, Adhan, `PrayerTimeCalculator`, or trigger ordinary repository refreshes. When an active countdown reaches zero, a guarded idempotent `refresh()` may ask STEP 3 to advance `nextPrayer` from its cached today/tomorrow data; STEP 3 alone decides whether a real recalculation is necessary because the local civil date/input changed. No Compose/Home UI and no modification to PrayerScheduleRepository, Prayer Settings, Location, or Prayer Engine is authorized. STEP 5 remains **NOT STARTED** pending separate approval after an exact-SHA STEP 4 gate.



### 2026-08-31 — Prayer Engine + Location integration STEP 3 CLOSED after exact clean-candidate gate

STEP 3 schedule orchestration is **CLOSED**. The final technical candidate `2476ae86f585a6849f6f2104cddd215c6abf7d0f` was reconstructed directly above spec-first commit `bb972d71c42f86e88f18d05144c61b91dcfa1dd3` as one clean commit containing only the three Prayer Schedule domain/orchestration production files plus `DefaultPrayerScheduleRepositoryTest.kt`; no temporary development workflow/script or incremental history entered the candidate. Definitive run `33357287019` / job `99381759479` checked out that exact SHA, preserved the frozen GeoNames SHA/policy gate, passed `testDebugUnitTest` and `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **35/35 tests, 0 failed and 0 skipped**. The same final orchestration test blob had already executed the full **80/80 JVM** contract matrix in development run `33346275711`; the exact clean-candidate run then revalidated that unchanged test suite successfully. The repository uses exact Ready Location coordinates/ZoneId, deduplicated `PrayerScheduleInput`, selected-zone/DST-aware midnight boundaries, today-plus-tomorrow next-prayer continuity, controlled unavailable states, and a monotonic generation guard so a stale synchronous calculation cannot overwrite a newer location even when coroutine cancellation is not cooperatively observed. No UI, ViewModel/countdown, Prayer Engine, Location, or Prayer Settings behavior was changed. The clean candidate was promoted to `main` by non-forced fast-forward only after an ahead-1/behind-0 anti-divergence check. **STEP 4 remains NOT STARTED and requires separate authorization.**

### 2026-08-31 — Prayer Engine + Location integration STEP 3 schedule orchestration authorized

STEP 2 is formally closed on `main` at docs-only commit `ad6d4ac5d81230e613b09547bd45a7c44035ddba`. STEP 3 is now explicitly authorized and limited to `PrayerScheduleRepository` orchestration plus pure/JVM contract tests with fake Location and fake/recording `PrayerTimeCalculator`. Implement the already-specified `PrayerScheduleInput` deduplication, cancellation-aware latest-input behavior, exact Ready Location coordinate/ZoneId forwarding, selected-zone local-date and DST-aware midnight handling, today+tomorrow next-prayer continuity and controlled unavailable states. No UI/presentation/countdown/Home work and no changes to the closed Prayer Engine, Location or Prayer Settings behavior are authorized. The technical candidate must be rebuilt cleanly above this spec-first commit and receive a new full gate on its exact SHA before promotion; STEP 4 remains **NOT STARTED** until STEP 3 closure is reported and confirmed.

### 2026-08-31 — Prayer Engine + Location integration STEP 2 CLOSED after exact-SHA persistence gate

STEP 2 Prayer settings persistence is **CLOSED**. Clean technical commit `ffb391846481a38cca0786be238bddae912ba862`, built directly above spec-first commit `e10b7cc2780556ca3635ac47f5057059f2720755`, passed definitive workflow run `33338723836` / job `99330268698`: `testDebugUnitTest` and `assembleDebug` were successful, and Android 9/API28 `connectedDebugAndroidTest` completed **35/35 tests with 0 failed and 0 skipped**. The implementation shares the exact existing Preferences DataStore instance/file used by Location without renaming or migrating it, persists only the approved `prayer.*` keys, materializes the complete MWL/STANDARD/AUTOMATIC/zero-offset default on first use, and atomically recovers malformed or partial Prayer records to that canonical value. Real shared-DataStore instrumentation proves Prayer↔Location isolation in both directions rather than assuming isolation from prefixes. The earlier ddmlib install timeout in run `33337223313` executed zero tests and was infrastructure-only; its rerun exposed a JUnit teardown-signature issue in the new test harness, corrected without production-code changes before corrected development run `33338267865` and the definitive clean-candidate gate. No UI, `PrayerTimeCalculator`, Location behavior, or existing Location keys changed. STEP 3 remains **NOT STARTED** and is not authorized by this closure.

### 2026-08-30 — Prayer Engine + Location integration STEP 2 Prayer settings persistence authorized

STEP 2 is explicitly authorized after STEP 1 approval. Implement `PrayerSettingsRepository` on the **same existing Preferences DataStore instance used by Location**; the current DataStore file/name (`location`) is deliberately left unchanged to avoid a Location migration, while Prayer persistence is isolated by the new keys `prayer.method`, `prayer.asr`, `prayer.high_latitude_rule`, `prayer.offset.fajr`, `prayer.offset.sunrise`, `prayer.offset.dhuhr`, `prayer.offset.asr`, `prayer.offset.maghrib`, and `prayer.offset.isha`. First use must atomically materialize the canonical MWL + STANDARD + AUTOMATIC + zero-offset settings. Any partial set, invalid enum value, or plausible type corruption in an expected Prayer key must recover atomically to that complete canonical default without clearing or rewriting Location keys. Custom settings and positive/negative/zero offsets must round-trip. Verification requires focused JVM tests plus real Preferences DataStore instrumentation on Android 9/API28, including explicit two-way isolation: Prayer operations preserve existing Location entries, and Location operations preserve Prayer entries. No UI, no `PrayerTimeCalculator` changes, no Location behavior/key changes, and no STEP 3 orchestration work are authorized in STEP 2.

### 2026-08-30 — Prayer Engine + Location integration STEP 1 architecture approved and documented

A new seven-step integration milestone is **OPEN** after the Prayer Engine and Location milestones closed independently. STEP 1 is spec-first only: no production code is authorized in this step. The approved `PrayerScheduleRepository` observes closed Location resolution state plus persisted `PrayerCalculationSettings`, forwards the exact `SelectedLocation.coordinates + zoneId` into the unchanged `PrayerTimeCalculator`, derives current selected-zone `LocalDate`, calculates today plus tomorrow only as needed for next-prayer continuity, and exposes controlled schedule/no-location/calculation-unavailable state to presentation. Calculation inputs are deduplicated; latest-input cancellation prevents stale results from overwriting a newer location. Recalculation occurs only for a distinct selected Location, Prayer settings, local-day change, bootstrap/restore or meaningful explicit refresh; countdown ticks never invoke prayer calculation. Local midnight scheduling is based on `atStartOfDay(selectedZoneId)` rather than a fixed 24-hour delay.

Prayer settings will use separate keys in the existing Preferences DataStore. Until a dedicated settings UI exists, first initialization persists MWL + STANDARD Asr + AUTOMATIC high-latitude rule + zero manual offsets. This default is explicitly documented as a **known temporary product limitation**: users in Saudi Arabia or any region/community whose local convention differs from MWL may see times that differ slightly from local practice until method/Asr/high-latitude/offset selection is exposed in its own future settings UI. Arihna will not auto-switch methods based on location. UI scope for this milestone is a minimal functional Home Prayer Schedule panel (next prayer, time, countdown, complete current-day schedule and secondary method label); the closed Location panel remains in `Impostazioni`, and the definitive Hero Dashboard stays deferred. Error policy remains strict: any non-Ready Location produces no schedule, and `PrayerCalculationResult.Unavailable` never gains a fabricated fallback. STEP 2 Prayer settings persistence is **NOT STARTED / NEXT** and requires explicit authorization.

### 2026-08-30 — Location milestone CLOSED after STEP 7 final regression

The **Location milestone is CLOSED** after the complete STEP 7 regression on exact pre-closure `main` SHA `b41dd6a4b8a29204a4cb01b0d640a44504139cfc`. Final run `33326121715` passed policy/asset checks, the entire `testDebugUnitTest` suite, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest` with **25/25 tests, 0 failed and 0 skipped**. Complementary run `33326126008` on the same exact SHA regenerated the frozen GeoNames runtime database, passed the full host regression and the explicit Location+Prayer API28 regression with **25/25 tests, 0 failed and 0 skipped**, and remeasured the final GeoNames APK increment at **15,033,263 bytes** against the 20,971,520-byte threshold (margin **5,938,257 bytes**). Baseline APK was 34,150,268 bytes, APK with GeoNames 49,183,531 bytes, SQLite 28,020,736 bytes and compressed APK asset 15,033,136 bytes; the database SHA-256 remains `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`, with 224,330 cities, 258,685 aliases and the controlled 17-city `America/Nuuk` API28 unsupported set unchanged. Final policy remains COARSE-only with no FINE/BACKGROUND permission, no Play Services Location and no foreground location service. The prior Prayer Engine remains unchanged from `e5987f878e253085425f9bfebf7bf714c8405de3` and passed the accumulated final regression.

Traceability across the original Location plan: **STEP 1** architecture/spec commit `4c818dd2c5ef35bce3110f41d89360ff96ba6c28` (spec/orchestration step; no dedicated Actions run); **STEP 2** pure Kotlin domain/state/policies and **STEP 3** Preferences DataStore landed together in `640891f79ed708d69befc5f5ed70110e982db582` (no dedicated Actions run at that point; both are included in the final unfiltered STEP 7 regression); **STEP 4** implementation `8620772ebe0dd5b51691ce2447c46ef996cd90d1`, definitive run `33296099459`, 16/16 API28 tests; **STEP 5** exact technical candidate `7f59c55da954347a1db5c17fe41c2cb07309184c`, definitive run `33317622881`, 21/21 API28 tests, followed by docs closure `44348eb2251e4586b4d9efd5683ef87aca2d000a`; **STEP 6** exact clean technical candidate `df53f71c07cd3da743604898941f6b4ef39e86aa`, definitive run `33325240888`, 25/25 API28 tests, followed by docs closure `b41dd6a4b8a29204a4cb01b0d640a44504139cfc`; **STEP 7** then revalidated that exact pre-closure `main` tree with final runs `33326121715` and `33326126008`. No later product milestone has been started.

### 2026-08-30 — Location STEP 6 CLOSED after clean-candidate regression and exact-SHA promotion

STEP 6 is **CLOSED** while the overall Location milestone remains **OPEN**. The final technical tree was rebuilt as clean candidate `df53f71c07cd3da743604898941f6b4ef39e86aa` directly above the STEP 6 spec-first commit `f7ce99c24d92bac973dce444db37c201fe9b0f30`. Definitive workflow run `33325240888` checked out that exact clean SHA, passed toolchain/permission policy, restored the exact STEP 4 GeoNames asset with SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`, passed `testDebugUnitTest`, passed `assembleDebug`, compiled the Android test suite, and completed Android 9/API28 `connectedDebugAndroidTest` with **25/25 tests, 0 failed and 0 skipped**. The exact candidate was then fast-forward promoted to `location-step6` before this documentation-only closure commit. The production result is the approved minimal functional Location panel in `Impostazioni`: explicit Arihna rationale/confirmation before any `ACCESS_COARSE_LOCATION` request, no startup permission side effect, clear domain/source/freshness/error states, offline `CityRepository` manual search/selection, controlled `UNSUPPORTED_TIME_ZONE` behavior, Device re-selection, and foreground significant-update wiring that continues to delegate 5 km/ZoneId acceptance to the existing domain policy. Manifest/dependency policy remains COARSE-only with no FINE/BACKGROUND permission, no Play Services Location and no foreground location service. The Nuuk UI instrumentation instability encountered while reaching the final gate was test-harness-only: first IME viewport shrinkage and then LazyColumn off-viewport composition; the final test isolates search rendering with injected populated state and scrolls the lazy container to the result rather than fabricating production behavior. STEP 7 is **NOT STARTED / NEXT**; no STEP 7 implementation or regression work has begun.

### 2026-08-30 — Location STEP 6 minimal functional UI authorized and started

STEP 6 is authorized as a deliberately minimal functional Location panel inside the existing `Impostazioni` navigation destination; it is not the definitive Home/Hero Dashboard. The panel must render all existing `LocationResolutionState` outcomes, expose the active Device/Manual source, support local `CityRepository` search/selection, and allow return from Manual to Device. Device selection follows an Arihna rationale before any system permission request; `ACCESS_COARSE_LOCATION` is the only requested permission and no startup/init/restore path may launch the prompt. While foreground with Device selected, the UI layer connects lifecycle to the already-closed STEP 5 update flow and sends candidates back through `LocationCoordinator` without reimplementing 5 km/ZoneId significance. Add focused presentation and Compose UI tests with fakes/injected state, then run the full unit/build/API28 regression before STEP 6 promotion. STEP 7 remains not started.

### 2026-08-30 — Location STEP 5 CLOSED after final regression and exact-SHA promotion

STEP 5 is **CLOSED** while the overall Location milestone remains **OPEN**. Definitive workflow run `33317622881` tested exact candidate `7f59c55da954347a1db5c17fe41c2cb07309184c`: toolchain/permission policy and exact STEP 4 asset restore passed; `testDebugUnitTest` passed; `assembleDebug` passed; Android 9/API28 `connectedDebugAndroidTest` completed **21/21 tests with 0 failed and 0 skipped**. The same exact SHA was then fast-forward promoted to `location-step5`. The production bridge still requests only `ACCESS_COARSE_LOCATION`, contains no Play Services Location dependency, no background location and no foreground service; platform update requests use the 15-minute interval with no provider-level 5 km filter, leaving 5 km/ZoneId significance and the 20-second fresh-fix timeout in the domain layer. API28 synthetic COARSE delivery remains documented as a test-harness limitation; the CI cases stay active and verify real framework registration/cancellation rather than hiding the limitation with skips. Independent physical verification on a Samsung Galaxy S25 with approximate permission produced a real `SUCCESS` current fix through the unmodified bridge, 2000 m reported accuracy and `Europe/Rome`; exact coordinates are intentionally omitted. The temporary Device Test Compose hook remains only on `location-step5-device-test`/its diagnostic Release and is not part of the promoted STEP 5 branch or STEP 6. Physical Samsung significant-update movement, OEM Location Services toggle, and denied-permission UI interaction were not separately executed in this STEP; the first is a manual-device-only behavior under the 15-minute policy, while the latter UI prompt/rationale path belongs to STEP 6. STEP 6 is **NOT STARTED / NEXT** and must be separately authorized; STEP 7 remains **NOT STARTED**.

### 2026-08-30 — STEP 5 API28 harness limit classified; real Galaxy S25 bridge verified

The API28 test-provider failure is now classified as a test-harness limitation rather than a production bridge failure. Diagnostic run `33303926016` proved that Android 9 accepts and stores a complete mock location while its app-facing COARSE path still returns no last-known/current/listener fix for that synthetic provider; the injected mock lacks the production no-GPS metadata used by the coarse-location path. Final STEP 5 CI therefore keeps the A/B tests active but bounds them to real framework registration and cancellation/unregistration instead of requiring synthetic COARSE delivery; `@Ignore` and skipped-test masking are not allowed. Independent physical verification on a Samsung Galaxy S25, using approximate/COARSE-only permission, returned a successful real current fix through the unmodified production data source with 2000 m reported accuracy and `Europe/Rome`. Exact coordinates are intentionally omitted. API30 run `33309369262` is infrastructure-inconclusive and produced no cross-API instrumentation payload. The temporary Device Test UI remains isolated from the official STEP 5 candidate and from STEP 6.

### 2026-08-30 — Location STEP 5 Android LocationManager integration started

STEP 5 is authorized under the original Location milestone scope. Implement the existing `DeviceLocationDataSource` with Android framework `LocationManager`/AndroidX Core compatibility helpers and no Google Play Services Location dependency. The manifest must remain `ACCESS_COARSE_LOCATION` only, with no FINE/BACKGROUND permission or location foreground service. The platform current-fix operation is cancellable while the existing domain coordinator remains the sole owner of the 20-second timeout. Foreground callbacks use the approved 15-minute interval but no provider-level 5 km distance filter, because the existing pure `LocationUpdatePolicy` must continue to decide 5 km significance and accept timezone changes even below 5 km. Permission/service-state integration must populate the already modeled denied/disabled/unavailable/cache states; no system permission request occurs at app startup. The actual request is callable only from the explicit Device-location action after rationale, which STEP 6 will render. Verify with `testDebugUnitTest`, `assembleDebug`, and API28 instrumentation using Android mock/test-provider support where deterministic; document any path that remains inherently physical-device-only. Do not start STEP 6 until STEP 5 is closed and confirmed.

### 2026-08-30 — Location milestone state corrected; STEP 4 remains closed

Commit `0abffce8eb320fe87e546ad9d48be9ca56dba4d1` incorrectly conflated successful closure of Location STEP 4 with closure of the entire Location milestone and incorrectly described STEP 5/STEP 6 as future work requiring a newly approved scope. This status interpretation is superseded. The originally approved seven-step Location plan remains authoritative: STEP 4 is **CLOSED**, while STEP 5 Android `LocationManager` + permission/resolution and STEP 6 minimal functional Device/Manual UI are **NOT STARTED** and remain inside the already approved milestone scope; STEP 5 is next. The Location milestone therefore remains **OPEN** and no subsequent product milestone may start yet.

The STEP 4 technical evidence itself remains valid and unchanged: definitive workflow run `33296099459` on implementation commit `8620772ebe0dd5b51691ce2447c46ef996cd90d1` restored the exact frozen E6 staging dataset from run `33292976302`, regenerated the API28-classified runtime-minimal database, and preserved 224,330 cities / 258,685 aliases / 391 timezone names with zero logical city/alias mismatches. Non-FTS table-scoped integrity checks were `ok`; FTS document parity/orphan checks and golden Roma/Makkah/Mecca/New York/Sydney MATCH tests passed; search used the FTS virtual table and nearest used `city_lat_lon_idx`. The four reviewed API28 mappings and the exact residual `America/Nuuk` set of 17 searchable-but-not-selectable cities passed the STEP 4 instrumentation policy. Host `testDebugUnitTest` passed; Android 9/API28 `connectedDebugAndroidTest` ran **16 tests, 0 failed, 0 skipped**. The final SQLite asset is 28,020,736 bytes, SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`; AAPT/DEFLATE stores it as 15,033,136 bytes and the measured APK increment is **15,033,263 bytes**, 5,938,257 bytes below the 20 MiB gate.

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