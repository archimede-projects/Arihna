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
- **Android applicationId / namespace:** `com.archimedeprojects.arihna` — definitive technical identifier; lowercase by convention and fixed for update continuity.
- The exact product name **Arihna** must be used consistently in application/package naming, display name, repository-facing documentation, release naming, commit language where the product name appears, and ordinary UI text. Do not introduce variants such as `ArihnaApp` or `Ari7naBiha`.
- The approved extended **artwork** may render the wordmark typographically as `ARIHNA`; this is a visual treatment only and does not change the canonical product name `Arihna`.
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
3. Commit the specification update before or together with the first corresponding implementation change, while preserving the ordering that the specification is updated first.
4. Implement the smallest coherent step.
5. Build/test where feasible.
6. Commit with a descriptive message.
7. Keep unresolved choices explicitly marked as **Pending decision** rather than silently choosing for the user.
8. Work one objective at a time; do not begin the next milestone until the current one is closed.

## 4. UX/UI and branding decisions

### 4.1 Mockup phase — CLOSED

The exploratory UI phase is complete and approved.

#### Official visual direction

- **Base layout:** Layout 1 — **Hero Dashboard**.
- Design character: calm, spacious, premium but sober.
- Main hierarchy: the **next prayer** is the primary piece of information and is shown in a large hero card.
- Cards should use generous spacing, restrained borders/shadows, and a low-density visual rhythm rather than a crowded dashboard.
- The definitive color basis is green/gold/off-white.

#### Screen-specific approved layout

- **Home**
  - follows Layout 1 Hero Dashboard;
  - next prayer is the dominant hero card;
  - prayer name, time and countdown have the strongest hierarchy;
  - current/last known location is visible but secondary;
  - “azione del giorno” and motivational/religious content appear below the hero content as calm supporting cards.
- **Prayer times**
  - exception to the Layout 1 list;
  - uses the **vertical timeline from Layout 2**;
  - all prayers for the current day are shown along a vertical line with dots/markers;
  - a current-time/progress indicator communicates where the user is within the prayer-day timeline;
  - the next/currently relevant prayer receives clear emphasis.
- **Qibla**
  - follows Layout 1;
  - large compass is the dominant element;
  - bearing/direction and location/calibration information are supporting content.
- **Quran**
  - follows Layout 1;
  - reading view prioritizes Arabic text;
  - Italian translation, when enabled, remains visually secondary;
  - generous vertical rhythm and reading-focused spacing;
  - avoid decorative treatment that competes with the Quran text.
- **Alarms**
  - follows Layout 1;
  - calm card/list presentation with clear time, label/scope, schedule and enabled state.
- **Settings**
  - follows Layout 1;
  - spacious, grouped settings with restrained dividers/cards.

No implementation of these screens has started yet. The approved design direction is now an implementation requirement.

### 4.2 Official palette — CLOSED

Approved colors:

- **Arihna Green:** `#0F5132`
- **Arihna Gold:** `#D4AF37`
- **Arihna Off-white:** `#FAFAF6`

These are the official branding/UI base colors. Accessibility contrast may require derived tonal values in implementation, but derived colors must remain visually consistent with this palette.

### 4.3 Logo/app icon phase — CLOSED

The definitive Arihna mark is approved.

#### Official icon concept

A geometric monogram **A** integrating:

- a minimal **minaret** into the letterform;
- a **crescent** above the minaret;
- **three radiating sound waves** evoking the adhan / propagation of the call to prayer.

The combination intentionally communicates both the place associated with the adhan (minaret) and the sound/call itself (radiating waves).

#### Religious-content restriction for the mark

The icon/logo must contain:

- no Quran verses;
- no divine names;
- no sacred text;
- no generated Arabic calligraphy.

#### Legibility requirements

Approved for:

- 48×48 px launcher reference size;
- 24×24 px notification reference size;
- 16×16 px favicon/badge reference size;
- Android adaptive masks including circular and squircle-like masks;
- light and dark presentation contexts.

#### Finalized branding assets

Android resources:

- `app/src/main/res/mipmap-mdpi/ic_launcher.png`
- `app/src/main/res/mipmap-hdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`
- matching `ic_launcher_round.png` files in each density
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- `app/src/main/res/drawable/ic_notification_arihna.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml`
- `app/src/main/res/values/branding_colors.xml`

Reference/web branding:

- `docs/branding/arihna-icon-master.svg`
- `docs/branding/arihna-icon-light.svg`
- `docs/branding/arihna-icon-dark.svg`
- `docs/branding/arihna-logo-extended-light.svg`
- `docs/branding/arihna-logo-extended-dark.svg`
- `docs/branding/favicon.svg`
- raster favicons/previews in `docs/branding/`

