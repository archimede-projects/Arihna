from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text()

replacements = {
    "3. **STEP 3 — schedule orchestration: IN PROGRESS.** Implement `PrayerScheduleRepository` using only closed Location state, closed `PrayerTimeCalculator`, persisted Prayer settings and an injected testable `Clock`; cover the approved pure fake-Location/fake/recording-calculator contract matrix. No UI, presentation/countdown, Prayer Engine changes, Location changes or Prayer Settings changes are authorized in STEP 3.":
    "3. **STEP 3 — schedule orchestration: CLOSED.** Clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f`, built directly above spec-first commit `bb972d71c42f86e88f18d05144c61b91dcfa1dd3`, contains only `PrayerScheduleModels.kt`, `PrayerScheduleRepository.kt`, `DefaultPrayerScheduleRepository.kt`, and `DefaultPrayerScheduleRepositoryTest.kt`. Definitive exact-SHA run `33357287019` / job `99381759479` passed the complete host unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35 tests, 0 failed, 0 skipped**) while preserving the frozen GeoNames asset/policy checks. The pure JVM orchestration suite covers the approved 80-case matrix, including exact Device/Manual coordinate+ZoneId forwarding, input dedup, selected-zone midnight/DST, today/tomorrow continuity, controlled unavailable states, and explicit monotonic-generation protection against stale Roma→Milano results from a synchronous non-cooperative calculator. No UI, presentation/countdown, Prayer Engine, Location, or Prayer Settings behavior changed. STEP 4 remains **NOT STARTED** and requires separate authorization.",
    "### Prayer Engine + Location integration milestone — OPEN / STEP 3 IN PROGRESS":
    "### Prayer Engine + Location integration milestone — OPEN / STEP 3 CLOSED",
    "**STEP 3 schedule orchestration is IN PROGRESS** after explicit authorization; STEP 4 remains NOT STARTED and requires confirmation after STEP 3 closure.":
    "**STEP 3 schedule orchestration is CLOSED** on clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive exact-SHA run `33357287019` / job `99381759479` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). STEP 4 remains **NOT STARTED** and requires separate authorization.",
    "STEP 4 remains **NOT STARTED** until STEP 3 is closed on a new gate that tests the exact clean candidate SHA.":
    "STEP 3 is **CLOSED** on exact clean candidate `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive run `33357287019` / job `99381759479`; STEP 4 remains **NOT STARTED** and requires separate authorization."
}

for old, new in replacements.items():
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match, got {count}: {old[:100]!r}")
    text = text.replace(old, new, 1)

marker = "## 17. Change log\n"
if text.count(marker) != 1:
    raise SystemExit("Change log marker missing or duplicated")

entry = """

### 2026-08-31 — Prayer Engine + Location integration STEP 3 CLOSED after exact clean-candidate gate

STEP 3 schedule orchestration is **CLOSED**. The final technical candidate `2476ae86f585a6849f6f2104cddd215c6abf7d0f` was reconstructed directly above spec-first commit `bb972d71c42f86e88f18d05144c61b91dcfa1dd3` as one clean commit containing only the three Prayer Schedule domain/orchestration production files plus `DefaultPrayerScheduleRepositoryTest.kt`; no temporary development workflow/script or incremental history entered the candidate. Definitive run `33357287019` / job `99381759479` checked out that exact SHA, preserved the frozen GeoNames SHA/policy gate, passed `testDebugUnitTest` and `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **35/35 tests, 0 failed and 0 skipped**. The same final orchestration test blob had already executed the full **80/80 JVM** contract matrix in development run `33346275711`; the exact clean-candidate run then revalidated that unchanged test suite successfully. The repository uses exact Ready Location coordinates/ZoneId, deduplicated `PrayerScheduleInput`, selected-zone/DST-aware midnight boundaries, today-plus-tomorrow next-prayer continuity, controlled unavailable states, and a monotonic generation guard so a stale synchronous calculation cannot overwrite a newer location even when coroutine cancellation is not cooperatively observed. No UI, ViewModel/countdown, Prayer Engine, Location, or Prayer Settings behavior was changed. The clean candidate was promoted to `main` by non-forced fast-forward only after an ahead-1/behind-0 anti-divergence check. **STEP 4 remains NOT STARTED and requires separate authorization.**
"""
text = text.replace(marker, marker + entry, 1)

if "STEP 4 remains **NOT STARTED**" not in text:
    raise SystemExit("STEP 4 NOT STARTED invariant missing")

path.write_text(text)
