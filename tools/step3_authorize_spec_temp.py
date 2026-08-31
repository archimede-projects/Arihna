from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 2 CLOSED",
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 3 IN PROGRESS",
    "integration heading",
)

replace_once(
    "3. **STEP 3 — schedule orchestration: NOT STARTED.** Implement `PrayerScheduleRepository` and pure fake-Location/fake-calculator contract tests.",
    "3. **STEP 3 — schedule orchestration: IN PROGRESS.** Implement `PrayerScheduleRepository` using only closed Location state, closed `PrayerTimeCalculator`, persisted Prayer settings and an injected testable `Clock`; cover the approved pure fake-Location/fake/recording-calculator contract matrix. No UI, presentation/countdown, Prayer Engine changes, Location changes or Prayer Settings changes are authorized in STEP 3.",
    "STEP 3 sequence row",
)

replace_once(
    "### Prayer Engine + Location integration milestone — OPEN / STEP 1 SPEC CLOSED",
    "### Prayer Engine + Location integration milestone — OPEN / STEP 3 IN PROGRESS",
    "decision-status heading",
)

replace_once(
    "- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is IN PROGRESS** after explicit authorization; STEP 3 remains NOT STARTED and requires confirmation after STEP 2 closure.",
    "- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is CLOSED** after exact-SHA gate `33338723836` on `ffb391846481a38cca0786be238bddae912ba862`. **STEP 3 schedule orchestration is IN PROGRESS** after explicit authorization; STEP 4 remains NOT STARTED and requires confirmation after STEP 3 closure.",
    "decision-status sequence",
)

replace_once(
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 2 CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **NOT STARTED** → STEP 4 presentation/countdown — NOT STARTED → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP. No STEP 3 work is authorized by the STEP 2 closure.",
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 3 IN PROGRESS.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **IN PROGRESS** → STEP 4 presentation/countdown — **NOT STARTED** → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP. STEP 3 is explicitly authorized; no STEP 4 work is authorized before STEP 3 closure and confirmation.",
    "milestone sequence current row",
)

authorization = """#### STEP 3 authorization / scope\n\nSTEP 3 schedule orchestration is explicitly authorized after STEP 2 closure. Implement `PrayerScheduleRepository.observeSchedule(): Flow<PrayerScheduleState>` plus idempotent `refresh()` exactly within the already-approved orchestration contract: consume closed `LocationResolutionState`, persisted `PrayerCalculationSettings` and an injected testable `Clock`; derive `PrayerScheduleInput(coordinates, zoneId, settings, localDate)` from only `LocationResolutionState.Ready`; deduplicate identical inputs; use cancellation-aware latest-input semantics (`mapLatest`/equivalent) so stale calculations cannot overwrite newer selections; calculate today and only the following day when needed for next-prayer continuity; use selected-zone `Instant`/`ZonedDateTime` semantics and DST-aware next-local-midnight boundaries; and recalculate only for the inputs/events already enumerated in the Recalculation policy. A same-input refresh may reuse in-memory calculated days and re-derive next-prayer state without invoking the calculator.\n\nSTEP 3 tests must be pure/JVM orchestration contract tests using fake Location state and a fake/recording `PrayerTimeCalculator`; no real GPS, `LocationManager`, Android geocoder, Adhan internals or prayer-formula retesting. Cover the complete STEP 1 matrix, including all non-Ready states → zero calculator calls, exact Manual/Device coordinate+ZoneId forwarding, Device/Manual/input/settings/date changes, identical-input dedup, raw non-accepted update non-effect, bootstrap once, selected-zone midnight including DST and replacement after ZoneId change, before-first/after-last next-prayer behavior, controlled `CalculationUnavailable`, today-valid/tomorrow-unavailable preservation, and stale Roma→Milano cancellation/race protection.\n\nNo UI, `PrayerScheduleViewModel`, countdown ticker, Home panel, `PrayerTimeCalculator` implementation/formula change, Location behavior/key change, or Prayer Settings behavior/key change is authorized in STEP 3. STEP 4 remains **NOT STARTED** until STEP 3 is closed on a new gate that tests the exact clean candidate SHA.\n\n"""
anchor = "#### Recalculation policy\n"
if text.count(anchor) != 1:
    raise SystemExit(f"authorization anchor: expected exactly one match, found {text.count(anchor)}")
if "#### STEP 3 authorization / scope" in text:
    raise SystemExit("STEP 3 authorization already present")
text = text.replace(anchor, authorization + anchor, 1)

changelog = """### 2026-08-31 — Prayer Engine + Location integration STEP 3 schedule orchestration authorized\n\nSTEP 2 is formally closed on `main` at docs-only commit `ad6d4ac5d81230e613b09547bd45a7c44035ddba`. STEP 3 is now explicitly authorized and limited to `PrayerScheduleRepository` orchestration plus pure/JVM contract tests with fake Location and fake/recording `PrayerTimeCalculator`. Implement the already-specified `PrayerScheduleInput` deduplication, cancellation-aware latest-input behavior, exact Ready Location coordinate/ZoneId forwarding, selected-zone local-date and DST-aware midnight handling, today+tomorrow next-prayer continuity and controlled unavailable states. No UI/presentation/countdown/Home work and no changes to the closed Prayer Engine, Location or Prayer Settings behavior are authorized. The technical candidate must be rebuilt cleanly above this spec-first commit and receive a new full gate on its exact SHA before promotion; STEP 4 remains **NOT STARTED** until STEP 3 closure is reported and confirmed.\n\n"""
change_anchor = "## 17. Change log\n\n"
if text.count(change_anchor) != 1:
    raise SystemExit(f"change log anchor: expected exactly one match, found {text.count(change_anchor)}")
if "2026-08-31 — Prayer Engine + Location integration STEP 3 schedule orchestration authorized" in text:
    raise SystemExit("STEP 3 authorization changelog already present")
text = text.replace(change_anchor, change_anchor + changelog, 1)

required = [
    "MILESTONE OPEN / STEP 3 IN PROGRESS",
    "STEP 3 — schedule orchestration: IN PROGRESS",
    "#### STEP 3 authorization / scope",
    "fake/recording `PrayerTimeCalculator`",
    "STEP 4 remains **NOT STARTED**",
    "ad6d4ac5d81230e613b09547bd45a7c44035ddba",
]
for marker in required:
    if marker not in text:
        raise SystemExit(f"missing required STEP 3 authorization marker: {marker}")

path.write_text(text, encoding="utf-8")
