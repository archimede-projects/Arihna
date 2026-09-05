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

#### S25 one-shot provider timeout correction — APPROVED 2026-08-31

Real-device diagnostics on the primary Galaxy S25 established that the Android framework provider selected by the shared coarse selector can be `fused`, and that `LocationManagerCompat.getCurrentLocation(...)` against that framework `fused` provider can systematically fail to deliver a genuinely new fix inside the existing 20-second current-fix timeout. In the same sessions the foreground update channel can still emit a previously captured fix whose age continues increasing; such an update does not prove that the one-shot request produced a fresh fix.

A direct source comparison with the current Timzguida app does **not** invalidate this diagnosis: Timzguida declares both `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`, depends on Google Play Services Location, and uses `FusedLocationProviderClient`. Its automatic-location path immediately consumes `fused.lastLocation`, while also issuing `getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, ...)` and `requestLocationUpdates(...)`. Timzguida's observed instant result can therefore come from Play Services cached location and/or a different provider engine; it is not evidence that Arihna's framework one-shot should behave identically. Arihna keeps its deliberate no-Play-Services and COARSE-only policies.

Approved production correction:

- `getCurrentLocation()` gets a **dedicated one-shot provider selector**: enabled `LocationManager.NETWORK_PROVIDER` first, framework `fused` only as fallback when network is unavailable.
- `observeSignificantUpdates()` continues to use the existing coarse provider selector unchanged; this correction must not alter its provider choice or update registration behavior.
- Do not change the 20-second timeout, foreground lifecycle, 5 km acceptance policy, 15-minute minimum update interval, cache/FRESH semantics, permission policy, or location persistence behavior.
- Do not add `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, Google Play Services Location, or a new fallback based on invented/default coordinates.
- The separate Home latency investigation remains diagnostic-only and is not part of this correction.

#### Galaxy S25 intermittent current-location fallback — APPROVED 2026-09-02

The 2026-08-31 network-first one-shot correction is superseded. A later draft proposing fused-first as a deterministic fix was intentionally kept off `main` and is also superseded by the complete real-device evidence below. The production problem is now classified as **intermittent current-location availability on the primary Galaxy S25**, not as a reliably wrong provider choice or a simple timeout that can be fixed by waiting longer.

Diagnostic path and evidence, all with framework `LocationManager`, `ACCESS_COARSE_LOCATION` only, no Google Play Services Location, Galaxy S25 / SDK 36:

- Production network-first one-shot repeatedly reached Arihna's 20-second timeout with no fix.
- Parallel A/B `getCurrentLocation()` showed framework `network` returning callback `null` at about **30.018 seconds**, while framework `fused` in one run returned a genuinely current fix at **22.838 seconds** with monotonic age about **0.014 seconds**.
- Repeating the same A/B in the same app session, without closing Arihna or changing the observable environment, later produced **`null` from both providers** at about **30.020 seconds network / 30.028 seconds fused**. Therefore fused is not a deterministic success path; timeout extension alone is not an adequate reliability fix.
- A later fused callback observed with `latencyMs=42` and `elapsedAgeMs=25974` is interpreted by units literally as a callback after **0.042 seconds** carrying a fix about **25.974 seconds old**, not as a 23-second acquisition. This reinforces that callback latency and fix age are separate quantities and must not be conflated.
- Bounded `network + requestLocationUpdates`: an immediate historical callback was observed at **0.039 seconds** with age about **284.536 seconds**, was rejected by the diagnostic 10-second bound, and no fresh callback arrived before terminal timeout at **35.027 seconds**. A later repetition returned a network callback after **0.037 seconds** aged about **589.022 seconds**, then again timed out at **35.020 seconds**.
- Bounded `fused + requestLocationUpdates`: one callback after **0.038 seconds** was already about **50.088 seconds old**, was rejected by the diagnostic bound, and no new callback arrived before timeout at **35.006 seconds**.
- During the later network-updates repetition, the existing production fused foreground stream subscribed after the one-shot timeout and received a real fused fix almost immediately; that fix was about **10.415 seconds old**. This demonstrates that a useful real cached/historical fix may exist in the framework stack even when the explicit current request fails.
- The diagnostic `requestLocationUpdates` acceptance bound of 10 seconds was methodological only. It is not and must never become a production cache TTL/FRESH threshold.
- Android framework documentation explicitly permits `getCurrentLocation()` to complete with `null` when a provider cannot generate a valid current fix. `getLastKnownLocation()` may return a quite old cached location or `null`; therefore Arihna must treat last-known data as optional real cache and expose its age honestly.

Approved production strategy — **Approach B: current attempt + transparent cache fallback**:

1. Keep one explicit framework **`fused` `getCurrentLocation()`** attempt for Device refresh. Revoke network-first selection; do not add a parallel provider race in this correction.
2. Set Arihna's current-fix opportunity budget to **30 seconds**. This value is not based on an assumption that fused will succeed by 30 seconds; it is a bounded UX/power budget that gives the framework a real opportunity to return a current fix while acknowledging that it may still terminate with `null`.
3. A non-null valid result from the explicit current request enters the existing acceptance pipeline as `FRESH` input. The existing 5 km/ZoneId significance policy remains authoritative; this correction does not force persistence of insignificant movement.
4. If framework current-location returns `null`/provider-unavailable, the Android datasource immediately attempts a **last-known fallback** instead of treating the session as a hard no-location failure.
5. If the coordinator's 30-second timeout cancels the current request before a result, the coordinator explicitly asks the datasource for the same last-known fallback after cancellation. Timeout ownership remains in `LocationCoordinator`/`LocationUpdatePolicy`; the datasource does not introduce a second independent timeout.
6. System last-known candidates are real cached framework `Location` objects only. Inspect the framework `fused` and `network` providers when available and choose the **most recently captured valid** raw candidate; do not use passive/GPS as an invented extra policy. `getLastKnownLocation()` may return `null`, so no cache is assumed to exist.
7. A raw framework last-known `Location` does **not** contain the historical `ZoneId` associated with its capture and therefore is not, by itself, a complete Arihna `DeviceLocationFix`. Never attach `ZoneId.systemDefault()` at fallback time to old coordinates. A raw framework candidate may become a calculable CACHED fix only when Arihna can provenance-match it to an already persisted real Device fix with the same capture instant and coordinates; that persisted record supplies the captured `ZoneId`. Otherwise the framework candidate remains evidence that cached coordinates exist, but the calculable fallback is the newest complete Arihna-persisted Device fix. If no complete cached Device fix exists, return the controlled `Unavailable` state rather than inventing a timezone.
8. There is **no production maximum-age TTL** in this correction. A real valid cached fix may remain usable even when it is hours or days old; its age/timestamp must be shown explicitly. If neither framework nor persisted cache exists, return the existing controlled `Unavailable` state.
9. Extend `DeviceLocationResult.Success` (or an equivalent datasource-domain result) with explicit `LocationFreshness.FRESH/CACHED` metadata so a last-known fallback can never be mislabeled as current. `SelectedLocation` also carries the resolved freshness metadata for Device locations (`FRESH` or `CACHED`; Manual has no Device freshness), so downstream Prayer/Home code does not need to infer freshness from timestamps. `LocationResolutionState.Ready` must expose the same authoritative value without allowing the two representations to diverge.
10. A CACHED fallback is allowed to produce a normal `Ready` location so prayer times continue to calculate from the last real coordinates/ZoneId. This is an intentional UX/reliability change from the current `Unavailable(cachedLocation=...)` behavior: cached real location remains usable for calculations while clearly disclosed as cached.
11. Prayer Schedule/Home must propagate Device freshness and capture time. When the active Device location is CACHED, show a visible message/badge such as **“Basato su posizione di 2 ore fa”** (or an absolute/date-style equivalent for older data). Do not show this cache-age badge for Manual location.
12. When Home is using a CACHED Device location, expose **“Aggiorna posizione”**. The action retries the existing Device current-location resolution on demand using current permission/services state; it does not introduce polling, a background service, or a new refresh cycle.
13. Keep the existing foreground significant-update stream, 15-minute minimum interval, zero provider-level distance filter, 5 km/ZoneId domain acceptance, lifecycle start/stop behavior, persistence model, manual-city path, COARSE-only permission policy, and no-Play-Services policy unchanged.
14. No FINE permission, background location, foreground location service, current-provider race, production freshness TTL, or cache expiry policy is authorized by this decision.

Freshness semantics for this correction:

- `FRESH`: result came from the explicit current-location operation in the current resolution flow and survived the existing validity/significance pipeline.
- `CACHED`: result came from framework last-known or Arihna's previously persisted real Device fix, or the existing significance policy deliberately retained the prior accepted real fix. CACHED always retains the original capture timestamp.
- Cache age shown to the user is derived from the real `LocationSource.Device.capturedAt` timestamp and the current clock; clamp negative display ages to zero rather than inventing future age.

Required implementation/gate evidence before promotion to `main`:

- exact technical SHA must descend directly from this spec-first commit;
- current one-shot selector is framework `fused`, not network-first;
- `currentFixTimeout` default is exactly 30 seconds;
- current callback `null` falls back to real last-known without mislabeling it FRESH;
- coordinator timeout also falls back to real last-known;
- raw framework last-known selection is deterministic, but no raw cached coordinates are paired with a newly sampled/system-default timezone; only a provenance-matched or already persisted complete Device fix can become calculable CACHED state, and absent complete cache remains controlled unavailable;
- existing 5 km/ZoneId acceptance and 15-minute foreground-update policy remain unchanged;
- Prayer Schedule propagates CACHED metadata/capture time and Home renders the cache-age disclosure plus on-demand `Aggiorna posizione` action;
- unit regression, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest` all pass with zero skipped tests;
- manifest/dependency policy remains `ACCESS_COARSE_LOCATION` only, no FINE/BACKGROUND and no Google Play Services Location;
- frozen GeoNames runtime asset integrity remains unchanged.

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

- fresh-fix timeout: **30 seconds**;
- significant movement threshold: **5 km**;
- minimum foreground update interval: **15 minutes**.

Accept/update a device location when at least one is true:

1. it is the first valid fix;
2. movement from the accepted fix is at least 5 km;
3. the associated device `ZoneId` changes;
4. there is no previously usable fix.

When Arihna enters foreground while `Device` is selected, request a fresh fix and observe significant updates while foreground; stop updates when leaving foreground. Do not persist every provider callback or behave like a navigation tracker.

- The Android callback layer must not reimplement the 5 km acceptance rule. Its update request uses the approved 15-minute minimum interval but must not impose a 5 km provider-level `minDistance`, because doing so could suppress a legitimate `ZoneId` change below 5 km. Every delivered candidate is passed to the existing pure `LocationUpdatePolicy.shouldAccept(...)` decision.
- The 30-second fresh-fix timeout remains owned by `LocationCoordinator`/`LocationUpdatePolicy`; `DeviceLocationDataSource` supplies a cancellable current-location operation and does not maintain a second independent timeout.

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
- freshness? (`FRESH`/`CACHED` for Device; null for Manual)

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

