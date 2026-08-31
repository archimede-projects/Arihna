from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one match, found {count}: {old[:120]!r}")
    text = text.replace(old, new, 1)

replace_once(
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 4 IN PROGRESS",
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 4 CLOSED",
)

replace_once(
    "No UI, `PrayerScheduleViewModel`, countdown ticker, Home panel, `PrayerTimeCalculator` implementation/formula change, Location behavior/key change, or Prayer Settings behavior/key change is authorized in STEP 3. STEP 3 is **CLOSED** on exact clean candidate `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive run `33357287019` / job `99381759479`. STEP 4 presentation/countdown is now **IN PROGRESS** under the separately authorized scope below.",
    "No UI, `PrayerScheduleViewModel`, countdown ticker, Home panel, `PrayerTimeCalculator` implementation/formula change, Location behavior/key change, or Prayer Settings behavior/key change is authorized in STEP 3. STEP 3 is **CLOSED** on exact clean candidate `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive run `33357287019` / job `99381759479`. STEP 4 presentation/countdown was separately authorized afterward and is now **CLOSED** under the scope and evidence below.",
)

step4_scope_tail = "Pure/JVM tests use a fake/recording `PrayerScheduleRepository`, controlled `Clock`, and manual/test ticker. Required coverage includes NoLocation mapping/message, Ready next-prayer/countdown mapping, countdown decrement with no repository calls, exactly-once refresh when countdown reaches zero, transition to the next prayer after the fake repository emits the advanced cached state, `CalculationUnavailable`, and proof that ticker activity alone never causes spurious refresh/calculation behavior. No change to `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or Home Compose UI is authorized. STEP 5 remains **NOT STARTED**."
step4_closure = step4_scope_tail + "\n\n#### STEP 4 closure evidence\n\nSTEP 4 presentation/countdown is **CLOSED** on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4`, built directly above the STEP 4 spec-first commit `fa16c35330c3540671eee681b352ea22df0c5d48` as one commit containing only `PrayerSchedulePresentation.kt`, `PrayerScheduleTicker.kt`, `PrayerScheduleViewModel.kt`, and `PrayerScheduleViewModelTest.kt`. Definitive exact-SHA workflow run `33360819887` / job `99391679780` checked out `5019906d0ef83a33d09f5a3295d736422c8af7a4`, restored and verified the frozen GeoNames asset, preserved the COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy checks, passed unfiltered `testDebugUnitTest` and `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **35/35 tests, 0 failed and 0 skipped**. The presentation boundary depends only on `PrayerScheduleRepository`, injected `Clock`, and a testable ticker; the production ticker cadence is one second, ordinary ticks only update `targetInstant - Clock.instant()` and never call the repository, and an expired target is guarded so `refresh()` is requested once while awaiting an advanced repository state. The focused pure/JVM ViewModel suite covers NoLocation mapping/message, Ready next-prayer/countdown, countdown decrement without refresh, exactly-once refresh at zero followed by the next repository-provided prayer, `CalculationUnavailable`, and no refresh when no next prayer exists. No Compose UI and no modification to `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or `AppContainer` wiring was introduced in STEP 4. **STEP 5 remains NOT STARTED and requires separate authorization.**"
replace_once(step4_scope_tail, step4_closure)

