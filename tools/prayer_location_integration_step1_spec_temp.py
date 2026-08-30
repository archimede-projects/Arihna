from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match, found {count}: {old[:140]!r}")
    text = text.replace(old, new, 1)


integration_section = r'''### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 1 SPEC CLOSED

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
2. **STEP 2 — Prayer settings persistence: NOT STARTED / NEXT.** Implement `PrayerSettingsRepository`, same-DataStore Prayer keys/default initialization and focused tests.
3. **STEP 3 — schedule orchestration: NOT STARTED.** Implement `PrayerScheduleRepository` and pure fake-Location/fake-calculator contract tests.
4. **STEP 4 — presentation/countdown: NOT STARTED.** Implement `PrayerScheduleViewModel`, next-prayer/countdown behavior and tests.
5. **STEP 5 — functional Home panel: NOT STARTED.** Render the minimal Prayer Schedule UI without final Hero Dashboard scope.
6. **STEP 6 — full regression gate: NOT STARTED.** Run Prayer + Location + Integration unit/build/API28 regressions plus permission/asset checks and final APK/data non-regression where practical.
7. **STEP 7 — documentation-only milestone closure: NOT STARTED.** After an exact tested technical SHA is green/promoted, update the specification with definitive evidence and stop.

No STEP 2 implementation may begin until STEP 1 is explicitly confirmed closed. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.

'''

replace_once("### 5.3 Qibla", integration_section + "### 5.4 Qibla")
replace_once("### 5.4 Alarms", "### 5.5 Alarms")
replace_once("### 5.5 Quran", "### 5.6 Quran")
replace_once("### 5.6 Daily motivational content", "### 5.7 Daily motivational content")

replace_once(
    "- Unit/instrumentation test dependencies required for verification.\n\nWorkManager, alarms/notifications, Quran data libraries and Google/Fused location providers remain deferred/not approved here.",
    "- Unit/instrumentation test dependencies required for verification.\n- Prayer schedule integration: existing `PrayerTimeCalculator` + closed Location state, new `PrayerScheduleRepository`, and Prayer settings stored in the existing Preferences DataStore under separate keys; no new calculation engine or persistence library.\n\nWorkManager, alarms/notifications, Quran data libraries and Google/Fused location providers remain deferred/not approved here.",
)

replace_once(
    "- `LocationFreshness`, `LocationPermissionState`, `LocationResolutionState`, `LocationFailure`.\n- `Alarm`: later milestone.",
    "- `LocationFreshness`, `LocationPermissionState`, `LocationResolutionState`, `LocationFailure`.\n- Integration: `PrayerScheduleInput`, `PrayerScheduleState`, `PrayerSchedule`, `NextPrayer`; reuse existing `PrayerCalculationSettings`, `PrayerDay`, `PrayerCalculationResult` rather than duplicating Prayer domain.\n- `Alarm`: later milestone.",
)

replace_once(
    "Single-module Android project with `app/src/main`, `app/src/test`, `app/src/androidTest`, version catalog and Gradle wrapper. Location remains inside `:app` using domain/data/platform/UI boundaries; do not create a Gradle module only for this milestone.",
    "Single-module Android project with `app/src/main`, `app/src/test`, `app/src/androidTest`, version catalog and Gradle wrapper. Location remains inside `:app` using domain/data/platform/UI boundaries. The Prayer+Location integration also remains inside `:app` in a dedicated integration/feature package with orchestration, settings persistence, presentation and functional Home UI boundaries; do not create a Gradle module only for this milestone.",
)

prayer_test_anchor = "### Prayer regression\n\nKeep representative prayer golden/mapping/high-latitude/DST/Ramadan/error tests and API28 HijrahChronology regression passing.\n\n"
integration_tests = r'''### Prayer Engine + Location integration tests — CURRENT MILESTONE

Do not reopen internal Prayer Engine or Location algorithms. Test their new contract through fakes/recording boundaries. Required integration coverage includes exact `SelectedLocation.coordinates + zoneId` forwarding, no calculator call for non-Ready Location states, deduplicated identical inputs, recalculation on accepted Location/settings/date changes only, timezone/DST-aware local-midnight rollover, cancellation of stale calculations, today/tomorrow next-prayer behavior, and controlled `PrayerCalculationResult.Unavailable` propagation with no fabricated fallback. Prayer settings DataStore tests cover canonical MWL/Standard/Automatic/zero-offset initialization and persistence, malformed/partial recovery, custom settings/offset round-trip and isolation from Location keys. Presentation/Compose tests cover no-location, Ready daily schedule/next prayer/countdown and unavailable states without retesting raw GPS or Adhan formulas.

Final gate for this milestone must preserve the complete existing Prayer and Location regression suites and run `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest` with zero skipped tests, plus manifest/dependency policy and GeoNames asset checks.

'''
replace_once(prayer_test_anchor, prayer_test_anchor + integration_tests)