### 5.3 Prayer Engine + Location integration — MILESTONE CLOSED

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
6. **STEP 6 — full regression gate: CLOSED.** Definitive exact-SHA run `33638278152` / job `100274563542` validated promoted `main` SHA `edc97468d209d2a19452ec6d374e76537b32566d` with the complete Prayer + Location + Integration regression, exact GeoNames asset, APK-size non-regression, and Android 9/API28 coverage.
7. **STEP 7 — documentation-only milestone closure: CLOSED by this documentation step.** The tested technical SHA was green, real-device validated and promoted before this docs-only closure; no application code or runtime policy changes here.

STEP 2 Prayer settings persistence, STEP 3 schedule orchestration, STEP 4 presentation/countdown, STEP 5 functional Home panel, STEP 6 full regression, and STEP 7 documentation-only closure are **CLOSED**. The Prayer Engine + Location integration milestone is therefore closed. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones and are not started or authorized by this closure.


#### STEP 6/7 definitive closure evidence — 2026-09-02

- Definitive technical source: promoted `main` SHA `edc97468d209d2a19452ec6d374e76537b32566d`, directly above spec-first `dc2262cdca2861a721777511e031ae2f416d4c67`.
- STEP 6: run `33638278152`, job `100274563542`, `success`; exact SHA/main equality verified; Temurin JDK 21 with Java/Kotlin target 17; COARSE-only, no FINE/background/Google Play Services Location, no ignored tests; 30 s / 5 km / 15 min policies unchanged.
- Unit regression: **98/98**, 0 failures, 0 errors, 0 skipped.
- Exact GeoNames: `28,020,736` bytes, SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`. Current-build APK measurement: baseline `34,363,378`, full `49,396,640`, increment **15,033,262 bytes**, `5,938,258` below the 20 MiB ceiling. This is a STEP 6 non-regression measurement and does not rewrite the historical STEP 4 measurement.
- Android 9/API28: **41/41**, 0 failures, 0 errors, 0 skipped; classified coverage: 17 Prayer, 28 Location, 5 Home/integration cases.
- Galaxy S25 manual validation PASS: candidate `685f37336fffc1cbfcd7169b74e2f0866c23498c` exercised the honest `CACHED` fallback; promoted candidate `edc97468d209d2a19452ec6d374e76537b32566d` then validated cache-first re-entry, showing persisted `Ready(CACHED)` Home/prayer data immediately on reopen without blocking for the current-location opportunity window. These PASS results do not claim framework FRESH acquisition is reliable; the documented S25 intermittency remains.
- Location technology at closure remains Android framework `LocationManager`, COARSE-only, no background permission and no Google Play Services Location. A possible future migration to Google Play Services `FusedLocationProviderClient` is explicitly **DEFERRED / PENDING** and is not decided by this closure.
- STEP 7 is documentation-only. No new product milestone is authorized or started; development stops here under the one-objective rule until the next objective is explicitly selected.

### 5.4 Qibla — MILESTONE CLOSED

The Qibla milestone consumes the already-closed Location contract and must not reopen location acquisition, prayer calculation, persistence, or provider policy. Its purpose is to calculate the great-circle initial bearing from the active `SelectedLocation` to the Kaaba and, when suitable Android orientation sensors exist, present the live device direction relative to that true-north Qibla bearing. Once a valid `SelectedLocation` exists, Qibla calculation and compass operation require no network service.

#### Authoritative target and bearing mathematics

- Arihna uses a fixed Kaaba target, not the centroid of Makkah or an online geocoder result.
- Approved target coordinates: **latitude `21.42251267`, longitude `39.82619741`**, the GeoNames Kaaba shrine record reviewed on 2026-09-02. These coordinates are frozen as an Arihna constant for this milestone; changing them later requires a separate spec decision and regression update.
- `QiblaBearingCalculator` is a pure Arihna-owned boundary taking origin coordinates and returning the initial great-circle bearing from **true north**, normalized to `[0°, 360°)`.
- Required formula, with all angular inputs converted to radians: `atan2(sin(Δλ) * cos(φ2), cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(Δλ))`, followed by degree conversion and normalization. `φ1/λ1` are the selected origin and `φ2/λ2` are the fixed Kaaba coordinates.
- Reject non-finite/out-of-range coordinates through a controlled result. If origin and target are numerically coincident, bearing is undefined and must not be fabricated as `0°`; return a controlled `AT_KAABA_OR_COINCIDENT` result.
- No Adhan/prayer library, Android Location API, network lookup, timezone, clock, magnetic model, or sensor may enter the pure bearing calculator.

Reference golden bearings for the frozen target, used as test fixtures with an appropriate floating-point tolerance rather than string equality:

- Rome `41.9028, 12.4964` → about **123.276°**.
- New York `40.7128, -74.0060` → about **58.482°**.
- Sydney `-33.8688, 151.2093` → about **277.500°**.
- London `51.5074, -0.1278` → about **118.987°**.
- Jakarta `-6.2088, 106.8456` → about **295.152°**.

#### North reference and magnetic declination

- Qibla bearing is always defined against **true/geographic north**.
- Android orientation sources may be **direct true-north** or **magnetic-north** sources. Arihna must keep the reference explicit and must never apply magnetic declination twice.
- On API 33+ (`Build.VERSION_CODES.TIRAMISU` and newer), prefer `Sensor.TYPE_HEADING` when the runtime actually exposes it. Android defines this sensor as the direction the device is pointing relative to **true north**; `values[0]` is heading in degrees and `values[1]` is heading accuracy in degrees. This path uses the reported true heading directly and does **not** apply `GeomagneticField` correction.
- For fallback sensor paths whose heading is magnetic, calculate declination with Android `android.hardware.GeomagneticField` using the exact active `SelectedLocation.coordinates`, the injected/testable current time, and altitude `0 m` because altitude is not part of Arihna's closed Location contract. The `0 m` value is an explicit geomagnetic-model approximation only; it is not a fabricated user location and must not propagate into Location or Prayer state.
- Android defines positive declination as magnetic north rotated east of true north. Arihna therefore derives `trueHeading = normalize(magneticHeading + declination)`.
- The platform geomagnetic model may vary by Android/tzdata generation and is suitable here for consumer compass guidance, not surveying/navigation claims. Do not bundle a second magnetic-field model in STEP 1-5 unless same-device evidence shows the platform model is inadequate.
- Recompute declination when the effective `SelectedLocation` changes and when a Qibla session starts with a materially different clock date/time; ordinary high-frequency sensor events must not reconstruct `GeomagneticField` on every callback.

#### Android orientation sensor policy

Preferred foreground sensor hierarchy:

1. **API 33+ `Sensor.TYPE_HEADING`**, when actually available — preferred because Android reports heading directly relative to true north and includes a degree accuracy estimate. Runtime capability detection remains mandatory; API level alone does not guarantee that the physical sensor exists.
2. `Sensor.TYPE_ROTATION_VECTOR` — preferred magnetic-reference fallback. Android documents its earth reference as approximately east / magnetic north / sky and exposes estimated heading accuracy in `SensorEvent.values[4]` when available; convert to true north with the approved declination path.
3. `Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR` — controlled fallback when the normal rotation vector is unavailable. It is lower-power/lower-accuracy and does not use the gyroscope, but retains a geomagnetic north reference; convert to true north with declination.
4. Calibrated `TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD` with `SensorManager.getRotationMatrix()`, `remapCoordinateSystem()` and `getOrientation()` — final compatibility fallback when no heading/rotation-vector path exists.

Explicit exclusions:

- Do not use deprecated `Sensor.TYPE_ORIENTATION`.
- `TYPE_HEADING` was added in API 33; the implementation must be SDK-guarded and capability-checked so API28 remains fully supported through the documented fallback hierarchy.
- Do not use `TYPE_GAME_ROTATION_VECTOR` for Qibla heading because Android documents that it deliberately does not use the geomagnetic field and its north reference may drift.
- Do not require a gyroscope, magnetometer, accelerometer, or rotation-vector feature in the manifest in a way that prevents installation on otherwise supported devices. Runtime capability detection is authoritative.
- No sensor runtime permission, `BODY_SENSORS`, new Location permission, `ACCESS_FINE_LOCATION`, background Location, foreground service, or Google Play Services Location dependency is authorized by Qibla.

#### Screen orientation and heading derivation

- For `TYPE_HEADING`, consume the direct true-north heading/accuracy values without converting them through a magnetic rotation matrix. For rotation-vector and accelerometer+magnetometer fallbacks, derive azimuth through the appropriate rotation matrix and `SensorManager.getOrientation()`.
- Android sensor coordinates are based on the device's natural orientation. Any sensor value mapped to an on-screen compass must account for the current display rotation; use `getRotation()` / `remapCoordinateSystem()` where applicable so portrait/landscape changes do not silently rotate the compass reference.
- Normalize magnetic and true headings to `[0°, 360°)`.
- The live relative Qibla direction shown by the compass is `normalize(qiblaBearingTrue - deviceHeadingTrue)`. A relative direction near `0°` means the top of the displayed device/compass is pointing toward Qibla.
- Keep three concepts distinct in domain/presentation models: `qiblaBearingTrue`, `deviceHeadingTrue`, and `relativeQiblaDirection`. Do not overwrite one with another or infer the Qibla bearing from sensor state.
- Sensor smoothing is presentation-only. It must use circular/shortest-angle interpolation across the `0°/360°` boundary, never a naive arithmetic average that can jump through `180°`. The exact smoothing coefficient remains a real-device tuning value for Galaxy S25 validation; it is not frozen in STEP 1 because no same-device motion evidence exists yet. Raw calculated bearing remains unchanged and testable.

#### Accuracy, calibration and degraded capability

Arihna-owned heading quality is derived transparently from Android sensor information rather than invented precision:

```text
HeadingQuality
- HIGH
- MEDIUM
- LOW
- UNRELIABLE
- UNKNOWN
```

- Mirror Android `SENSOR_STATUS_ACCURACY_HIGH/MEDIUM/LOW/UNRELIABLE` where that status is provided.
- For `TYPE_HEADING`, retain Android's direct `values[1]` accuracy in degrees. For `TYPE_ROTATION_VECTOR`, retain `values[4]` estimated heading accuracy in radians/degrees when available (`-1` means unavailable). Numeric accuracy may be displayed as secondary guidance but must not be converted into a fabricated platform accuracy category.
- LOW or UNRELIABLE quality must surface a calm calibration/interference message. Arihna must not claim that the device is precisely aligned with Qibla while the platform reports unreliable orientation.
- HIGH/MEDIUM may show normal live compass guidance. UNKNOWN remains usable with an honest lack-of-accuracy indicator rather than being silently labeled good.
- Strong magnetic fields, metal cases, speakers, vehicles and other interference may make orientation inaccurate; the UI should advise moving away from interference and recalibrating/moving the phone when Android reports poor accuracy.
- No fixed user-facing degree threshold for "aligned" is approved in STEP 1. If a future haptic/success alignment cue is desired, its tolerance must be separately specified and validated on hardware rather than guessed now.

#### Location integration contract

- Qibla consumes only the already-authoritative `LocationResolutionState` / `SelectedLocation`. It never calls `LocationManager`, `CityRepository`, DataStore, provider callbacks, or a new geocoder directly.
- `LocationResolutionState.Ready` with either Device or Manual source is sufficient to calculate the Qibla bearing from the exact `SelectedLocation.coordinates`.
- A valid `Ready(CACHED)` Device location remains valid for Qibla and must be labeled consistently with existing Location freshness semantics; Qibla must not block on or trigger a fresh location request merely to calculate direction.
- Manual city works identically for mathematical bearing. Sensor heading still describes the physical phone's orientation at the user's actual device position; therefore when the active location source is Manual and represents a remote city, Arihna may show the **numeric/static bearing for that selected city**, but must not present the live physical compass arrow as if the phone were physically located in the remote city. Live sensor-to-Qibla alignment requires a Device-selected location representing the device position.
- Non-Ready Location states produce a controlled `NoLocation` Qibla state and no bearing. Reuse existing navigation/CTA toward Location settings; never substitute Rome, Makkah, device timezone coordinates, last UI city, or any default.
- Switching Device↔Manual or changing accepted `SelectedLocation` immediately invalidates/recalculates the bearing and relevant declination through latest-state semantics; stale sensor/location combinations must not overwrite the newer state.

#### Lifecycle and power

- Bearing calculation itself is pure and may remain available whenever a valid selected location exists.
- Register orientation sensors only while the Qibla screen is visibly active in foreground (`STARTED`/equivalent) and unregister promptly when it leaves the foreground or screen.
- Use a UI-suitable sensor sampling rate (`SENSOR_DELAY_UI` or an explicitly measured equivalent); do not request fastest/raw sampling without evidence.
- No background compass monitoring, background service, wake lock, persistent notification, WorkManager loop, or sensor batching milestone is authorized.

#### Domain / presentation boundaries

Approved conceptual boundaries:

```text
interface QiblaBearingCalculator {
    fun calculate(origin: Coordinates): QiblaBearingResult
}