replace_once(
    "3. **STEP 3 — schedule orchestration: CLOSED.** Clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f`, built directly above spec-first commit `bb972d71c42f86e88f18d05144c61b91dcfa1dd3`, contains only `PrayerScheduleModels.kt`, `PrayerScheduleRepository.kt`, `DefaultPrayerScheduleRepository.kt`, and `DefaultPrayerScheduleRepositoryTest.kt`. Definitive exact-SHA run `33357287019` / job `99381759479` passed the complete host unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35 tests, 0 failed, 0 skipped**) while preserving the frozen GeoNames asset/policy checks. The pure JVM orchestration suite covers the approved 80-case matrix, including exact Device/Manual coordinate+ZoneId forwarding, input dedup, selected-zone midnight/DST, today/tomorrow continuity, controlled unavailable states, and explicit monotonic-generation protection against stale Roma→Milano results from a synchronous non-cooperative calculator. No UI, presentation/countdown, Prayer Engine, Location, or Prayer Settings behavior changed. STEP 4 remains **NOT STARTED** and requires separate authorization.",
    "3. **STEP 3 — schedule orchestration: CLOSED.** Clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f`, built directly above spec-first commit `bb972d71c42f86e88f18d05144c61b91dcfa1dd3`, contains only `PrayerScheduleModels.kt`, `PrayerScheduleRepository.kt`, `DefaultPrayerScheduleRepository.kt`, and `DefaultPrayerScheduleRepositoryTest.kt`. Definitive exact-SHA run `33357287019` / job `99381759479` passed the complete host unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35 tests, 0 failed, 0 skipped**) while preserving the frozen GeoNames asset/policy checks. The pure JVM orchestration suite covers the approved 80-case matrix, including exact Device/Manual coordinate+ZoneId forwarding, input dedup, selected-zone midnight/DST, today/tomorrow continuity, controlled unavailable states, and explicit monotonic-generation protection against stale Roma→Milano results from a synchronous non-cooperative calculator. No UI, presentation/countdown, Prayer Engine, Location, or Prayer Settings behavior changed. STEP 4 was separately authorized afterward and is now **CLOSED** below.",
)

replace_once(
    "4. **STEP 4 — presentation/countdown: IN PROGRESS.** Implement `PrayerScheduleViewModel` plus pure presentation models and a testable one-second ticker. The ViewModel depends only on `PrayerScheduleRepository`, injected `Clock`, and ticker; maps domain state to `PrayerScheduleUiState`; updates countdown from `targetInstant - Clock.instant()` without recalculation; and calls repository `refresh()` only when an active countdown reaches zero so STEP 3 can re-derive the next prayer from its existing today/tomorrow cache. Pure ticker events before zero must not call the repository. No Compose UI or changes to PrayerScheduleRepository, Prayer Settings, Location, or Prayer Engine are authorized.",
    "4. **STEP 4 — presentation/countdown: CLOSED.** Clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4`, built directly above spec-first commit `fa16c35330c3540671eee681b352ea22df0c5d48`, adds only the pure presentation state, testable one-second ticker, `PrayerScheduleViewModel`, and focused JVM ViewModel tests. Definitive exact-SHA run `33360819887` / job `99391679780` passed unfiltered `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 instrumentation (**35/35 tests, 0 failed, 0 skipped**) with policy/GeoNames gates intact. Countdown ticks never call the repository before expiry; each expired target is guarded to request one idempotent `refresh()` while waiting for the repository to expose the next prayer. No Compose UI or changes to PrayerScheduleRepository, Prayer Settings, Location, Prayer Engine, or AppContainer wiring were made. STEP 5 remains **NOT STARTED** and requires separate authorization.",
)

replace_once(
    "STEP 2 is **CLOSED** after exact-SHA persistence verification and promotion. STEP 3 schedule orchestration remains **NOT STARTED** and requires separate explicit authorization before any implementation begins. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.",
    "STEP 2 Prayer settings persistence, STEP 3 schedule orchestration, and STEP 4 presentation/countdown are **CLOSED** after their independent exact-SHA gates and promotion. STEP 5 functional Home panel remains **NOT STARTED** and requires separate explicit authorization. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.",
)

replace_once(
    "### Prayer Engine + Location integration milestone — OPEN / STEP 4 IN PROGRESS",
    "### Prayer Engine + Location integration milestone — OPEN / STEP 4 CLOSED",
)

replace_once(
    "- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is CLOSED** after exact-SHA gate `33338723836` on `ffb391846481a38cca0786be238bddae912ba862`. **STEP 3 schedule orchestration is CLOSED** on clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive exact-SHA run `33357287019` / job `99381759479` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). **STEP 4 presentation/countdown is IN PROGRESS** after explicit authorization; STEP 5 remains **NOT STARTED**.",
    "- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is CLOSED** after exact-SHA gate `33338723836` on `ffb391846481a38cca0786be238bddae912ba862`. **STEP 3 schedule orchestration is CLOSED** on clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive exact-SHA run `33357287019` / job `99381759479` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). **STEP 4 presentation/countdown is CLOSED** on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4` after definitive exact-SHA run `33360819887` / job `99391679780` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). STEP 5 remains **NOT STARTED** and requires separate authorization.",
)

