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

### 5.1 Prayer times — calculation architecture APPROVED

Core behavior:

- Calculate daily Islamic prayer times from coordinates, civil date, timezone and user calculation settings.
- Core calculation is fully offline and requires no cloud/network service.
- GPS/location acquisition is a later milestone and is **not** part of the calculation-engine milestone.
- Recalculation/update scheduling is a later milestone.
- Prayer notifications/adhan reminders are later milestones.

#### Selected engine

- **Library:** Adhan Kotlin Multiplatform
- **Artifact:** `com.batoulapps.adhan:adhan2:0.0.7`
- **Version:** `0.0.7`
- **License:** MIT
- Pure Kotlin/KMP dependency usable on Android; no JNI/native binding and no cloud/network dependency for calculation.
- Arihna must not expose Adhan-specific types outside the adapter implementation.

Architecture:

```text
Arihna domain
    ↓
PrayerTimeCalculator
    ↓
AdhanPrayerTimeCalculator
    ↓
Adhan 0.0.7
```

#### Domain models

Create Arihna-owned models, independent of Adhan:

- `Coordinates(latitude, longitude)` with validation latitude `[-90, 90]`, longitude `[-180, 180]`.
- `PrayerCalculationMethod`.
- `AsrMethod`.
- `HighLatitudeRule`.
- `PrayerTimeAdjustments`.
- `PrayerCalculationSettings`.
- `PrayerTimes` using `java.time.Instant` for Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha.
- `PrayerDay` containing `LocalDate`, `ZoneId`, coordinates, settings and times.
- `PrayerCalculationResult` with explicit success/unavailable outcomes.

`PrayerCalculationResult.Unavailable` must be used for controlled failures such as invalid inputs or unavailable astronomical events. Never fabricate `00:00`, copy another day, silently substitute another location/method, or let upstream calculation exceptions crash the app.

#### Supported calculation methods

Expose these 11 Arihna methods:

1. Muslim World League (MWL)
2. Umm al-Qura University, Makkah
3. ISNA / North America
4. Egyptian General Authority of Survey
5. University of Islamic Sciences, Karachi
6. Dubai / Gulf
7. Kuwait
8. Qatar
9. Moonsighting Committee
10. Singapore
11. Turkey / Diyanet

Do not expose generic `OTHER` or arbitrary Fajr/Isha angle editing in this milestone.

#### Asr

- `STANDARD` → shadow factor 1; maps to Adhan Shafi setting and is the standard criterion commonly used by Shafi'i/Maliki/Hanbali.
- `HANAFI` → shadow factor 2.

The domain enum is intentionally named `AsrMethod`, not `Madhab`, because it configures the Asr shadow criterion.

#### High latitude handling

Arihna domain supports:

- `AUTOMATIC`
- `MIDDLE_OF_THE_NIGHT`
- `SEVENTH_OF_THE_NIGHT`
- `TWILIGHT_ANGLE`

Arihna-owned automatic rule:

```text
abs(latitude) > 48° → SEVENTH_OF_THE_NIGHT
otherwise          → MIDDLE_OF_THE_NIGHT
```

Use `abs(latitude)` so north/south behavior is symmetric. Pass an explicit resolved rule to the Adhan adapter rather than blindly relying on an upstream automatic heuristic.

For true polar/extreme cases where required astronomical events cannot be produced coherently, return `Unavailable` rather than inventing a religious fallback. `nearest day`, `nearest latitude`, `Makkah time`, fixed arbitrary intervals or equivalent fallbacks require a separate future religious/technical decision.

#### Timezone and DST

- `PrayerTimeCalculator` receives an explicit `java.time.ZoneId`; never hide `ZoneId.systemDefault()` inside the calculation engine.
- Domain uses `LocalDate`, `Instant` and `ZoneId` from `java.time`.
- DST and historical timezone offsets are handled by `ZoneId`/`ZoneRules`.
- Do not add manual “summer +1 hour” logic.
- Coordinate-to-timezone discovery is a later location milestone.