QiblaBearingResult
- Success(bearingTrueDegrees)
- InvalidCoordinates
- AtKaabaOrCoincident

interface DeviceHeadingDataSource {
    fun observeHeading(): Flow<DeviceHeadingState>
}

DeviceHeadingState
- Unavailable(reason)
- Reading(
    trueHeadingDegrees,
    quality,
    estimatedAccuracyDegrees?,
    source,
    magneticHeadingDegrees?,
    declinationDegrees?
  )

HeadingSource
- TRUE_HEADING_SENSOR
- ROTATION_VECTOR
- GEOMAGNETIC_ROTATION_VECTOR
- ACCELEROMETER_MAGNETIC_FIELD

QiblaUiState
- NoLocation(...)
- StaticBearing(...)
- LiveCompass(...)
- SensorUnavailable(...)
```

- Exact naming may change during implementation if semantics remain identical; do not merge bearing math, Android sensors, Location acquisition and Compose rendering into one class.
- Inject testable time where `GeomagneticField` construction depends on time.
- Sensor datasource is Android/platform code. Bearing calculator and relative-angle math remain pure JVM-testable code.

#### Qibla UI contract

Qibla uses the previously approved **Layout 1** visual direction. The dominant element is the large compass, not raw coordinates or sensor diagnostics.

When live compass is valid:

- large compass/dial with clear Qibla/Kaaba marker;
- selected readable location as secondary context;
- numeric true-north bearing such as `Qibla 123°`;
- live directional rotation toward Qibla;
- secondary accuracy/calibration state.

When only static bearing is appropriate (for example Manual remote city or no usable orientation sensor):

- still show the selected location and numeric true-north Qibla bearing;
- explicitly state that live compass guidance is unavailable/not applicable;
- never animate a sensor arrow in a way that implies physical alignment for a remote Manual location.

The definitive visual styling remains the approved calm/spacious Arihna Green `#0F5132`, Gold `#D4AF37`, Off-white `#FAFAF6` direction. STEP 1 authorizes no Compose implementation yet.

#### STEP 1 test contract

Pure/JVM tests to be implemented before later closure must cover at least:

- frozen Kaaba constant;
- golden bearings for Rome, New York, Sydney, London and Jakarta;
- normalization around `0°/360°`;
- invalid latitude/longitude/non-finite input;
- coincident Kaaba input → controlled undefined result;
- relative direction math including wrap-around, e.g. `bearing=5°, heading=355° → 10°`;
- direct `TYPE_HEADING` true-north input is not declination-corrected a second time;
- magnetic-to-true fallback correction with positive and negative declination;
- sensor-source fallback selection is deterministic and API33 `TYPE_HEADING` absence falls through safely;
- Device Ready and CACHED Ready use exact accepted coordinates without a fresh-location request;
- Manual location produces static bearing and does not claim live physical alignment;
- all non-Ready Location states → no bearing / zero sensor-location fabrication;
- latest Device↔Manual/location change cannot expose stale combined state;
- circular smoothing crosses north by the shortest path once smoothing is implemented.

Android instrumentation / API28 coverage must verify where feasible:

- sensor capability discovery does not crash when preferred sensors are absent;
- sensor registration/unregistration follows foreground screen lifecycle with zero ignored/skipped Qibla tests at final gate;
- fallback selection order is deterministic, with API28 exercising the no-`TYPE_HEADING` compatibility path;
- no deprecated orientation sensor path;
- existing manifest remains COARSE-only with no FINE/BACKGROUND/BODY_SENSORS additions;
- existing Prayer + Location + Home regressions remain green.

Physical Galaxy S25 closure must verify the actual live compass:

- device-source Qibla screen opens without requesting new permissions;
- validation records which heading source the S25 actually exposes/selects, preferring `TYPE_HEADING` when available;
- compass responds smoothly to real rotation and crosses north without a 360° jump;
- displayed bearing remains stable while the phone rotates;
- poor sensor accuracy/calibration state is surfaced honestly when reproducible;
- leaving/re-entering Qibla does not leave duplicate sensor subscriptions;
- comparison against a trusted independent compass/Qibla reference is directionally consistent within ordinary consumer-sensor limitations. Do not freeze a numerical acceptance tolerance until the first diagnostic S25 readings establish realistic sensor variance.

#### Seven-step Qibla implementation sequence

1. **STEP 1 — spec-first architecture: CLOSED by this documentation step.** Freeze target, true-north bearing formula, sensor hierarchy, declination policy, Location semantics, lifecycle, UI states and test contract before code.
2. **STEP 2 — pure bearing engine: CLOSED.** Clean technical commit `c4bf58ab341bc69127d9075cd093a4c4080c6062`, built directly above STEP 2 authorization commit `1062bc33eda946351c799d694c4e8eecf849ace2`, adds only the frozen Kaaba target, pure `QiblaBearingCalculator`, angle utilities and focused JVM tests. Definitive exact-SHA run `33652882463` / job `100324058113` passed the full unit regression, `assembleDebug`, and Android 9/API28 connected regression. No sensors, Compose, Location-provider behavior, dependencies or permissions changed.
3. **STEP 3 — Android heading/sensor layer: CLOSED.** Clean technical commit `f5528b235d07d8504676a2b641f6fa2a381f486a`, built directly above STEP 3 authorization commit `27245561525e2a403a3642d19de8d723f0a4e455`, implements the foreground Android heading datasource and platform adapters only. Definitive exact-SHA workflow run `33668783356` / job `100376952037` checked out that exact technical SHA, passed the complete unit regression with **117/117 tests, 0 failures, 0 errors, 0 skipped** including **19 Qibla cases / 12 heading cases**, passed `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **42/42 tests, 0 failures, 0 errors, 0 skipped**, including the dedicated API28 heading compatibility case. The gate restored the frozen GeoNames asset and preserved the manifest/dependency policy: COARSE only, no FINE, no background Location, no `BODY_SENSORS`, no Google Play Services Location and no ignored tests. The implementation preserves the approved runtime hierarchy (`TYPE_HEADING` when API33+ and exposed, then rotation vector, geomagnetic rotation vector, then accelerometer + magnetic-field pair), applies `GeomagneticField` declination only to magnetic-reference fallbacks, handles display rotation, uses `SENSOR_DELAY_UI`, and unregisters the Android listener when flow collection is cancelled. No Compose UI, Location orchestration/provider change, Prayer change, manifest permission or dependency was introduced. STEP 4 remains **NOT STARTED** until separately authorized.
4. **STEP 4 — Location/Qibla orchestration: CLOSED.** Clean technical commit `947d7b41d0d8f32403133653d08852e56ec6cf6e`, built directly above STEP 4 authorization commit `e10b39c613a25c42cc28dfcb1ee855d60998132d`, adds only the Arihna-owned Qibla orchestration boundary/models and focused JVM tests. Definitive exact-SHA workflow run `33703960509` / job `100488988440` passed **126/126 unit tests** (0 failures/errors/skipped; 28 Qibla cases including 9 orchestration cases), `assembleDebug`, and Android 9/API28 connected regression with **42/42 tests**, 0 failures/errors/skipped. The gate restored the frozen GeoNames asset and preserved COARSE-only/no-FINE/no-BACKGROUND/no-BODY_SENSORS/no-Play-Services/no-ignored-test policy. Manual source makes zero heading subscriptions; Device including valid CACHED uses exact accepted coordinates, preserves static bearing on sensor failure, and cancellation-aware latest-location semantics suppress stale heading/location combinations. No Compose/navigation/AppContainer wiring, Location acquisition behavior, Prayer behavior, permissions, dependencies or sensor algorithms changed.
5. **STEP 5 — Qibla Compose UI: CLOSED.** Replace only the existing Qibla placeholder with the approved functional Layout 1 screen and composition-root wiring to the already-closed `QiblaRepository`; do not reopen bearing math, heading algorithms, Location acquisition, Prayer behavior, persistence, permissions or dependencies. The composition root may construct `DefaultQiblaRepository` from the existing authoritative Location presentation flow (`LocationResolutionState`), `QiblaBearingCalculator`, and `AndroidDeviceHeadingDataSource`. Qibla collection must be lifecycle-bound to the Qibla destination using `STARTED`/equivalent semantics so leaving the screen cancels upstream heading collection and the already-closed Android datasource unregisters promptly; no app-wide/background compass subscription is permitted.

STEP 5 UI states are fixed as follows: `NoLocation` shows a clear location-required message and CTA to the existing Location settings path with no bearing; `BearingUnavailable` shows a controlled explanatory state; `StaticBearing` (including Manual source) shows selected readable location, numeric true-north bearing and an explicit statement that live physical compass guidance is not applicable, with no animated live arrow; `LiveCompassStarting` shows the valid static bearing plus a calm sensor-starting state; `LiveCompass` renders the dominant large compass/dial and Qibla/Kaaba direction from the repository-provided relative direction, selected location, numeric bearing, quality/calibration guidance, optional numeric accuracy and heading source as secondary diagnostic context; `SensorUnavailable` preserves location + numeric bearing and clearly states that live guidance is unavailable. Device CACHED freshness remains visible/consistent rather than being silently presented as fresh. Do not expose raw coordinates as primary UI and do not create an alignment-success tolerance, haptic cue or fabricated precision.

Compass motion smoothing, if applied, is presentation-only and must cross `0°/360°` by the shortest circular path. A small pure helper may unwrap consecutive relative angles for Compose animation; it must not alter the repository bearing/heading values. No fixed alignment threshold is introduced. Styling uses the existing Arihna theme with Green `#0F5132`, Gold `#D4AF37`, Off-white `#FAFAF6`, a large central dial, clear north/cardinal reference and Qibla marker.