replace_once(
    "### Pending\n\n1. Arabic Quran source/packaging.\n2. Italian translation inclusion/source.\n3. Debug-release trigger convention.\n4. Default calculation method for fresh install.\n5. Default notification/adhan sound policy and audio licensing.\n6. Pre-31 custom splash fallback.\n7. Any future need for `ACCESS_FINE_LOCATION`.\n8. Any future need for background location/foreground location service.",
    "### Prayer Engine + Location integration milestone — OPEN / STEP 1 SPEC CLOSED\n\n- Approved orchestration boundary: `PrayerScheduleRepository.observeSchedule(): Flow<PrayerScheduleState>` + idempotent `refresh()`, consuming only closed Location state, persisted Prayer settings, the existing `PrayerTimeCalculator`, and an injected testable `Clock`.\n- Canonical temporary default: MWL / STANDARD / AUTOMATIC / zero offsets, materialized in the existing Preferences DataStore under separate Prayer keys. This is explicitly a known temporary convention limitation until the dedicated Prayer settings UI exists; no location-based automatic method selection is authorized.\n- Recalculate only on distinct selected Location, settings or selected-zone local-date change/bootstrap; countdown is presentation-only and never a Prayer recalculation trigger.\n- UI scope is a minimal functional Prayer Schedule panel on Home; the existing Location panel remains in `Impostazioni`; final Hero Dashboard is deferred.\n- Error discipline: non-Ready Location means no prayer schedule; Prayer calculation unavailable remains a controlled unavailable state with no invented fallback.\n- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is NOT STARTED / NEXT and requires explicit authorization after this STEP 1 commit.**\n\n### Pending\n\n1. Arabic Quran source/packaging.\n2. Italian translation inclusion/source.\n3. Debug-release trigger convention.\n4. Dedicated Prayer settings UI for user selection of calculation method, Asr convention, high-latitude rule and offsets; until then the documented MWL/Standard/Automatic/zero-offset default is a known temporary limitation.\n5. Default notification/adhan sound policy and audio licensing.\n6. Pre-31 custom splash fallback.\n7. Any future need for `ACCESS_FINE_LOCATION`.\n8. Any future need for background location/foreground location service.",
)

out_of_scope_anchor = "- background location\n- foreground location service\n\n## 16. Milestone sequence"
replace_once(
    out_of_scope_anchor,
    "- background location\n- foreground location service\n\nCurrent Prayer Engine + Location integration milestone additionally excludes:\n\n- Qibla and sensors\n- `POST_NOTIFICATIONS`, notification channels and adhan reminders\n- `AlarmManager`, WorkManager and custom alarms\n- adhan audio\n- Quran and daily-content work\n- definitive Hero Dashboard / final Prayer Times timeline\n- editing Prayer calculation settings in UI (persistence/default only in this milestone)\n- changes to the internal Prayer Engine formulas or closed Location acceptance/acquisition logic\n\n## 16. Milestone sequence",
)

replace_once(
    "6. Notifications, AlarmManager, definitive UI, Qibla, Quran and custom alarms remain separate future milestones; none is authorized or started by the Location closure.",
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 1 SPEC CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **NOT STARTED / NEXT** → STEP 3 schedule orchestration — NOT STARTED → STEP 4 presentation/countdown — NOT STARTED → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP.\n7. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and definitive Hero Dashboard remain separate future milestones and must not begin before this integration milestone is closed.",
)

changelog_anchor = "## 17. Change log\n\n"
changelog_entry = r'''### 2026-08-30 — Prayer Engine + Location integration STEP 1 architecture approved and documented

A new seven-step integration milestone is **OPEN** after the Prayer Engine and Location milestones closed independently. STEP 1 is spec-first only: no production code is authorized in this step. The approved `PrayerScheduleRepository` observes closed Location resolution state plus persisted `PrayerCalculationSettings`, forwards the exact `SelectedLocation.coordinates + zoneId` into the unchanged `PrayerTimeCalculator`, derives current selected-zone `LocalDate`, calculates today plus tomorrow only as needed for next-prayer continuity, and exposes controlled schedule/no-location/calculation-unavailable state to presentation. Calculation inputs are deduplicated; latest-input cancellation prevents stale results from overwriting a newer location. Recalculation occurs only for a distinct selected Location, Prayer settings, local-day change, bootstrap/restore or meaningful explicit refresh; countdown ticks never invoke prayer calculation. Local midnight scheduling is based on `atStartOfDay(selectedZoneId)` rather than a fixed 24-hour delay.

Prayer settings will use separate keys in the existing Preferences DataStore. Until a dedicated settings UI exists, first initialization persists MWL + STANDARD Asr + AUTOMATIC high-latitude rule + zero manual offsets. This default is explicitly documented as a **known temporary product limitation**: users in Saudi Arabia or any region/community whose local convention differs from MWL may see times that differ slightly from local practice until method/Asr/high-latitude/offset selection is exposed in its own future settings UI. Arihna will not auto-switch methods based on location. UI scope for this milestone is a minimal functional Home Prayer Schedule panel (next prayer, time, countdown, complete current-day schedule and secondary method label); the closed Location panel remains in `Impostazioni`, and the definitive Hero Dashboard stays deferred. Error policy remains strict: any non-Ready Location produces no schedule, and `PrayerCalculationResult.Unavailable` never gains a fabricated fallback. STEP 2 Prayer settings persistence is **NOT STARTED / NEXT** and requires explicit authorization.

'''
replace_once(changelog_anchor, changelog_anchor + changelog_entry)

path.write_text(text, encoding="utf-8")