replace_once(
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 4 IN PROGRESS.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **CLOSED** → STEP 4 presentation/countdown — **IN PROGRESS** → STEP 5 functional Home panel — **NOT STARTED** → STEP 6 full Prayer+Location+Integration regression — **NOT STARTED** → STEP 7 docs-only closure — **NOT STARTED** → STOP. STEP 4 is explicitly authorized only for ViewModel/presentation/countdown plus pure JVM tests; STEP 5 Compose UI remains unauthorized.",
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 4 CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **CLOSED** → STEP 4 presentation/countdown — **CLOSED** → STEP 5 functional Home panel — **NOT STARTED** → STEP 6 full Prayer+Location+Integration regression — **NOT STARTED** → STEP 7 docs-only closure — **NOT STARTED** → STOP. STEP 4 is closed on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4` after definitive exact-SHA run `33360819887` / job `99391679780`; STEP 5 Compose/Home work remains unauthorized without separate confirmation.",
)

changelog_anchor = "## 17. Change log\n\n### 2026-08-31 — Prayer Engine + Location integration STEP 4 presentation/countdown authorized"
closure_entry = "## 17. Change log\n\n### 2026-08-31 — Prayer Engine + Location integration STEP 4 CLOSED after exact clean-candidate gate\n\nSTEP 4 presentation/countdown is **CLOSED**. The final technical candidate `5019906d0ef83a33d09f5a3295d736422c8af7a4` was reconstructed directly above STEP 4 spec-first commit `fa16c35330c3540671eee681b352ea22df0c5d48` as one clean commit containing only `PrayerSchedulePresentation.kt`, `PrayerScheduleTicker.kt`, `PrayerScheduleViewModel.kt`, and `PrayerScheduleViewModelTest.kt`. Definitive run `33360819887` / job `99391679780` checked out that exact SHA, restored the frozen GeoNames asset with the approved SHA, preserved the COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy gate, passed unfiltered `testDebugUnitTest` and `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **35/35 tests, 0 failed and 0 skipped**. The ViewModel depends only on `PrayerScheduleRepository`, injected `Clock`, and a testable ticker; the production ticker cadence is one second, normal ticks update only the remaining duration, and a target that reaches zero is guarded so repository `refresh()` is requested once while awaiting the next cached schedule state. Focused pure/JVM tests cover NoLocation, Ready next-prayer/countdown, countdown decrement without repository calls, exactly-once zero refresh plus next-prayer transition, controlled `CalculationUnavailable`, and no refresh when no next prayer exists. No Compose/Home UI, `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or AppContainer wiring changed. The clean candidate was promoted to `main` by non-forced fast-forward after an ahead-1/behind-0 anti-divergence check. **STEP 5 remains NOT STARTED and requires separate authorization.**\n\n### 2026-08-31 — Prayer Engine + Location integration STEP 4 presentation/countdown authorized"
replace_once(changelog_anchor, closure_entry)

path.write_text(text, encoding="utf-8")

required = [
    "MILESTONE OPEN / STEP 4 CLOSED",
    "STEP 4 presentation/countdown is **CLOSED** on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4`",
    "Definitive exact-SHA workflow run `33360819887` / job `99391679780`",
    "STEP 5 remains **NOT STARTED** and requires separate authorization",
]
for marker in required:
    if marker not in text:
        raise SystemExit(f"missing required marker: {marker}")

for forbidden in [
    "MILESTONE OPEN / STEP 4 IN PROGRESS",
    "STEP 4 presentation/countdown is IN PROGRESS",
    "STEP 4 presentation/countdown — **IN PROGRESS**",
    "STEP 3 schedule orchestration remains **NOT STARTED**",
]:
    if forbidden in text:
        raise SystemExit(f"stale marker remains: {forbidden}")