STEP 5 verification must add focused Compose/API28 coverage for at least NoLocation + settings CTA, Manual/static bearing with no live claim, LiveCompass rendering, degraded/unavailable sensor state, and absence of fabricated values. Pure/JVM coverage must verify shortest-path north-wrap helper if introduced. The exact technical candidate must pass unfiltered `testDebugUnitTest`, `assembleDebug`, Android 9/API28 `connectedDebugAndroidTest`, frozen GeoNames integrity and existing permission/dependency/no-ignored-test gates before promotion. After a green STEP 5 candidate is promoted, STEP 6 may package that exact runtime code as a prerelease APK for physical Galaxy S25 compass validation; STEP 6 is not closed until the user performs that hardware test.
6. **STEP 6 — full regression + API28 + Galaxy S25 validation: CLOSED.** Final approved WOW runtime `1b40c9f2e98014ce3b36f5b8629d8a8ac1931e37`, directly above spec-first `f0e5262a524a901d673dc6b51991177d0a79baa4`, passed definitive exact-runtime run `33880602477` / job `101048037602`: unit regression PASS, `assembleDebug` PASS, Android 9/API28 **47/47**, 0 failed and 0 skipped, frozen GeoNames and policy checks PASS. Persistent-debug prerelease `qibla-wow-1b40c9f2-20260904` was identity/signature/digest verified and physically validated PASS on the primary Galaxy S25.
#### Galaxy S25 compass-rose presentation correction — APPROVED 2026-09-04

Physical STEP 6 validation on the primary Samsung Galaxy S25 established that the closed STEP 5 runtime is mathematically and sensor-wise directionally correct but its live compass presentation has one semantic defect. With Device/CACHED location at Mirandola the app displayed a true-north Qibla bearing of **125°**; when the yellow Qibla marker was brought to the top of the phone, an independent compass also read **125°**. A complete physical rotation was smooth through the `359°→0°` crossing, lifecycle leave/re-enter behavior remained normal, the runtime selected `ROTATION_VECTOR`, and heading quality was reported HIGH. However, while the Qibla marker rotated correctly, the cardinal labels `N/E/S/O` remained fixed to the screen. In a UI explicitly presented as a live true-north compass, those fixed cardinals can misrepresent geographic north after the phone rotates.

The user also confirmed that Manual mode behaves correctly as a static bearing, but the existing explanatory sentence is too visually weak. The static/manual limitation must therefore be elevated into a clearly distinct Arihna card/banner rather than remaining ordinary secondary text.

Approved correction, narrowly scoped to Qibla presentation:

1. In `LiveCompass`, the cardinal rose must rotate from the already-authoritative `deviceHeadingTrueDegrees` so `N` tracks geographic true north relative to the physical phone. The presentation rotation is `normalize(-deviceHeadingTrueDegrees)` or an exactly equivalent transform; it must use shortest-path circular animation across north.
2. The yellow Qibla marker continues to use the repository-provided `relativeQiblaDirectionDegrees = normalize(qiblaBearingTrue - deviceHeadingTrue)` unchanged. The hardware-validated 125° result must not be re-derived or altered inside Compose.
3. Static/Manual mode remains a north-referenced static bearing diagram with no heading subscription and no live physical-alignment claim. Replace/elevate the existing plain explanatory copy with a visually distinct message such as **“Bussola statica”**, explaining that the direction is calculated for the selected city and that Device location is required for live phone alignment.
4. Do not change Qibla bearing mathematics, heading-source hierarchy, magnetic declination, Android sensor algorithms, Location acquisition/cache semantics, Prayer behavior, persistence, permissions, dependencies, lifecycle ownership, or manifest policy.
5. Focused tests must cover cardinal-rose rotation semantics from true heading, shortest-path north wrap for the rose, preservation of the independent Qibla-marker relative-direction path, and the prominent Manual/static notice. Existing Qibla and full-app regressions remain mandatory.
6. The exact technical candidate must pass unfiltered `testDebugUnitTest`, `assembleDebug`, Android 9/API28 `connectedDebugAndroidTest` with zero skipped tests, frozen GeoNames integrity, and the existing COARSE-only/no-FINE/no-BACKGROUND/no-BODY_SENSORS/no-Play-Services gates before promotion.
7. After promotion, publish a new persistent-debug-signed Galaxy S25 prerelease APK and revalidate only the corrected cardinal rose, Manual/static notice, and non-regression of the already-passed 125° direction, smooth `359°→0°`, CACHED behavior and lifecycle re-entry.

This decision **supersedes only** STEP 6's earlier restriction that the physical validation must use runtime `73ec85f3c40b5dced87eef2e16dc41eaca80173d` unchanged. STEP 6 remains **AUTHORIZED / IN PROGRESS** and is reopened only enough to permit this presentation correction plus its required regression and focused Galaxy S25 revalidation. STEP 7 remains **NOT STARTED**.

7. **STEP 7 — documentation-only milestone closure: CLOSED by this documentation step.** This commit records already-verified STEP 6 evidence only; no application code or runtime policy changes are made.

#### STEP 5 closure / STEP 6 authorization evidence — 2026-09-03

- Definitive STEP 5 technical runtime: `73ec85f3c40b5dced87eef2e16dc41eaca80173d` (`feat(qibla): implement lifecycle-bound compass ui`), with direct parent STEP 5 authorization commit `bf5d9c6472837f84752c3dafbac2fbfc64f01d2b`.
- Definitive exact-SHA gate: workflow run `33717262812`, attempt **2**, job `100533758926`, conclusion **success**. The workflow checked out exactly the technical runtime while `main` was still at the authorization spec SHA, then the runtime was promoted to `main` by non-forced fast-forward.
- Host unit regression: **130/130**, 0 failures, 0 errors, 0 skipped; **32 Qibla cases**, including **4 compass-motion wrap cases**.
- `assembleDebug`: **PASS**.
- Android 9/API28 connected regression: **47/47**, 0 failures, 0 errors, 0 skipped; **5/5 dedicated Qibla Compose UI cases**.
- The gate restored and verified frozen GeoNames SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb` and reverified COARSE-only policy, absence of FINE/BACKGROUND/BODY_SENSORS, absence of Google Play Services Location, absence of ignored tests, and lifecycle-bound Qibla collection at `Lifecycle.State.STARTED`.
- STEP 5 UI/wiring changes are limited to the approved composition-root/navigation/Qibla presentation scope plus focused tests. Manual location remains static-bearing only; Device locations may use live compass; CACHED provenance is visible; no fabricated alignment tolerance is introduced.
- An earlier API28 attempt was non-definitive because the emulator/test process was unstable. The independent rerun above completed all **47/47** tests and is the closure evidence.
- STEP 6 is packaging plus physical Galaxy S25 validation of the exact runtime `73ec85f3c40b5dced87eef2e16dc41eaca80173d`; no new product code is authorized before that hardware result.

Evidence basis reviewed for STEP 1 on 2026-09-02: Android Developers documentation for `Sensor`, `SensorEvent`, `SensorManager`, sensor coordinate/display remapping, `TYPE_HEADING`, rotation-vector/position sensors and `GeomagneticField`; GeoNames Kaaba shrine record for the frozen target coordinates. Android documents `TYPE_HEADING` (API 33+) as direct true-north heading with degree accuracy, rotation-vector orientation as magnetic-north referenced, `TYPE_GAME_ROTATION_VECTOR` as omitting geomagnetic north and potentially drifting, and `GeomagneticField.getDeclination()` as the magnetic-to-true-north declination estimate.

#### STEP 2 closure evidence — 2026-09-02

- STEP 2 authorization commit: `1062bc33eda946351c799d694c4e8eecf849ace2`.
- Definitive technical commit: `c4bf58ab341bc69127d9075cd093a4c4080c6062` (`feat(qibla): implement pure bearing engine`), exactly one technical commit above the authorization spec.
- Technical scope is exactly five new files under `core/qibla` and `src/test`; no existing production source, manifest, dependency, UI, Location, Prayer or integration file changed.
- Frozen Kaaba target: `21.42251267, 39.82619741`.
- Pure great-circle initial-bearing calculation is true-north referenced, normalized to `[0°, 360°)`, rejects invalid/non-finite coordinates, and returns controlled `AtKaabaOrCoincident` for the frozen target itself.
- Focused JVM coverage validates the frozen target, approved Rome/New York/Sydney/London/Jakarta golden bearings, invalid coordinates, coincident target, angle normalization and north wrap-around relative-direction math.
- Definitive exact-SHA gate: workflow run `33652882463`, job `100324058113`, `success`, checking out exactly `c4bf58ab341bc69127d9075cd093a4c4080c6062` with parent exactly `1062bc33eda946351c799d694c4e8eecf849ace2` and `main` still at the spec SHA during the gate.
- Host unit regression: **105/105**, 0 failures, 0 errors, 0 skipped, including **7 Qibla STEP 2 cases**.
- `assembleDebug`: PASS.
- Android 9/API28 connected regression: **41/41**, 0 failures, 0 errors, 0 skipped.
- Gate reverified frozen GeoNames SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`, COARSE-only policy, absence of FINE/BACKGROUND/BODY_SENSORS, absence of Google Play Services Location and absence of ignored tests.
- No physical-device validation is required for STEP 2 because no sensor/UI behavior exists yet; Galaxy S25 validation remains mandatory at Qibla STEP 6 after the live compass exists.

STEP 2 is **CLOSED**. STEP 3 Android heading/sensor layer remains **NOT STARTED** and requires separate explicit authorization. No sensor registration, UI or new permission is started by this closure.



#### STEP 6/7 definitive closure evidence — 2026-09-04

- Definitive runtime: `1b40c9f2e98014ce3b36f5b8629d8a8ac1931e37` (`feat(qibla): add final wow compass polish`), direct parent spec `f0e5262a524a901d673dc6b51991177d0a79baa4`.
- Definitive full exact-runtime gate: run `33880602477`, job `101048037602`, `success`; unit regression and `assembleDebug` passed; Android 9/API28 connected regression completed **47/47 tests, 0 failed, 0 skipped**; frozen GeoNames SHA-256 remained `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`; established permission/dependency checks passed.
- Galaxy S25 prerelease: `qibla-wow-1b40c9f2-20260904`, asset `arihna-qibla-wow.apk`, exact runtime target, `uploaded`, 49,609,751 bytes, APK SHA-256 `a76be3d084ecb413aba5309a882a1c874a2618c88e0108789db5b99a47c6960f`.
- APK identity: `com.archimedeprojects.arihna`; persistent Arihna debug certificate SHA-256 `13:97:00:8C:1F:96:2D:BB:D3:6D:D8:A8:EA:02:16:AF:DD:06:E4:B2:B3:E0:8B:C0:F6:D4:B5:43:44:D7:B0:FA`; keystore SHA-256 `bc9057f26ad6de7efb70a5df06effb1ed259f1d015c3f062d0f757b3f0983b72`.
- Physical Galaxy S25 final PASS: approved WOW presentation confirmed, including tone-on-tone Islamic skyline and gold geographic `N`; the already-validated dynamic checks remain PASS: smooth 360° rotation, correct `359°→0°` wrap, coherent true heading, correct Qibla marker direction, normal lifecycle and compact single portrait viewport without scroll.
- STEP 7 changes documentation only. Qibla mathematics, heading/sensor algorithms, declination, Location/cache, Prayer, lifecycle, permissions, dependencies, persistence and manifest policy remain unchanged.
- Qibla STEP 1–7 are CLOSED; the Qibla milestone is CLOSED. No next product milestone is authorized or started by this closure.