#### Manual prayer offsets

Model per-prayer integer-minute adjustments for Fajr, Sunrise, Dhuhr, Asr, Maghrib and Isha. Default is zero. The model/calculation behavior is implemented now; DataStore/settings UI are deferred.

#### Umm al-Qura Ramadan Isha rule — Android API 28 VERIFIED

Required behavior:

- outside Ramadan: Isha = Maghrib + 90 minutes;
- during Ramadan: Isha = Maghrib + 120 minutes.

Ramadan detection uses `java.time.chrono.HijrahChronology.INSTANCE`, the Umm al-Qura Hijrah chronology supplied by `java.time`.

The mandatory minimum-SDK verification gate was completed successfully on 2026-08-29 using a real Android instrumentation run on an Android 9 / API 28 x86_64 emulator in GitHub Actions (run `33245235911`). `connectedDebugAndroidTest` started and finished **2 tests** successfully on `test(AVD) - 9`.

The instrumentation verification confirmed:

- `HijrahChronology.INSTANCE` is available on API 28;
- chronology id is `Hijrah-umalqura`;
- its calendar type identifies an Islamic calendar;
- Gregorian `2024-02-20` resolves to Islamic month 8 (Sha'ban 1445 context);
- Gregorian `2024-03-20` resolves to Islamic month 9 (Ramadan 1445);
- Gregorian `2024-04-20` resolves to Islamic month 10 (Shawwal 1445);
- Gregorian `2016-06-15` resolves to Islamic month 9 (Ramadan 1437).

Therefore the Android API 28 gate is **CLOSED/PASSED** and Arihna may implement the approved Umm al-Qura 90/120-minute Isha rule using `HijrahChronology.INSTANCE`. Keep the instrumentation regression test in the project so this platform assumption remains testable.

#### Prayer calculation test plan

Automated tests must cover at least:

- golden-value regression cases for Raleigh (MWL and ISNA/Hanafi), Cairo (Egyptian), Makkah (Umm al-Qura), and Karachi;
- all Arihna calculation-method mappings;
- Standard Asr vs Hanafi Asr on the same date/location;
- manual positive/negative/zero per-prayer offsets;
- high-latitude Middle/Seventh/Twilight/Automatic cases;
- southern-hemisphere `abs(latitude)` automatic-rule behavior;
- polar/extreme case returning controlled `Unavailable` rather than an uncaught exception or fabricated time;
- DST around `Europe/Rome` transitions;
- Umm al-Qura Ramadan 90/120-minute behavior;
- invalid latitude/longitude inputs;
- API 28 instrumentation regression for `HijrahChronology.INSTANCE`.

Published minute-level golden values should use ±1 minute tolerance; internal mapping/offset invariants should be exact.

Reference golden cases approved for implementation include:

- Raleigh, 2015-12-01, MWL/Standard: Fajr 05:35, Sunrise 07:06, Dhuhr 12:05, Asr 14:42, Maghrib 17:01, Isha 18:26.
- Raleigh, 2015-07-12, ISNA/Hanafi: 04:42, 06:08, 13:21, 18:22, 20:32, 21:57.
- Cairo, 2020-01-01, Egyptian/Standard: 05:18, 06:51, 11:59, 14:47, 17:06, 18:29.
- Makkah coordinates 21.427009, 39.828685, `Asia/Riyadh`, 2016-01-05, Umm al-Qura/Standard: 05:38, 07:00, 12:26, 15:31, 17:52, 19:22.
- high-latitude regression at latitude 55.983226, longitude -3.216649, 2020-06-15: Middle Fajr 01:14 / Isha 01:14; Seventh Fajr 03:31 / Isha 22:56; Twilight Fajr 02:31 / Isha 23:50, with Sunrise 04:26, Dhuhr 13:14, Asr 17:46, Maghrib 22:01 for all three.
- polar/extreme regression around Utqiagvik, Alaska, 2018-01-01.
- DST regressions around `Europe/Rome` on 2026-03-29 and 2026-10-25.

This milestone must run at least:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The Android API 28 HijrahChronology gate has passed and the Ramadan-specific rule is now allowed to be implemented and regression-tested.

### 5.2 Qibla

- Determine Qibla bearing from current coordinates to the Kaaba.
- Use Android sensors for live compass direction and surface calibration/accuracy gracefully.
- Remain calculable offline once coordinates are available.

### 5.3 Alarms

- Custom alarms in addition to prayer-linked alarms.
- Configurable sounds.
- Prayer/custom alarm reliability must respect Android standby/doze restrictions.

### 5.4 Quran

- Arabic Quran text readable offline with surah navigation.
- Evaluate optional Italian translation for redistribution license, attribution, integrity, size and offline packaging.
- **Pending:** bundled vs optional pack vs omitted in v1.
- Religious text must come from a verified attributable source and never be silently altered.

### 5.5 Daily motivational content

Morning/evening offline-oriented content may include a practical action, reflection, Quran verse and/or authentic hadith. Content changes daily, avoids close repeats, and stores verifiable source metadata. Quran citations include surah/verse; hadith include collection/reference and authenticity/source information. Never fabricate quotations/references.

## 6. Permissions and Android behavior

### 6.1 Location

Request only when needed, explain purpose, support precise/approximate behavior and degrade gracefully when denied. Manual-location fallback may be added if separately approved.

### 6.2 Notifications

Request `POST_NOTIFICATIONS` on Android 13+ only when notifications are enabled/requested. Separate channels for prayer/adhan and daily content where useful.

### 6.3 Exact alarms / standby reliability

Evaluate `AlarmManager` exact alarms including `setExactAndAllowWhileIdle`, special access restrictions, and Samsung sleeping-app/battery caveats. Use WorkManager only for deferrable/non-exact work. Avoid broad battery-optimization exemptions unless justified.

## 7. Offline/data requirements

Maximize offline operation. Quran text, optional translation packs, curated daily content, settings, alarms, prayer calculation from saved coordinates/settings and Qibla bearing should work locally once required data exists. Core prayer calculation must not require network access.

## 8. Android technical architecture — bootstrap CLOSED

### 8.1 App architecture

- One `:app` module initially.
- Package-level separation rather than premature Gradle multi-module decomposition.
- Manual `AppContainer` DI; no Hilt/Koin for now.
- Feature-oriented packages plus core/domain/platform/data boundaries as needed.
- Lifecycle ViewModels/Flow and unidirectional Compose state when feature UI begins.

### 8.2 Approved dependencies/components

Bootstrap dependencies remain minimal. Prayer calculation milestone adds only the approved Adhan dependency `com.batoulapps.adhan:adhan2:0.0.7` plus test dependencies required for unit/instrumentation verification. Room, DataStore, WorkManager, location providers, alarms/notifications and Quran data remain deferred.

### 8.3 Data concepts

- `PrayerCalculationSettings`: method, Asr method, high-latitude rule, manual offsets.
- `PrayerDay`: computed times for a local civil date/timezone/coordinates.
- `LocationState`: later milestone.
- `Alarm`: later milestone.
- `QuranSurah` / `QuranAyah`: later milestone.
- `DailyContent` / history: later milestone.

### 8.4 Definitive bootstrap toolchain

- applicationId/namespace: `com.archimedeprojects.arihna`
- minSdk 28
- compileSdk 37
- targetSdk 37
- AGP 9.3.1
- Gradle wrapper 9.5.0
- Kotlin 2.4.10
- Compose compiler plugin 2.4.10
- AGP built-in Kotlin; do not apply `org.jetbrains.kotlin.android`
- Compose BOM 2026.08.00
- JDK / Java target 17
- Kotlin DSL
- version catalog `gradle/libs.versions.toml`
- one `:app` module
- manual `AppContainer`

### 8.5 Bootstrap shell — CLOSED

Contains `ArihnaApplication`, `MainActivity`, `ArihnaApp`, `AppContainer`, base theme, navigation shell and placeholder-only Home/Prayer Times/Qibla/Quran/Alarms/Settings destinations. Approved branding assets are wired through Android resources/manifest.

`versionName = 0.1.0-bootstrap` is temporary; before the first real public release, normalize to clean SemVer such as `0.1.0`.

Current branded splash integration uses Android's native Splash Screen behavior on API 31+. Before definitive UI/release, evaluate whether a custom/pre-31 fallback is needed for Android 9–11; this is not part of the prayer calculation milestone.

## 9. Repository structure

Single-module Android project with `app/src/main`, `app/src/test`, `app/src/androidTest`, version catalog and Gradle wrapper. Feature/core packages stay inside `:app`. The technical package identifier is permanently `com.archimedeprojects.arihna` for update continuity.

## 10. CI/CD requirements

### Debug APK

Build signed debug APKs with a persistent debug keystore reconstructed from GitHub Secrets and publish them through GitHub Releases, not solely Actions artifacts. Final trigger convention is pending.

### Stable release

On approved version tags/releases: checkout, JDK/Android setup, test/lint, reconstruct signing key, build signed APK, checksum if practical, create/update GitHub Release and attach APK. Production signing key must remain persistent and secret.

## 11. README requirements

Eventually document what Arihna is, supported device notes, how to download/install/update GitHub Release APKs, Samsung sideload guidance, debug vs stable releases, permissions, battery/alarm caveats and third-party/religious-data attributions.

## 12. Religious-source integrity

Never present Quran/hadith from memory as authoritative app content without verification. Store provenance/source metadata. Preserve Quran text integrity and licensing. Any translation requires redistribution-compatible permission/license.

## 13. Testing expectations

At minimum test:

- prayer-time calculation across representative coordinates/dates/methods, including the detailed plan in §5.1;
- API 28 HijrahChronology instrumentation regression;
- Qibla math when that milestone starts;
- DST/timezone transitions;
- exact alarm reboot/timezone/time changes when alarms start;
- notification permission flows;
- location precise/approximate/denied flows;
- daily-content anti-repeat logic;
- Quran data integrity;
- Compose navigation/state.

## 14. Decision status

### Closed

- applicationId/namespace `com.archimedeprojects.arihna`.
- Bootstrap architecture/toolchain and successful structural build.
- UI palette, Hero Dashboard direction, Prayer Times vertical timeline.
- Logo/app icon and Android branding assets.
- Prayer calculation engine: Adhan Kotlin Multiplatform `0.0.7`, MIT, behind Arihna adapter.
- Prayer calculation domain model set.
- 11 calculation methods.
- Asr Standard/Hanafi behavior.
- Arihna automatic high-latitude rule based on `abs(latitude) > 48°`.
- Explicit ZoneId/ZoneRules timezone/DST architecture.
- Per-prayer manual offset model.
- Controlled unavailable/error behavior for extreme astronomical cases.
- Prayer calculation test plan.
- Android API 28 `HijrahChronology.INSTANCE` verification gate — passed with 2 instrumentation tests on Android 9 emulator.
- Umm al-Qura Ramadan Isha rule: 90 minutes normally / 120 minutes in Ramadan, with Ramadan detected offline via verified `HijrahChronology.INSTANCE`.

### Pending

1. Arabic Quran source/packaging.
2. Italian translation inclusion/source.
3. Debug-release trigger convention.
4. Default calculation method for fresh install.
5. Default notification/adhan sound policy and audio licensing.
6. Pre-31 custom splash fallback decision before definitive release/UI.

## 15. Explicitly out of scope unless later approved

- Play Store publication
- paid services/APIs
- user accounts/cloud sync
- advertising/monetization
- third-party tracking analytics
- social/community features

## 16. Milestone sequence

1. Branding/UI decision closure — CLOSED.
2. Android bootstrap — CLOSED and build verified.
3. **Current milestone:** prayer-time calculation engine only.
4. Sequence inside current milestone: specification — DONE → dependency/domain/adapter excluding Ramadan special rule — STAGED/COMPILED → API 28 HijrahChronology instrumentation verification — PASSED → Ramadan special rule → unit tests/build → dedicated implementation commit → STOP.
5. Location/GPS, permissions, notifications, AlarmManager, definitive UI, Qibla, Quran and custom alarms remain separate milestones.

## 17. Change log

### 2026-08-29 — Android API 28 HijrahChronology verification passed

Before implementing the Ramadan-specific Umm al-Qura rule, Arihna ran `HijrahChronology.INSTANCE` in an Android instrumentation test on an Android 9/API 28 emulator. GitHub Actions run `33245235911` completed successfully; `connectedDebugAndroidTest` executed and passed 2 tests. The tests verified `Hijrah-umalqura` and known Gregorian dates mapping to Islamic months 8/9/10, including Ramadan 1445 and Ramadan 1437. The gate is closed and the 90/120-minute Isha rule may now be implemented.

### 2026-08-29 — Prayer calculation architecture approved

Approved before implementation:

- Adhan Kotlin Multiplatform `com.batoulapps.adhan:adhan2:0.0.7`, MIT;
- adapter boundary `PrayerTimeCalculator` / `AdhanPrayerTimeCalculator`;
- Arihna-owned domain models `Coordinates`, `PrayerCalculationMethod`, `AsrMethod`, `HighLatitudeRule`, `PrayerTimeAdjustments`, `PrayerCalculationSettings`, `PrayerTimes`, `PrayerDay`, `PrayerCalculationResult`;
- 11 calculation methods: MWL, Umm al-Qura, ISNA, Egyptian, Karachi, Dubai, Kuwait, Qatar, Moonsighting Committee, Singapore, Turkey;
- Standard/Hanafi Asr;
- Arihna automatic high-latitude rule using absolute latitude;
- controlled `Unavailable` behavior for polar/extreme cases;
- explicit ZoneId and ZoneRules for timezone/DST;
- per-prayer manual minute offsets;
- intended Umm al-Qura Isha 90/120 rule with mandatory Android API 28 HijrahChronology verification before implementation;
- golden-value/mapping/Asr/offset/high-latitude/southern-hemisphere/polar/DST/Ramadan/invalid-input test plan;
- explicit exclusion of GPS, permissions, notifications, AlarmManager and definitive UI from this milestone.

### 2026-08-29 — Bootstrap completed

Created and build-verified the single-module Android/Compose structural shell. `versionName 0.1.0-bootstrap` remains temporary; pre-31 splash fallback is deferred for later UI/release evaluation.

### 2026-08-29 — Android bootstrap matrix approved

Approved `com.archimedeprojects.arihna`, minSdk 28, compile/target 37, AGP 9.3.1, Gradle 9.5.0, Kotlin/Compose plugin 2.4.10, Compose BOM 2026.08.00, JDK 17, built-in Kotlin, Kotlin DSL, version catalog and manual AppContainer.

### 2026-08-29 — Branding and UI direction finalized

Closed palette, Hero Dashboard, Prayer Times timeline, final A/minaret/crescent/adhan-wave icon and Android branding assets.

### 2026-08-29 — Initial specification

Captured native Kotlin/Compose, zero-cost/private-GitHub/sideload distribution, prayer times, Qibla, alarms, offline Quran, optional Italian translation, daily motivational content, permissions/exact alarms/offline behavior, UI/logo approval gate, GitHub Releases signing and religious-source verification requirements.
