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
- The exact form **Arihna** must be used consistently in the app name, repository-facing documentation, UI, release naming, commit language where the product name appears, and application/package naming. Do not introduce variants such as `ArihnaApp` or `Ari7naBiha`.
- Name inspiration: the hadith expression **«أرحنا بها يا بلال»** (*Arihna biha ya Bilal* — “Dacci sollievo con essa, o Bilal”), referring to prayer.

## 2. Non-negotiable constraints

1. **Zero monetary cost** for development and normal use.
2. No credit card requirement, paid API, paid subscription, or developer-store subscription.
3. Prefer only free/open-source dependencies; a free tier is acceptable only if it requires no card and does not create a hard external dependency for core offline functionality.
4. Source code is hosted in the private GitHub repository.
5. The app is installed manually from APK files attached to GitHub Releases.
6. Intermediate debug APKs must also be published through GitHub Releases, **not** retained only as GitHub Actions artifacts.
7. Debug builds must be signed with a **persistent debug signing key** so a newly downloaded debug APK can update the previous debug installation without uninstalling it.
8. The persistent debug keystore must **never be committed**. Its encoded content and credentials must be stored as GitHub Secrets.
9. Production/release signing material must also never be committed.
10. Development history must remain understandable through descriptive commits plus this specification file.

## 3. Development workflow rule

For every future requested feature/change:

1. Read the current `PROJECT_SPEC.md`.
2. Update `PROJECT_SPEC.md` first with the new/changed requirement and any accepted design/technical decision.
3. Commit the specification update before or together with the first code change, while preserving the ordering that the specification is updated before implementation work.
4. Implement the smallest coherent step.
5. Build/test where feasible.
6. Commit with a descriptive message.
7. Keep unresolved choices explicitly marked as **Pending decision** rather than silently choosing for the user.

## 4. UX/UI process — mandatory approval gate

Before implementing the definitive UI, provide at least **2–3 distinct visual directions** for the main screens and wait for user selection.

Screens requiring mockups before final implementation:

- Home
  - current/next prayer time
  - daily action/good deed
  - daily motivational/religious content
- Prayer times
  - all prayer times for the current day
- Qibla
  - compass/direction toward the Kaaba
- Quran
  - surah list
  - reading view
- Alarms
- Settings

Logo/app icon process:

- Provide multiple real generated image variants, not text descriptions only.
- Wait for explicit user choice before finalizing icon/logo assets.

### Current UI status

- **Pending decision:** visual style, layout system, colors, typography, iconography, logo and app icon.
- No definitive UI implementation should be started until a visual direction is selected.

## 5. Core feature requirements

### 5.1 Prayer times

- Calculate daily Islamic prayer times from the phone's geographic position.
- Support precise GPS location when granted.
- Support approximate location when that is what Android/user allows.
- Recalculate/update automatically each day.
- Allow the user to choose a standard calculation method.
- Candidate methods to expose include, subject to technical validation:
  - Muslim World League (MWL)
  - Umm al-Qura University, Makkah
  - ISNA
  - Egyptian General Authority of Survey
  - Karachi / University of Islamic Sciences
  - other widely used standard methods if supported by the selected calculation engine
- Allow Asr jurisprudence/shadow method selection where relevant:
  - Standard / Shafi'i (also commonly used by Maliki/Hanbali)
  - Hanafi
- Provide configurable prayer-time notifications / adhan reminders.
- Core time calculation should not require a paid cloud service.

### 5.2 Qibla

- Determine Qibla bearing from the current geographic position to the Kaaba.
- Use Android device sensors to present a compass-style live direction.
- Handle sensor accuracy/calibration state gracefully.
- Qibla should remain calculable offline once location coordinates are available.

### 5.3 Alarms

- Users can create custom alarms in addition to automatic prayer-related alarms.
- Alarm sounds are configurable.
- Prayer-related alarms/notifications must be designed for high reliability during device standby/doze, within Android platform restrictions.

### 5.4 Quran

- Arabic Quran text must be readable offline.
- Navigation by surah is required.
- Reading must not require connectivity after the Quran text has been bundled or downloaded.
- Evaluate an optional Italian translation before inclusion.
- Translation choice must consider:
  - redistribution license
  - attribution requirements
  - text integrity requirements
  - APK/database size
  - offline availability
- **Pending decision:** whether Italian translation is included by default, offered as optional offline pack, or omitted in v1.
- Religious text must come from a verified, attributable source and must not be invented or silently modified.

### 5.5 Daily motivational content

Each day provide a morning-oriented and evening-oriented item.

Morning content should include some combination of:

- a practical good action for the day
- a motivational/reflection phrase
- a Quran verse and/or authentic hadith

Evening content should provide analogous reflective content suited to the evening.

Requirements:

- morning notification
- evening notification
- content changes daily
- avoid close repetitions
- Quran references must cite surah and verse number
- hadith must be authentic/verified and cite collection plus identifying number/reference
- never fabricate religious quotations or references
- Prefer a curated local/offline content dataset so core daily content does not depend on a network service.

## 6. Permissions and Android behavior

### 6.1 Location

- Request only when needed.
- Explain clearly why location is required for prayer times and Qibla.
- Correctly support Android precise vs approximate location permission behavior.
- The app should degrade gracefully when permission is denied, for example by allowing a manual location path if added/approved later.

### 6.2 Notifications

- Correctly request `POST_NOTIFICATIONS` on Android 13+ when notifications are enabled/requested.
- Notification categories/channels should separate at least prayer reminders/adhan from morning/evening content when beneficial.

### 6.3 Exact alarms / standby reliability

- Evaluate `AlarmManager` exact alarms, including `setExactAndAllowWhileIdle`, for time-critical prayer/custom alarms.
- Respect current Android restrictions around exact-alarm special access and platform policy.
- Use less exact mechanisms (for example WorkManager) for non-time-critical background refresh where appropriate.
- Document Samsung battery optimization / sleeping-app caveats that may affect reliability on Galaxy devices.
- Do not request broad battery-optimization exemptions unless technically justified and acceptable under Android behavior/policy.

## 7. Offline/data requirements

The architecture should maximize offline operation.

Expected offline-capable data/features:

- Quran Arabic text
- optional translation if bundled/downloaded locally
- curated daily motivational/religious content dataset
- stored settings
- stored alarm definitions
- prayer-time calculation given saved coordinates/date/configuration
- Qibla bearing given saved coordinates

Network may be required for:

- GitHub APK download/update by the user
- an optional first-time content download if the chosen Quran/translation packaging model uses downloadable packs
- optional future features explicitly approved later

Location itself should use Android location providers and must not require a paid geocoding service for core calculation.

## 8. Proposed technical architecture — initial, subject to validation

### 8.1 App architecture

Recommended baseline:

- single Android application module initially, split by feature/package to avoid unnecessary Gradle complexity during early development
- Clean-ish layered architecture with pragmatic boundaries:
  - `ui/` — Compose screens/components/theme/navigation
  - `domain/` — use cases and pure models/calculation interfaces
  - `data/` — Room repositories, preferences, bundled datasets
  - `platform/` — location, sensors, notifications, alarms, receivers
  - `feature/` — feature-oriented packages for home, prayers, qibla, quran, alarms, settings
- MVVM/state-holder approach using lifecycle-aware ViewModels and Kotlin Flows.
- Unidirectional data flow in Compose.

If the project grows substantially, feature Gradle modules may be introduced later; avoid premature modularization.

### 8.2 Candidate Android libraries/components

All final versions and licenses must be verified before adding them.

- Jetpack Compose + Material 3
- Navigation Compose
- AndroidX Lifecycle / ViewModel
- Kotlin Coroutines + Flow
- Room for structured local content, Quran metadata/text, and alarm/content history where appropriate
- DataStore Preferences for lightweight settings
- WorkManager for deferrable/non-exact daily maintenance
- AlarmManager for time-critical prayer/custom alarms
- Android location APIs / fused location where available without introducing a paid service dependency; fallback strategy to be evaluated
- Android `SensorManager` rotation-vector/orientation sensors for Qibla compass
- A verified open-source prayer-time calculation library or a small audited local implementation; final choice pending technical/license review

### 8.3 Proposed data separation

- `PrayerSettings`: calculation method, madhhab/Asr method, manual offsets if later approved, notification preferences
- `LocationState`: last usable coordinates, accuracy/source, timestamp
- `PrayerDay`: computed times for a local civil date
- `Alarm`: user-created and prayer-linked schedules
- `QuranSurah` / `QuranAyah`: offline text and metadata
- `DailyContent`: curated action/reflection + linked Quran/hadith source metadata
- `DailyContentHistory`: recent IDs/dates to enforce anti-repeat rules

## 9. Repository structure — proposed