### 5.5 Alarms — MILESTONE OPEN / STEP 5 CLOSED / STEP 6 AUTHORIZED

The Alarms milestone follows the closed Qibla milestone and must not reopen Prayer calculation, Location acquisition/cache, Qibla mathematics/sensors, or existing persistence policy. STEP 1 is documentation/architecture only: it authorizes no manifest, dependency, Kotlin, UI, receiver, notification-channel or runtime behavior change. The first technical candidate must be a direct child of this spec commit.

The milestone provides two user-owned alarm families:

- **Prayer-linked alarms:** each rule targets one of the five obligatory prayers already produced by the authoritative `PrayerScheduleRepository` (`Fajr`, `Dhuhr`, `Asr`, `Maghrib`, `Isha`). Sunrise remains schedule information and is not a prayer-alarm target in this milestone. A prayer rule may carry a signed minute offset; `0` means the exact calculated prayer time. The concrete UI bounds for that offset remain **Pending** until the functional UI step, but the domain/scheduler must support crossing civil-day boundaries correctly.
- **Custom alarms:** independent personal alarms defined in the device's local civil time, with user label, local time and optional weekday recurrence. No selected prayer/location city is allowed to redefine a custom alarm's wall clock. An empty recurrence set means one-shot at the next valid device-local occurrence and auto-disables after delivery; a non-empty set repeats only on the selected weekdays.

`enabled` records the user's intent and is distinct from whether Android currently allows the alarm to be scheduled. Arihna must never label an alarm as scheduled/reliable merely because its rule is enabled.

#### Alarm domain and ownership boundaries

Use Arihna-owned boundaries equivalent to:

```text
AlarmRuleRepository
- observeRules(): Flow<List<AlarmRule>>
- create/update/delete/setEnabled(...)

AlarmScheduleCoordinator
- reconcile(reason)
- observes alarm rules, PrayerScheduleState and platform capability state while the app is active

AlarmPlatformScheduler
- scheduleExact(ResolvedAlarmOccurrence)
- cancel(alarmId / occurrence token)
- capability(): AlarmCapabilityState

AlarmRule
- PrayerLinked(...)
- Custom(...)

ResolvedAlarmOccurrence
- alarmId
- ruleRevision
- triggerAt Instant
- display local date/time + ZoneId provenance
- occurrence token

AlarmCapabilityState
- Ready
- NeedsNotificationPermission
- NeedsExactAlarmAccess
- PrayerScheduleUnavailable(reason)
- PlatformUnavailable(reason)
```

Exact class names may change if these ownership rules remain intact. `PrayerScheduleRepository` remains the sole source of calculated prayer times; the alarm layer must not call Adhan directly, duplicate prayer formulas, reinterpret Location acceptance/freshness, or invent coordinates/timezones. `AlarmRuleRepository` owns user alarm rules only. `AlarmPlatformScheduler` owns `AlarmManager`/`PendingIntent` details only. Compose must not call `AlarmManager` directly.

Every persisted rule has a stable app-owned `alarmId` plus monotonically changing rule revision/generation. Every scheduled OS occurrence carries an occurrence token derived from the current rule revision and resolved trigger. A receiver must validate that token against current persisted state before user-visible delivery, so a stale `PendingIntent` left behind by a reschedule can be ignored rather than delivering a duplicate/obsolete alarm.

#### Persistence decision

Reuse the already-approved Preferences DataStore dependency; do not add Room, a second DataStore instance, a serialization library or another persistence dependency for STEP 2 without a new spec decision.

- Alarm keys/records are isolated under an `alarm.*` namespace and must not clear or rewrite Location or Prayer settings.
- Persist the **rule**, enabled state, stable id/revision and sound-profile reference; do not persist a resolved epoch timestamp as the authoritative schedule because timezone, prayer settings and selected location can make it stale.
- Collection encoding must be deterministic, versioned, lossless and migration-testable. The exact internal codec may be selected in STEP 2 if it requires no new dependency and does not leak into the domain API.
- Deleting a rule must also cancel any currently resolved OS occurrence for that id.

#### Exact-alarm platform policy

Alarm delivery is user-facing and time-sensitive, so the approved platform is Android framework `AlarmManager`; WorkManager is not the trigger mechanism for prayer/custom alarm delivery.

- On API 23+ use a one-occurrence `RTC_WAKEUP` exact alarm compatible with Doze (`setExactAndAllowWhileIdle` or the tested equivalent). Do **not** use repeating `AlarmManager` APIs as the source of recurrence; always resolve and schedule the next single occurrence, then reconcile again after delivery/change. This avoids interval drift and allows DST, timezone, prayer-setting and location changes to take effect.
- On API 31+ exact scheduling must be guarded by `AlarmManager.canScheduleExactAlarms()`.
- Arihna will declare/request **`SCHEDULE_EXACT_ALARM`**, not `USE_EXACT_ALARM`. `SCHEDULE_EXACT_ALARM` is explicit user-controlled Special App Access and fits Arihna's opt-in alarm feature; Arihna must not claim the narrower alarm-clock/calendar entitlement implied by `USE_EXACT_ALARM` merely to bypass user approval.
- Do not request the special access at app startup. When the user explicitly enables the first alarm (or taps a capability CTA), explain why exact timing is needed and then open the system `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` flow where supported.
- If exact-alarm access is absent/revoked, retain the user's configured rule but do not call an exact API and do not show it as scheduled. Surface `NeedsExactAlarmAccess` with a recovery CTA. No silent inexact fallback is approved in STEP 1 because a late alarm must not be presented as exact/reliable.
- Handle `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` by rechecking `canScheduleExactAlarms()` and reconciling current rules. Do not assume there is a symmetric reliable revoke broadcast; foreground/resume reconciliation must re-check capability because revocation cancels scheduled exact alarms.
- API 28 remains supported and uses the exact path without Android 12+ Special App Access.
- No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, no broad battery-whitelist prompt, no wake lock, no background polling loop and no always-on foreground service are approved by STEP 1.

Current Android platform evidence reviewed for this decision (2026-09-04): Android Developers `Schedule alarms` documents `SCHEDULE_EXACT_ALARM`, `canScheduleExactAlarms()`, the permission-state-change broadcast and exact-alarm behavior; Android 14 behavior changes document that `SCHEDULE_EXACT_ALARM` is denied by default for most fresh installs targeting Android 13+.

#### Notification permission and delivery contract

Alarm delivery in this milestone is a user-visible notification/alarm alert, not silent background work.

- On API 33+ declare `POST_NOTIFICATIONS`, but request it only after an explicit user action to enable alarm delivery and after a concise rationale. Never request it automatically on first launch.
- If notification permission is denied, preserve the rule but do not represent it as deliverable/scheduled; expose `NeedsNotificationPermission`. Arihna must not schedule invisible background alarm firings that the user cannot receive as an alarm notification.
- At least two semantic notification families are required: prayer alarms and custom alarms. Channel ids must be stable and migration/versioning safe.
- Configurable sound is represented by an Arihna-owned `AlarmSoundProfile` reference whose Android delivery maps to immutable notification-channel sound configuration. The concrete profile catalog/UI is **Pending** until STEP 5, but it must support at least system-default alarm sound and silent. Bundled adhan playback, long-form audio, media playback services and imported user audio are explicitly outside this milestone unless separately specified.
- No full-screen intent, lock-screen takeover or alarm-clock UI is approved in STEP 1. A later requirement for those behaviors needs a separate spec decision and Android-policy review.
- Notification text may identify the prayer/custom label and scheduled time but must not fabricate religious quotations or authoritative content.

Android Developers' notification-permission guidance reviewed for STEP 1 confirms `POST_NOTIFICATIONS` is a runtime permission on API 33+ and user denial prevents ordinary app notifications.

#### Prayer-linked scheduling semantics

Prayer-linked alarms consume the existing `PrayerScheduleRepository` output exactly.

- Use the selected location and `ZoneId` already embedded/provenanced by the authoritative Prayer schedule. Do not call `ZoneId.systemDefault()` to reinterpret a Manual/remote prayer city.
- A `Ready(CACHED)` Device location remains valid exactly as Prayer Schedule already defines it; the alarm layer must not trigger a fresh location request merely to schedule alarms.
- If Prayer Schedule is non-Ready/unavailable, keep prayer rules persisted but unscheduled and surface the controlled reason. Never substitute a default city, stale UI city, device timezone or fabricated prayer time.
- Applying a signed prayer offset may cross midnight; resolve the resulting instant in the same selected prayer `ZoneId` while preserving the schedule's source date/time semantics.
- Any accepted Prayer Schedule change caused by selected Location, Prayer settings, local date or timezone invalidates outstanding prayer occurrences and triggers idempotent reconciliation. Stale occurrences must fail the occurrence-token validation at delivery.
- Schedule only future occurrences. If the resolved time for today's target has passed, choose the next authoritative occurrence from the existing today/tomorrow schedule semantics; do not invent a multi-day prayer calculator inside the alarm layer.

#### Custom-alarm time semantics

Custom alarms intentionally follow the **device** civil timezone because they are personal wall-clock alarms and are independent of the selected prayer city.

- Recompute custom next-occurrence time after device timezone or wall-clock changes.
- For a DST spring-forward gap, resolve a nonexistent requested local time to the first valid instant after the gap and retain the user's displayed wall-clock rule unchanged for future recurrences.
- For a DST fall-back overlap, fire once using the earlier valid offset unless a later product decision explicitly chooses otherwise; never fire the same recurrence twice solely because the local clock repeats.
- One-shot custom alarms auto-disable only after a validated successful delivery occurrence; stale/ignored occurrences must not disable the rule.

#### Reconciliation and reboot/time-change events

OS alarms are ephemeral scheduling state and must be reconstructed from persisted rules.

Reconcile on at least:

1. app/process bootstrap or foreground resume;
2. rule create/update/delete/enable/disable;
3. authoritative Prayer Schedule change;
4. validated alarm delivery, to schedule the next recurrence;
5. `BOOT_COMPLETED` after device reboot;
6. system wall-clock/timezone change broadcasts relevant to the current Android API;
7. exact-alarm special-access grant/change after rechecking capability;
8. notification-permission recovery when the app returns to foreground.

A later technical step may add only the minimal manifest receivers/normal permissions required for these events (`RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`) and must keep receivers explicit/non-exported unless Android requires otherwise for a system broadcast. No receiver may perform long-running arbitrary work; use `goAsync`/bounded coroutine work for the small offline persistence/schedule reconciliation path, and introduce WorkManager only if a separately demonstrated deferrable need exists.

#### Samsung / standby reliability policy