Android's actual small notification icon is intentionally a monochrome vector because Android masks/tints small notification icons. API 33+ adaptive-icon resources include a monochrome layer for themed launcher icons; API 26–32 use background + foreground layers only.

## 5. Core feature requirements

### 5.1 Prayer times

- Calculate daily Islamic prayer times from the phone's geographic position.
- Support precise GPS location when granted.
- Support approximate location when that is what Android/user allows.
- Recalculate/update automatically each day.
- Allow the user to choose a standard calculation method.
- Candidate methods to expose include, subject to final technical validation:
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
- Core time calculation must not require a paid cloud service.
- **First real functional milestone after project bootstrap:** prayer-time calculation.

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
  - redistribution license;
  - attribution requirements;
  - text integrity requirements;
  - APK/database size;
  - offline availability.
- **Pending decision:** whether Italian translation is included by default, offered as optional offline pack, or omitted in v1.
- Religious text must come from a verified, attributable source and must not be invented or silently modified.

### 5.5 Daily motivational content

Each day provide a morning-oriented and evening-oriented item.

Morning content should include some combination of:

- a practical good action for the day;
- a motivational/reflection phrase;
- a Quran verse and/or authentic hadith.

Evening content should provide analogous reflective content suited to the evening.

Requirements:

- morning notification;
- evening notification;
- content changes daily;
- avoid close repetitions;
- Quran references must cite surah and verse number;
- hadith must be authentic/verified and cite collection plus identifying number/reference;
- never fabricate religious quotations or references;
- prefer a curated local/offline content dataset so core daily content does not depend on a network service.

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

- Quran Arabic text;
- optional translation if bundled/downloaded locally;
- curated daily motivational/religious content dataset;
- stored settings;
- stored alarm definitions;
- prayer-time calculation given saved coordinates/date/configuration;
- Qibla bearing given saved coordinates.

Network may be required for:

- GitHub APK download/update by the user;
- an optional first-time content download if the chosen Quran/translation packaging model uses downloadable packs;
- optional future features explicitly approved later.

Location itself should use Android location providers and must not require a paid geocoding service for core calculation.

## 8. Android technical architecture — bootstrap APPROVED

The bootstrap architecture and toolchain below were explicitly approved on 2026-08-29. The bootstrap may create only a buildable structural shell; prayer-time calculation and other functional features remain separate future milestones.

### 8.1 App architecture

Approved bootstrap baseline:

- one Android application module (`:app`) initially;
- feature/package organization rather than premature Gradle multi-module decomposition;
- pragmatic layered boundaries:
  - `ui/` — Compose screens/components/theme/navigation;
  - `domain/` — use cases and pure models/calculation interfaces;
  - `data/` — Room repositories, preferences, bundled datasets;
  - `platform/` — location, sensors, notifications, alarms, receivers;
  - `feature/` — feature-oriented packages for home, prayers, qibla, quran, alarms, settings;
- MVVM/state-holder approach using lifecycle-aware ViewModels and Kotlin Flows;
- unidirectional data flow in Compose.

If the project grows substantially, feature/core Gradle modules may be introduced later; avoid premature modularization.

### 8.2 Candidate Android libraries/components

All final versions and licenses must be verified immediately before addition.

- Jetpack Compose + Material 3
- Navigation Compose
- AndroidX Lifecycle / ViewModel
- Kotlin Coroutines + Flow
- Room for structured local content, Quran metadata/text, and alarm/content history where appropriate
- DataStore Preferences for lightweight settings
- WorkManager for deferrable/non-exact daily maintenance
- AlarmManager for time-critical prayer/custom alarms
- Android location APIs / fused location where available without introducing a paid service dependency; final choice to be approved
- Android `SensorManager` rotation-vector/orientation sensors for Qibla compass
- a verified open-source prayer-time calculation library or a small audited local implementation; final choice pending technical/license review

### 8.3 Proposed data separation

- `PrayerSettings`: calculation method, madhhab/Asr method, manual offsets if later approved, notification preferences
- `LocationState`: last usable coordinates, accuracy/source, timestamp
- `PrayerDay`: computed times for a local civil date
- `Alarm`: user-created and prayer-linked schedules
- `QuranSurah` / `QuranAyah`: offline text and metadata
- `DailyContent`: curated action/reflection + linked Quran/hadith source metadata
- `DailyContentHistory`: recent IDs/dates to enforce anti-repeat rules

### 8.4 Definitive bootstrap toolchain

Approved versions and build choices:

- **applicationId / namespace:** `com.archimedeprojects.arihna`
- **minSdk:** `28` (Android 9)
- **compileSdk:** `37` (Android 17)
- **targetSdk:** `37` (Android 17)
- **Android Gradle Plugin:** `9.3.1`
- **Gradle wrapper:** `9.5.0`
- **Kotlin:** `2.4.10`
- **Compose compiler plugin:** `org.jetbrains.kotlin.plugin.compose` `2.4.10`
- **AGP built-in Kotlin:** enabled; do not apply `org.jetbrains.kotlin.android`
- **Compose BOM:** `2026.08.00`
- **JDK / Java target:** `17`
- **build scripts:** Kotlin DSL
- **version catalog:** `gradle/libs.versions.toml`
- **Gradle modules:** only `:app` for the bootstrap
- **dependency injection:** manual `AppContainer`; no Hilt/Koin