```text
Arihna/
├─ PROJECT_SPEC.md
├─ README.md
├─ LICENSES/                       # third-party notices/text-data licenses
├─ app/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml
│     │  ├─ java/.../arihna/
│     │  │  ├─ ArihnaApplication.kt
│     │  │  ├─ ui/
│     │  │  ├─ domain/
│     │  │  ├─ data/
│     │  │  ├─ platform/
│     │  │  └─ feature/
│     │  └─ res/
│     ├─ test/
│     └─ androidTest/
├─ gradle/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ .github/
│  └─ workflows/
│     ├─ debug-release.yml
│     └─ release.yml
└─ docs/
   ├─ architecture.md              # optional when architecture grows
   ├─ religious-sources.md         # verified Quran/hadith source inventory
   └─ ui-decisions.md              # accepted UI direction/assets
```

The exact package/application ID must use the `Arihna` project identity without product-name variants while still conforming to Android package naming rules. Final reverse-domain prefix is **Pending decision/initial project bootstrap**.

## 10. CI/CD requirements

### 10.1 Debug APK publication

- A workflow must build a signed debug APK for intermediate testing.
- Do not rely on GitHub Actions artifact retention as the delivery channel.
- Publish the APK to GitHub Releases.
- Use a persistent debug keystore reconstructed at workflow runtime from GitHub Secrets.
- Required secret concept (exact names may be finalized during implementation):
  - base64-encoded debug keystore
  - keystore password
  - key alias
  - key password
- Never print secrets in logs.
- Release/tag naming must make debug builds unmistakable and allow convenient installation/update.
- Prefer a deliberate trigger strategy that does not flood Releases for every tiny commit (for example manual dispatch and/or a dedicated debug tag convention). Final trigger is **Pending decision**.

### 10.2 Release APK publication

- On an approved version tag/release convention, GitHub Actions must:
  1. check out source
  2. install the required JDK/Android build environment
  3. restore safe Gradle caches
  4. reconstruct signing keystore from GitHub Secrets
  5. run tests/lint as appropriate
  6. build the signed release APK
  7. calculate checksum if practical
  8. create/update the corresponding GitHub Release
  9. attach the APK (and optional checksum file) to the Release
- Production signing key must remain persistent so updates install over previous releases.

## 11. README requirements

`README.md` must eventually include:

- what Arihna is
- supported target/device notes
- how to download an APK from GitHub Releases
- how to enable sideload/install unknown apps on Samsung/Android safely
- distinction between debug test releases and stable releases
- update procedure without uninstalling when signing key/package is unchanged
- permissions explanation
- any Samsung battery/background-alarm guidance required for reliable alerts
- third-party/religious-data attribution links or references

## 12. Religious-source integrity

- Never generate a Quran verse or hadith from memory as authoritative app content without verification against the chosen source dataset/reference.
- Store source metadata alongside content.
- Preserve Quran Arabic text integrity according to the source license/terms.
- For hadith, store collection, book/chapter where available, reference/number, grading/source where relevant, and source dataset provenance.
- Any translation included in the app requires an explicit redistribution-compatible license or explicit permission.

## 13. Testing expectations

At minimum, plan for tests around:

- prayer-time calculation across representative coordinates/dates/methods
- Qibla bearing math
- DST/time-zone transitions
- exact alarm rescheduling after reboot/time-zone/time changes where supported
- notification permission denied/granted flows
- location approximate/precise/denied flows
- anti-repeat daily-content selection
- Quran database/data integrity checks
- Compose navigation and essential screen state

## 14. Decisions currently pending user approval

1. UI visual direction among proposed mockups.
2. Logo/app icon variant.
3. Exact reverse-domain `applicationId` prefix while retaining the exact product name Arihna.
4. Prayer calculation library/implementation after license/API validation.
5. Arabic Quran source and packaging after license/integrity validation.
6. Italian translation: include, optional pack, or omit; exact licensed source.
7. Debug-release trigger convention.
8. Default calculation method for a fresh installation (can also be locale/location-informed if later approved).
9. Default notification/adhan sound policy and included audio licensing.

## 15. Explicitly out of scope unless later approved

- Google Play Store publication
- paid APIs or services
- user accounts/cloud sync
- advertising/monetization
- analytics requiring third-party tracking
- social/community features

## 16. Change log

### 2026-08-29 — Initial specification

Captured initial requirements for:

- native Kotlin + Jetpack Compose app
- zero-cost/private-GitHub/sideload distribution constraints
- prayer times and calculation preferences
- Qibla compass
- custom alarms and prayer alerts
- offline Quran
- optional Italian translation evaluation
- morning/evening daily motivational religious content
- Android permissions, exact alarms, offline behavior and Samsung battery considerations
- mandatory UI/logo approval gate before definitive UI implementation
- GitHub Releases based debug and production APK delivery with persistent secret signing keys
- README installation guidance
- religious-source verification and citation policy

No definitive UI, logo, Quran translation, prayer library, or release-trigger design has been selected yet.