`setExactAndAllowWhileIdle` is selected because prayer/custom alarms are user-intentioned time-sensitive events and must retain exact behavior through Doze when Android permits it. OEM behavior still requires physical verification on the primary Samsung Galaxy S25.

- Do not ask the user to disable battery optimization globally as a default requirement.
- Surface an honest troubleshooting note for Samsung sleeping/deep-sleep restrictions only if physical testing demonstrates a relevant limitation.
- STEP 7 hardware validation must include at least screen-off delivery, a short idle/Doze-oriented case feasible on-device, reboot/reschedule, permission denial/recovery and one prayer-linked plus one custom occurrence. Long-duration overnight evidence may be added where practical, but no pass may be inferred from emulator behavior alone.

#### STEP 1 test contract for later implementation

Pure/JVM coverage must eventually prove at least:

- custom one-shot next occurrence and auto-disable after validated delivery;
- daily/weekday recurrence ordering;
- device-zone change recomputation;
- DST gap and overlap rules above;
- prayer rule consumes exact existing schedule output and selected `ZoneId` without recalculating Prayer or Location;
- prayer offset crossing midnight;
- non-Ready Prayer Schedule produces configured-but-unscheduled state with no fabricated time;
- rule update/cancel/reconcile is idempotent;
- stale occurrence token cannot deliver or disable a newer rule;
- capability matrix for notification denied / exact access denied / ready;
- no silent inexact fallback when exact access is absent.

Android instrumentation must eventually prove at least:

- API28 exact scheduling/cancel `PendingIntent` identity path works without Special App Access and does not crash;
- notification channels/receiver path works on API28;
- boot/time-change receivers invoke reconciliation without duplicate delivery;
- a modern Android API (prefer API36 to match the Galaxy S25 runtime when available in CI) exercises `canScheduleExactAlarms()` denied/granted handling and API33+ notification-permission state;
- no ignored/skipped Alarm tests at the definitive gate.

Every technical Alarm step must preserve unfiltered existing regression: `testDebugUnitTest`, `assembleDebug`, Android 9/API28 `connectedDebugAndroidTest` with zero skipped tests, frozen GeoNames SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`, `applicationId = com.archimedeprojects.arihna`, and established permission/dependency policy outside the explicitly authorized Alarm permissions. A technical candidate is valid only after a full gate on its exact SHA and promotion to `main` by non-forced fast-forward.

#### STEP 2 closure / STEP 3 authorization — APPROVED 2026-09-04

Alarms STEP 2 domain + persistence is **CLOSED** on exact technical commit `7579490c58e354a5423e76d4d8ad4e1f7749dfac`, built directly above STEP 1 spec commit `4fdbc9b68e1e6637133b6e68ef73a194b78aa92a`. Definitive full gate run `33888892314` / job `101075412204` checked out that exact SHA, restored and verified the frozen GeoNames asset, passed unfiltered `testDebugUnitTest`, passed `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **53/53 tests, 0 failed and 0 skipped**. The implementation added only Arihna-owned Alarm domain/persistence/pure resolver code, shared-DataStore wiring and focused tests. No `AlarmManager`, manifest alarm permission/receiver, Prayer integration, notification delivery or Compose UI was introduced.

STEP 3 Android scheduler + capability is now authorized with this exact scope:

- Introduce `AlarmPlatformScheduler`/equivalent as the only owner of Android `AlarmManager` and `PendingIntent` mechanics. Production scheduling is a single future `RTC_WAKEUP` occurrence using `setExactAndAllowWhileIdle` on API 23+; no repeating API and no inexact fallback.
- On API 31+ the scheduler must check `canScheduleExactAlarms()` before any exact call and return an explicit `NeedsExactAlarmAccess`/equivalent result when denied. API28 remains `Ready` without Special App Access. Provide an Arihna-owned capability reader and a settings-intent factory for `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; STEP 3 must not launch that settings flow automatically because there is still no alarm UI.
- Manifest changes are limited to `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED` plus the minimal explicit receiver declarations needed by this step. `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, battery-optimization exemptions, wake locks, foreground services and new dependencies remain forbidden in STEP 3.
- A scheduled occurrence uses an explicit `AlarmOccurrenceReceiver` `PendingIntent` whose identity is stable per `alarmId`, while extras carry `alarmId`, rule revision, trigger epoch and occurrence token. Cancel uses the same stable identity. Stale extras must never change the identity of another alarm.
- Add a minimal `AlarmSystemEventReceiver`/equivalent for `BOOT_COMPLETED`, device wall-clock/timezone changes, `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, and app replacement where supported. Receiver work must be bounded via `goAsync`/coroutine delegation. STEP 3 may expose the reconciliation trigger contract, but actual rule/Prayer reconciliation is STEP 4 and must not be silently implemented here.
- Occurrence envelope decoding/validation may be implemented and unit-tested in STEP 3, but there is **no user-visible delivery** yet: no notification channel, sound, full-screen UI or notification permission. No production code may schedule an alarm merely at app startup in this step; scheduling is exercised only through the platform boundary/tests until STEP 4/5 provide real callers.
- Preserve `applicationId = com.archimedeprojects.arihna`, existing COARSE-only Location policy, Prayer/Qibla behavior and all existing persistence namespaces. No change to Prayer calculation, Prayer schedule, Location/cache, Qibla, or their UI.

Required STEP 3 tests: pure/JVM fake-gateway coverage proving API28-ready/API31+-denied capability behavior, exact-call-only when allowed, zero schedule call when exact access is denied, no inexact fallback, deterministic `PendingIntent` identity/envelope and cancel symmetry; Android 9/API28 instrumentation proving manifest/component availability and the exact scheduling/cancel path does not crash without Special App Access. Modern-API denied/granted emulator coverage remains mandatory at the definitive STEP 6 gate, with focused unit/API policy coverage added now.

STEP 4 is not started by this authorization. STEP 3 must pass the full exact-SHA regression gate before non-forced promotion to `main`.

#### STEP 3 closure / STEP 4 authorization — APPROVED 2026-09-04

Alarms STEP 3 Android scheduler + capability is **CLOSED** on exact technical commit `a87835850bd7f7659909504f76ac1012723f028e`, built directly above STEP 3 authorization commit `e9e1f3262b5383df4dc48f010c0f5dcd394ffec7`. Definitive exact-SHA gate rerun `33892642365` / job `101087773292` passed lineage/scope verification, frozen GeoNames integrity, unfiltered `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 connected regression with zero failed and zero skipped tests. The earlier run failure was CI-only after the emulator test body had passed; the rerun changed only the temporary gate workflow and validated the identical runtime SHA.

STEP 4 Prayer-linked reconciliation is now authorized with this exact scope:

- Add an Arihna-owned `AlarmReconciler`/equivalent that is the only orchestration layer translating persisted enabled `AlarmRule`s into the next exact `AlarmOccurrence` per rule and calling the already-closed `AlarmPlatformScheduler`. No direct `AlarmManager` access outside the STEP 3 platform adapter.
- Prayer-linked rules must consume prayer times from the already-closed Prayer schedule boundary; do not duplicate or modify Adhan calculation, Prayer settings, Location selection/cache, timezone semantics or Qibla behavior. Map `AlarmPrayer` deterministically to the corresponding `PrayerTimes` instant and apply only the persisted alarm offset through the already-closed occurrence resolver.
- Custom rules continue to use the already-closed device-wall-clock/DST resolver. The reconciler may accept the current device `ZoneId` as an injected provider; it must not use a prayer-location zone for custom alarms.
- Reconciliation is authoritative and idempotent: for each persisted rule, cancel when disabled/unresolvable, otherwise schedule exactly one next occurrence. A rules snapshot must never create more than one platform schedule per alarm id. Exact-access denial is surfaced as a controlled result and must not fall back to inexact scheduling.
- System-event reconciliation from the STEP 3 `AlarmSystemEventReceiver` must invoke the real reconciler after boot, wall-clock/timezone change, exact-alarm access change and app replacement. Work remains bounded via the existing `goAsync` coroutine path. No user-visible notification/delivery is introduced in STEP 4.
- App/foreground reconciliation may be wired only where needed to keep schedules current after persisted rule, Prayer schedule, Location or Prayer-setting changes. It must not request Location permission, acquire a new fix merely for alarms, or create an app-wide live sensor/location subscription. Persisted accepted Location data is the only background-safe input.
- Occurrence delivery remains deliberately silent in STEP 4. `POST_NOTIFICATIONS`, notification channels, sound playback, UI, full-screen intents, foreground services, wake locks, battery-optimization exemptions and new dependencies remain forbidden.
- Preserve `applicationId = com.archimedeprojects.arihna`, current COARSE-only Location policy, `SCHEDULE_EXACT_ALARM` + `RECEIVE_BOOT_COMPLETED`, and all established persistence namespaces.

Required STEP 4 tests: pure/JVM coverage for Prayer-name→PrayerTimes mapping, offsets before/after prayer, disabled/unresolvable cancellation, exactly-one schedule per rule, exact-access denial propagation, custom-rule device-zone semantics, deterministic reconciliation of mixed Prayer/custom snapshots and idempotent repeated reconciliation; Android API28 regression must keep the STEP 3 receiver/scheduler coverage green with zero ignored/skipped Alarm tests. The exact technical candidate must pass the full exact-SHA gate before non-forced promotion.

STEP 5 is not started by this authorization.

#### STEP 4 closure / STEP 5 authorization — APPROVED 2026-09-04