Bootstrap-only dependencies are limited to AndroidX Core KTX, Activity Compose, Compose/Material 3, Lifecycle runtime/ViewModel Compose, Navigation Compose, and Kotlin Coroutines Android. Room, DataStore, WorkManager, Adhan/prayer calculation, location providers, Quran data, alarm/notification frameworks, KSP, and DI frameworks are explicitly deferred until their own milestones.

### 8.5 Bootstrap shell scope

The approved structural shell contains:

- `ArihnaApplication.kt`;
- `MainActivity.kt`;
- `ArihnaApp.kt`;
- `AppContainer.kt`;
- base Arihna theme (`Color.kt`, `Theme.kt`, `Type.kt`, `Shape.kt`);
- navigation shell;
- placeholder-only destinations for Home, Prayer Times, Qibla, Quran, Alarms, and Settings;
- existing approved launcher/adaptive/themed branding assets wired through the manifest.

The placeholders are not the definitive approved UI implementation and must contain no prayer calculation, location access, Quran data, alarm scheduling, or notification behavior.

## 9. Repository structure — approved bootstrap target

```text
Arihna/
├─ PROJECT_SPEC.md
├─ README.md
├─ LICENSES/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml
│     │  ├─ java/com/archimedeprojects/arihna/
│     │  │  ├─ ArihnaApplication.kt
│     │  │  ├─ ui/
│     │  │  ├─ domain/
│     │  │  ├─ data/
│     │  │  ├─ platform/
│     │  │  └─ feature/
│     │  └─ res/
│     │     ├─ drawable/             # branding vector layers already finalized
│     │     ├─ mipmap-*/             # launcher assets already finalized
│     │     └─ values/
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
   ├─ branding/                      # approved logo/web/reference assets
   ├─ architecture.md
   ├─ religious-sources.md
   └─ ui-decisions.md
```

The technical Android package/application identifier is definitively `com.archimedeprojects.arihna`. The visible product name remains exactly `Arihna`. Changing the applicationId after distributed installs would break update continuity and is therefore prohibited.

## 10. CI/CD requirements

### 10.1 Debug APK publication

- A workflow must build a signed debug APK for intermediate testing.
- Do not rely on GitHub Actions artifact retention as the delivery channel.
- Publish the APK to GitHub Releases.
- Use a persistent debug keystore reconstructed at workflow runtime from GitHub Secrets.
- Required secret concept (exact names may be finalized during implementation):
  - base64-encoded debug keystore;
  - keystore password;
  - key alias;
  - key password.
- Never print secrets in logs.
- Release/tag naming must make debug builds unmistakable and allow convenient installation/update.
- Final trigger is **Pending decision**.

### 10.2 Release APK publication

- On an approved version tag/release convention, GitHub Actions must:
  1. check out source;
  2. install the required JDK/Android build environment;
  3. restore safe Gradle caches;
  4. reconstruct signing keystore from GitHub Secrets;
  5. run tests/lint as appropriate;
  6. build the signed release APK;
  7. calculate checksum if practical;
  8. create/update the corresponding GitHub Release;
  9. attach the APK (and optional checksum file) to the Release.
- Production signing key must remain persistent so updates install over previous releases.

## 11. README requirements

`README.md` must eventually include:

- what Arihna is;
- supported target/device notes;
- how to download an APK from GitHub Releases;
- how to enable sideload/install unknown apps on Samsung/Android safely;
- distinction between debug test releases and stable releases;
- update procedure without uninstalling when signing key/package is unchanged;
- permissions explanation;
- any Samsung battery/background-alarm guidance required for reliable alerts;
- third-party/religious-data attribution links or references.

## 12. Religious-source integrity

- Never generate a Quran verse or hadith from memory as authoritative app content without verification against the chosen source dataset/reference.
- Store source metadata alongside content.
- Preserve Quran Arabic text integrity according to the source license/terms.
- For hadith, store collection, book/chapter where available, reference/number, grading/source where relevant, and source dataset provenance.
- Any translation included in the app requires an explicit redistribution-compatible license or explicit permission.

## 13. Testing expectations

At minimum, plan for tests around:

- prayer-time calculation across representative coordinates/dates/methods;
- Qibla bearing math;
- DST/time-zone transitions;
- exact alarm rescheduling after reboot/time-zone/time changes where supported;
- notification permission denied/granted flows;
- location approximate/precise/denied flows;
- anti-repeat daily-content selection;
- Quran database/data integrity checks;
- Compose navigation and essential screen state.

Branding assets should additionally be verified during project bootstrap by a real Android build/resource merge.

## 14. Decisions status

### Closed

- Android applicationId/namespace: `com.archimedeprojects.arihna`.
- Bootstrap architecture: one `:app` module, package-level `core/` + `feature/`, manual `AppContainer`.
- Bootstrap toolchain: minSdk 28, compileSdk/targetSdk 37, AGP 9.3.1, Gradle 9.5.0, Kotlin 2.4.10, Compose BOM 2026.08.00, JDK 17.
- UI palette: `#0F5132`, `#D4AF37`, `#FAFAF6`.
- UI base layout: Layout 1 Hero Dashboard.
- Prayer Times layout: Layout 2 vertical timeline within the Layout 1 design language.
- Logo/app icon: final A + integrated minaret + crescent + radiating adhan sound waves.
- Logo legibility/adaptive-mask review.
- Android launcher/adaptive/themed icon asset structure.

### Pending

1. Prayer calculation library/implementation after license/API validation.
2. Arabic Quran source and packaging after license/integrity validation.
3. Italian translation: include, optional pack, or omit; exact licensed source.
4. Debug-release trigger convention.
5. Default calculation method for a fresh installation.
6. Default notification/adhan sound policy and included audio licensing.

## 15. Explicitly out of scope unless later approved

- Google Play Store publication
- paid APIs or services
- user accounts/cloud sync
- advertising/monetization
- analytics requiring third-party tracking
- social/community features

## 16. Milestone sequence

Current agreed sequence:

1. **Branding/UI decision closure — CLOSED in specification and assets.**
2. **Android bootstrap proposal/toolchain approval — CLOSED.**
3. **Current milestone:** create and verify the structural Android bootstrap without feature logic.
4. After a successful bootstrap build and dedicated commit, stop.
5. First real functional feature after bootstrap: **prayer-time calculation**.
6. UI implementation follows approved layout/branding but must not jump ahead of the agreed milestone sequence.

## 17. Change log

### 2026-08-29 — Android bootstrap matrix approved

Approved the definitive structural/bootstrap decisions before implementation:

- fixed technical Android identifier to `com.archimedeprojects.arihna`;
- approved single Gradle module `:app` with package-level `core/` and `feature/` organization;
- approved manual dependency injection via `AppContainer`;
- approved `minSdk 28`, `compileSdk 37`, `targetSdk 37`;
- approved AGP `9.3.1`, Gradle `9.5.0`, Kotlin/Compose compiler plugin `2.4.10`, Compose BOM `2026.08.00`, JDK/Java target `17`;
- approved AGP 9 built-in Kotlin and explicitly excluded `org.jetbrains.kotlin.android`;
- approved Kotlin DSL and version catalog;
- limited bootstrap dependencies to the minimal Compose/navigation/lifecycle/coroutines shell;
- authorized placeholder-only navigation for the six primary destinations;
- explicitly deferred all prayer calculation and other functional implementation until after a successful bootstrap commit.

### 2026-08-29 — Branding and UI direction finalized

Closed the visual decision phase:

- approved official palette: Arihna Green `#0F5132`, Arihna Gold `#D4AF37`, Arihna Off-white `#FAFAF6`;
- approved Layout 1 Hero Dashboard as the definitive UI base;
- approved Layout 2 vertical timeline specifically for the Prayer Times screen;
- approved final icon: A monogram + integrated minaret + crescent + radiating sound waves evoking the adhan;
- confirmed no sacred text, Quran verse or generated Arabic calligraphy in the mark;
- finalized Android legacy launcher assets for mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi;
- finalized round launcher variants;
- finalized adaptive background/foreground resources;
- finalized API 33+ monochrome adaptive-icon resources;
- finalized monochrome notification vector;
- finalized SVG light/dark/reference/wordmark assets and favicons;
- explicitly stopped before Android UI or functional-code implementation.

Next milestone is a **proposal** for Android project bootstrap; implementation must wait for approval.

### 2026-08-29 — Initial specification

Captured initial requirements for:

- native Kotlin + Jetpack Compose app;
- zero-cost/private-GitHub/sideload distribution constraints;
- prayer times and calculation preferences;
- Qibla compass;
- custom alarms and prayer alerts;
- offline Quran;
- optional Italian translation evaluation;
- morning/evening daily motivational religious content;
- Android permissions, exact alarms, offline behavior and Samsung battery considerations;
- mandatory UI/logo approval gate before definitive UI implementation;
- GitHub Releases based debug and production APK delivery with persistent secret signing keys;
- README installation guidance;
- religious-source verification and citation policy.

At initial capture no definitive UI, logo, Quran translation, prayer library, or release-trigger design had yet been selected.