Alarms STEP 4 Prayer-linked reconciliation is **CLOSED** on exact technical commit `82ca7754494bb537b9b69695193c92b4f4f1fe48`, built directly above STEP 4 authorization commit `91598b513c51f86010159380be6d4700be3f5784`. Definitive exact-SHA gate run `33894459758` / job `101093710372` checked out that exact runtime, restored and verified frozen GeoNames, passed unfiltered `testDebugUnitTest`, passed `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **57/57 tests, 0 failures, 0 errors, 0 skipped**. The implementation adds only Alarm reconciliation/orchestration plus focused tests and composition-root wiring; Prayer calculation, Location acquisition/cache semantics, Qibla, permissions and dependencies outside the previously-approved Alarm scope remain unchanged.

STEP 5 functional Alarm UI + notification delivery is now authorized with this exact scope:

- Add `POST_NOTIFICATIONS` to the manifest for API33+ delivery. Arihna must never request it automatically at startup: the runtime permission prompt is launched only after an explicit user action inside the Alarm screen, with a concise rationale. API32 and below remain notification-ready without a runtime permission.
- Exact-alarm special access remains a separate capability. The existing `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` intent may be launched only from an explicit Alarm-screen action. Rules may remain persisted/enabled when either capability is missing, but they must not remain platform-scheduled and the UI must state that user action is required. No inexact fallback is permitted.
- Introduce one Arihna-owned notification-delivery boundary. A validated occurrence may post a normal notification only when the envelope still matches the current enabled rule revision/token and notification permission is available. Stale, malformed, deleted or disabled occurrences produce no user-visible delivery. No full-screen intent, lock-screen takeover, foreground service, wake lock or battery-optimization exemption.
- Freeze two sound profiles already present in the domain: `SYSTEM_DEFAULT` uses the platform alarm/default sound on an Arihna Alarm notification channel; `SILENT` uses a separate silent Arihna Alarm channel. No bundled Adhan audio, media playback service, custom-file picker or audio catalogue belongs to STEP 5.
- After a validated delivery, a one-shot custom rule (`weekdays` empty) is atomically disabled through the already-closed repository revision check; recurring custom and Prayer-linked rules remain enabled. Reconciliation then computes/schedules the next occurrence. If notification delivery is blocked, do not mark a one-shot as delivered.
- Replace the Alarm placeholder with a compact functional Compose screen. It must show exact-alarm and notification capability status, explicit actions to grant missing access, all five Prayer alarms with enable/disable controls and persisted sound profile, and a minimal Custom alarm creator with label + local `HH:mm`; a custom alarm with no selected weekdays is a one-shot, while optional weekday selection may be exposed without changing the closed domain model. Existing rules must support enable/disable, sound-profile change and delete.
- All rule mutations must persist first and then run the STEP 4 reconciler. The screen must not request Location, run Prayer formulas, poll in background, subscribe to Qibla sensors or fabricate delivery/scheduling success.
- Keep the existing app shell and bottom navigation. UI styling stays within the existing Arihna Green/Gold/Off-white theme and should fit the primary Galaxy S25 compactly without requiring unnecessary scrolling for the capability header and Prayer controls.
- Preserve `applicationId = com.archimedeprojects.arihna`, COARSE-only Location policy, existing `SCHEDULE_EXACT_ALARM` + `RECEIVE_BOOT_COMPLETED`, frozen GeoNames, Prayer/Qibla behavior, current dependencies and DataStore namespace. No new dependency is authorized.

Required STEP 5 tests: JVM coverage for notification-permission gating, no scheduling when delivery capability is missing, valid-vs-stale occurrence handling, one-shot disable only after successful validated delivery, recurring reschedule, and rule-mutation→reconcile behavior; Compose/API28 coverage for capability status, five Prayer controls, custom creation/list state and absence of fabricated permission success; API28 instrumentation for notification-channel creation/delivery compatibility. The technical candidate must pass the full exact-SHA gate with zero ignored/skipped tests before non-forced promotion.

STEP 6 is not started by this authorization.

#### STEP 5 closure / STEP 6 definitive regression + package authorization — APPROVED 2026-09-05

Alarms STEP 5 functional UI + notification delivery is **CLOSED** on exact technical runtime `310413e886e9d7d54ea6f86c3fc7849360069c49` (`feat(alarms): add functional alarm ui and notification delivery`), built directly above STEP 5 authorization commit `864514bad53e4d7e3cbeaa2efaf486efcb3d99c1`. Definitive exact-SHA gate run `33952510472` / job `101269810089` verified the exact lineage and approved 15-file scope, restored frozen GeoNames, passed the unfiltered JVM regression, passed `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **62/62 tests, 0 failures, 0 errors, 0 skipped**. The gate also reverified `applicationId = com.archimedeprojects.arihna`, COARSE-only Location policy, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, absence of `USE_EXACT_ALARM`, FINE/BACKGROUND Location and battery-optimization exemption, unchanged dependencies, and zero ignored Alarm tests.

STEP 6 is now authorized as **validation + packaging only for the same runtime `310413e886e9d7d54ea6f86c3fc7849360069c49`**. This authorization does not permit new product behavior, UI, alarm semantics, Prayer calculation, Location/cache, Qibla, persistence, dependency or manifest-policy changes. If any required gate reveals a runtime defect, STEP 6 stops and a separately spec-authorized corrective candidate must again be a direct child of its corrective spec commit; packaging must never silently include an unreviewed fix.

Required STEP 6 evidence:

- **Exact-runtime regression:** check out exactly `310413e886e9d7d54ea6f86c3fc7849360069c49`; prove its parent is STEP 5 spec `864514bad53e4d7e3cbeaa2efaf486efcb3d99c1`; restore and verify GeoNames SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`; run unfiltered `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest`; require zero failures/errors/skipped and retain the STEP 5 Alarm test families in the connected result.
- **Modern exact-alarm gate:** run focused instrumentation on a stable Android 16/API36 emulator. Establish and verify both denied and granted `SCHEDULE_EXACT_ALARM` states using deterministic emulator/system tooling, and assert the corresponding real `AlarmManager.canScheduleExactAlarms()` state. In denied state Arihna must surface the controlled blocked capability and make no exact scheduling call; in granted state the existing exact `PendingIntent` schedule/cancel path must execute without `SecurityException`. No inexact fallback, `USE_EXACT_ALARM`, battery-whitelist exemption, wake lock or foreground service is allowed.
- **Modern notification gate:** on the same API36 image, explicitly verify `POST_NOTIFICATIONS` denied and granted states. Denied state must produce controlled non-delivery and must not mark a one-shot as delivered; granted state must allow channel-backed notification delivery. Verify both stable Arihna Alarm channel families (`SYSTEM_DEFAULT` and `SILENT`) and that the silent channel has no sound. No bundled audio/media-service path is introduced. The current Android 17 background-audio hardening does not justify adding a playback service because STEP 5 delivery remains NotificationManager/channel-backed rather than app-managed background media playback.
- **Current-target policy:** keep `compileSdk = 37` and `targetSdk = 37`; review Android 17/API37 behavior changes for regressions relevant to Alarms, but do not change runtime solely to exercise unrelated platform changes. The mandatory modern automated permission/capability matrix for this step remains API36 stable; physical-device behavior is reserved for STEP 7.
- **APK identity and signing:** package that exact runtime with the persistent Arihna debug signing material already used by Galaxy S25 validation APKs. Verify package/application id exactly `com.archimedeprojects.arihna`; signer certificate SHA-256 exactly `13:97:00:8C:1F:96:2D:BB:D3:6D:D8:A8:EA:02:16:AF:DD:06:E4:B2:B3:E0:8B:C0:F6:D4:B5:43:44:D7:B0:FA`; keystore SHA-256 exactly `bc9057f26ad6de7efb70a5df06effb1ed259f1d015c3f062d0f757b3f0983b72`; record the final APK byte size and SHA-256 after signing.
- **Prerelease handoff:** publish a GitHub **prerelease**, not a final release, whose tag/release target resolves to exact runtime `310413e886e9d7d54ea6f86c3fc7849360069c49`, with one clearly named installable APK asset for Galaxy S25 validation. Verify release id, tag, target SHA, asset id/state/size and direct asset URL after upload. The APK built for prerelease must be the same signed artifact whose SHA-256 was recorded by the package gate.
- **Stop point:** STEP 6 ends after all gates are green and the verified prerelease APK/link is available. Do not perform or claim the physical result automatically. STEP 7 begins only with the user's Galaxy S25 installation/reliability validation and explicit Pass/Fail evidence; no documentation-only milestone closure may precede that physical result.

Current Android evidence re-reviewed for STEP 6 on 2026-09-05: Android Developers `Schedule alarms` and Android 14 exact-alarm behavior guidance confirm that fresh installs targeting Android 13+ do not receive `SCHEDULE_EXACT_ALARM` by default, that apps must check `canScheduleExactAlarms()` before exact scheduling, and that user/system revocation cancels future exact alarms; Android Developers `Notification runtime permission` confirms `POST_NOTIFICATIONS` runtime gating on API33+; Android 17/API37 behavior documentation was reviewed because Arihna targets API37, including the new background-audio restrictions, with no authorization to expand STEP 5's channel-backed notification delivery into app-managed background media playback.

STEP 7 is not started by this authorization.

#### Alarms milestone sequence

1. **STEP 1 — spec-first architecture/policy: CLOSED.** No runtime change.
2. **STEP 2 — domain + persistence: CLOSED.** Exact runtime `7579490c58e354a5423e76d4d8ad4e1f7749dfac`; gate `33888892314` / `101075412204`, API28 53/53, 0 failed, 0 skipped.
3. **STEP 3 — Android scheduler + capability layer: CLOSED.** Exact runtime `a87835850bd7f7659909504f76ac1012723f028e`; definitive gate rerun `33892642365` / `101087773292`; exact scheduling/capability/receiver layer only, with no user-visible delivery.
4. **STEP 4 — Prayer-linked reconciliation: CLOSED.** Exact runtime `82ca7754494bb537b9b69695193c92b4f4f1fe48`; gate `33894459758` / `101093710372`; API28 57/57, 0 failed, 0 skipped.
5. **STEP 5 — functional alarm UI + notification/sound profiles: CLOSED.** Exact runtime `310413e886e9d7d54ea6f86c3fc7849360069c49`; definitive gate `33952510472` / `101269810089`; API28 62/62, 0 failed, 0 skipped.
6. **STEP 6 — definitive regression/package gate: AUTHORIZED / IN PROGRESS.** Same exact STEP 5 runtime only; API28 + API36 permission/capability matrix, identity/signing checks and persistent-debug Galaxy S25 prerelease.
7. **STEP 7 — Galaxy S25 reliability validation + documentation-only closure: NOT STARTED.** Hardware validation first; closure commit after explicit physical PASS, with no runtime change.

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
- 30s timeout / 5 km / 15 min; timezone change significant.
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

### 2026-09-02 — Cache-first foreground re-entry approved; provider decision deferred

Real-device validation of prerelease `location-cache-fallback-s25-validation-685f3733-20260902` on the Samsung Galaxy S25 successfully exercised the CACHED path: the current framework fused attempt did not yield the selected fresh fix, while Arihna recovered and used the persisted provenance-safe Device fix and exposed it as CACHED. Commit `685f37336fffc1cbfcd7169b74e2f0866c23498c` is therefore the validated production baseline for this follow-up correction.

The next Location behavior correction is **cache-first foreground re-entry**. When Device mode is selected and a valid complete persisted `DeviceLocationFix` exists, an app foreground transition must make that fix immediately available as `LocationResolutionState.Ready(... CACHED)` so Home/prayer calculation is not blocked behind a new current-location attempt. Arihna may still start the existing framework fused current-location attempt on foreground, but that attempt is a **non-blocking revalidation**: the already usable CACHED state remains authoritative while acquisition is pending. A successful current fix may replace/update the selected Device fix only through the existing acceptance policy; a current fix that does not satisfy the existing significant-change rules must not silently alter those rules. Timeout, `null`, provider failure or cancellation of the revalidation must leave the already displayed CACHED state usable rather than regress the UI to Resolving/Unavailable solely because revalidation failed.

This correction does **not** add a freshness TTL or suppress foreground revalidation yet. The question of whether a sufficiently recent persisted/verified fix should skip a later `ON_START` current attempt remains a separate decision. The existing 30-second current budget, 5 km significant-movement threshold, timezone-change significance, 15-minute foreground update stream, COARSE-only permission policy, provenance-safe historical ZoneId rule, manual-location behavior and Prayer calculation boundary remain unchanged.

The choice between Android framework LocationManager and Google Play Services `FusedLocationProviderClient` is explicitly **deferred**. No Play Services Location dependency, `ACCESS_FINE_LOCATION`, background location or foreground location service is authorized by this decision. A future provider change requires a separate spec decision and, if considered, should be supported by same-device comparative evidence rather than by assumption.

Implementation must remain spec-first and must not be promoted until the exact implementation SHA passes the complete unit regression, `assembleDebug`, Android 9/API28 `connectedDebugAndroidTest` with zero skipped tests and Prayer regression presence, plus the existing manifest/dependency and exact GeoNames asset checks. Real-device Galaxy S25 validation remains required before treating the cache-first lifecycle correction as closed.


### 2026-09-02 — Galaxy S25 intermittent current-location cache fallback approved

After the complete S25 diagnostic campaign, the prior network-first assumption is revoked and fused is not treated as deterministic: repeated same-session A/B runs showed both a genuinely current fused success and a later fused `null` at the framework's approximately 30-second terminal window, while network also returned `null`. Bounded request-updates probes for both providers delivered historical callbacks and failed to produce a fresh follow-up. A later production fused foreground callback demonstrated that a useful real cached fix may still exist while current acquisition fails. Approved correction is therefore Approach B: one framework fused current attempt with a 30-second coordinator-owned budget, followed on `null` or timeout by the newest valid real last-known/persisted Device fix, explicitly marked CACHED with original timestamp and no arbitrary expiry TTL. Prayer calculations may use that CACHED real location, Home must disclose its age and offer an on-demand `Aggiorna posizione` retry. Existing 5 km/ZoneId significance, 15-minute foreground updates, COARSE-only/no-Play-Services policy and no-invented-location discipline remain unchanged. Production implementation requires a new exact-SHA full gate before promotion.

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

#### Full Qibla compass instrumentation — APPROVED 2026-09-04

Galaxy S25 physical revalidation confirmed that the corrected live cardinal rose is directionally valid when the phone is free from nearby magnetic interference. The earlier apparent fixed-north error was reproduced while the charger was connected and disappeared after removing the charger; therefore the runtime correction at `e641cb44fea5ab6095c87ed681b883db8c7c00b5` remains valid and STEP 6 stays AUTHORIZED / IN PROGRESS.

The user explicitly approves an immediate full-feature compass presentation inspired by the validated Samsung compass reference while preserving Arihna branding and Qibla semantics. This is a Qibla STEP 6 enhancement, not STEP 7 closure.

Approved implementation contract:

1. Live Device mode shall present a full 360° compass dial with readable degree graduations, major 30° labels, cardinal labels N/E/S/O, a fixed screen-top heading index, and the existing yellow Qibla marker. The rose continues to rotate from authoritative true heading using shortest circular presentation paths; the Qibla marker continues to consume `relativeQiblaDirectionDegrees` unchanged.
2. Live Device mode shall prominently show the current true heading in degrees plus a compact cardinal sector label. This display is derived from `deviceHeadingTrueDegrees` only and must not alter bearing calculations.
3. Live Device mode shall show instrumentation cards for heading quality/estimated accuracy, selected heading source, magnetic declination when available, and magnetic-field intensity in microtesla when a magnetic-field sensor is available.
4. Magnetic-field intensity is diagnostic telemetry only. It must never be used to change Qibla bearing, heading-source selection, declination correction, smoothing, or sensor fusion. The magnetic sensor may be observed in parallel only while the existing lifecycle-bound Qibla heading collection is active, and must be unregistered with the same lifecycle ownership.
5. The UI shall surface an interference advisory when magnetic-field intensity is implausibly high or low for normal ambient use, and shall always include concise guidance to keep the phone approximately horizontal and away from chargers, metal, magnets, cases/accessories with magnets, and other interference sources. The UI must not claim that intensity alone proves heading accuracy.
6. When the selected heading source is magnetic-reference, show the authoritative geomagnetic declination already computed by the heading layer. When `TYPE_HEADING` supplies true north directly, declination may be shown as not applicable/unavailable rather than fabricated.
7. Manual mode remains strictly static: north-referenced Qibla diagram, prominent `Bussola statica` explanation, no live heading, magnetic telemetry, or physical-alignment claim.
8. Preserve the existing source hierarchy, true-north correction, Qibla bearing math, Location/cache behavior, Prayer behavior, persistence, permissions, dependencies, manifest policy, and lifecycle ownership. No FINE/BACKGROUND/BODY_SENSORS permission and no Google Play Services Location dependency may be introduced.
9. Add focused host tests for heading/cardinal presentation and diagnostic classification; update Compose/API28 tests for full dial, instrumentation cards, declination availability semantics, interference guidance, and Manual non-live behavior. Existing regressions remain mandatory with zero skipped tests.
10. The exact technical candidate must pass unfiltered `testDebugUnitTest`, `assembleDebug`, Android 9/API28 `connectedDebugAndroidTest`, GeoNames integrity, and existing permission/dependency policy gates before any promotion to `main`. After promotion, produce a new persistent-debug-signed Galaxy S25 prerelease APK and physically revalidate the full compass with the charger disconnected and with normal ambient magnetic conditions. STEP 6 is not CLOSED until that physical validation passes.

#### Galaxy S25 compact single-viewport Qibla layout — APPROVED 2026-09-04

Physical Galaxy S25 validation of the full-compass prerelease on runtime `b0017a6e5964ebdee9b6e6545a3356d7dce83f0a` is positive for the compass concept and instrumentation, but the user rejects the resulting vertically scrollable Qibla page. STEP 6 therefore remains AUTHORIZED / IN PROGRESS for a presentation-only compact-layout correction before closure.

Approved correction:

1. On the primary Galaxy S25 portrait viewport, normal `LiveCompass` content must fit as a compact single-screen instrument without requiring vertical scrolling. The compass remains the dominant element and the visual hierarchy should match the previously approved/generated compact Samsung-inspired Arihna concept as closely as possible using the available specification rather than inventing a new visual direction.
2. Preserve the full 360-degree dial, degree graduations, major 30-degree labels, N/E/S/O, fixed screen-top heading index, yellow Qibla marker, current true heading/cardinal sector, selected location, true-north Qibla bearing, Device freshness, heading quality/accuracy, source, magnetic declination and magnetic-field intensity.
3. Compress secondary telemetry instead of deleting it: use compact rows/chips/two-column instrumentation or equivalent dense Arihna treatment, reduced redundant explanatory copy and tighter spacing. Avoid a vertical stack of large diagnostic cards in the normal healthy state.
4. Interference/calibration guidance remains honest and visible when actionable, but should use a compact high-priority banner/row rather than forcing a long page. Routine precision guidance may be condensed to a short line/icon treatment. Magnetic intensity remains diagnostic only.
5. Manual mode remains clearly static with the prominent `Bussola statica` meaning preserved; it must not expose live heading/telemetry or imply physical alignment. Other controlled Qibla states remain usable without fabricated values.
6. This correction is presentation-only. Do not change Qibla bearing/relative-direction mathematics, heading source hierarchy, declination, sensor fusion, magnetic diagnostic thresholds, Location/cache behavior, Prayer behavior, persistence, permissions, dependencies, manifest policy or lifecycle ownership.
7. Focused Compose/API28 tests must verify the compact live hierarchy and retained instrumentation without relying on scrolling to reach normal live telemetry, plus Manual non-live behavior and interference visibility. Existing host/unit, `assembleDebug`, Android 9/API28 connected, GeoNames, permission/dependency and no-skipped-test gates remain mandatory.
8. The exact clean technical candidate must pass the full gate before non-forced promotion to `main`, followed by a new persistent-debug-signed Galaxy S25 prerelease. STEP 6 closes only after physical confirmation that the compact screen is satisfactory and the already-validated compass behavior remains correct.

### Qibla sacred gold banner — APPROVED 2026-09-04

Galaxy S25 review of compact runtime `7d88b83b423de353a399c07ab80ccaf247b4b45d` confirmed the single-viewport layout but exposed insufficient contrast for the primary Qibla value. The user explicitly approved generated concept **variant 5 — Banner islamico** as the visual target. Reference: `docs/qibla/reference/qibla-banner-variant-5-approved.jpg`.

Approved contract:
- Normal LiveCompass uses one compact premium horizontal gold banner immediately above the compass, replacing the separate low-contrast Qibla value plus standalone heading row.
- Banner content: small geometric Kaaba mark at left, prominent high-contrast `Qibla N°`, current true heading/cardinal beneath, subtle mosque/minaret silhouette ornament. No sacred text, Arabic calligraphy, network image or religious claim.
- Use Arihna Gold/warm derived gold and Arihna Green high-contrast foreground, rounded premium geometry, matching variant 5 as closely as practical in native Compose.
- Preserve Galaxy S25 portrait single viewport with no vertical scrolling and preserve full dial, top index, yellow marker, location/freshness and all telemetry/guidance.
- Manual/static semantics remain truthful; no live-alignment claim in Manual mode.
- Presentation only: no Qibla math, north correction, heading hierarchy/smoothing, geomagnetic, Location/cache, Prayer, persistence, permissions, dependencies, manifest or lifecycle changes.
- API28 Compose coverage must verify the gold banner, Qibla value, live heading and Kaaba mark while existing regressions remain mandatory with zero skipped tests.
- Technical implementation commit directly follows this spec commit; exact full gate is required before non-forced main promotion and persistent-debug S25 prerelease. STEP 6 remains open pending physical validation.


#### Galaxy S25 final Qibla WOW polish — APPROVED 2026-09-04

Physical Galaxy S25 validation of the sacred-gold banner runtime passed the functional STEP 6 checks: the live rose rotates smoothly through a full 360° including north wrap, the true heading changes coherently, the yellow Qibla marker preserves the validated direction, lifecycle behavior remains normal, and the compact screen remains a single portrait viewport. The user approved one final presentation-only refinement before STEP 6 closure.

Approved visual target:

1. Preserve the compact sacred-gold live banner, Kaaba mark, large high-contrast `Qibla <bearing>°`, and live true-heading line exactly as functional content.
2. Replace the abstract right-side banner decoration with a refined tone-on-tone Islamic skyline inspired by the approved visual reference: layered mosque domes, slim minarets and a crescent, kept decorative and subordinate to the bearing text. No generated Arabic text or sacred scripture is introduced.
3. In the live compass rose, render only the geographic `N` cardinal in Arihna Gold `#D4AF37`; `E/S/O` and numeric graduations remain neutral. Gold is chosen instead of red so north reads as a premium navigational anchor rather than an alert/error state.
4. The skyline and gold `N` are presentation-only. Do not change Qibla bearing mathematics, relative-direction math, true-heading/declination processing, sensor-source hierarchy, smoothing, magnetic-field diagnostics, Location/cache semantics, Prayer behavior, persistence, permissions, dependencies, manifest policy, or lifecycle ownership.
5. Keep the Galaxy S25 portrait LiveCompass screen within one normal viewport with no vertical scrolling. Preserve all existing telemetry and interference guidance.
6. Add focused Compose/API28 assertions that the approved skyline container is present in LiveCompass and that the north-cardinal presentation has its dedicated semantic/test hook. Existing full unit, assembleDebug, Android 9/API28 connected regression, frozen GeoNames, permission/dependency/no-skips and exact-SHA gates remain mandatory.
7. After a clean exact-SHA candidate passes the full gate, promote by non-forced fast-forward and publish a new prerelease APK using Arihna's persistent debug signing identity. STEP 6 closes only after physical confirmation of this final presentation build; STEP 7 remains NOT STARTED until then.
