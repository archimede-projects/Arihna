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
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 2 IN PROGRESS",
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 2 CLOSED",
    "milestone heading",
)

replace_once(
    "2. **STEP 2 — Prayer settings persistence: IN PROGRESS.** Implement `PrayerSettingsRepository` with the exact same existing Preferences DataStore instance used by Location (the current `name = \"location\"` store is not renamed or migrated), Prayer-only keys `prayer.method`, `prayer.asr`, `prayer.high_latitude_rule`, and `prayer.offset.*`, canonical-default materialization/recovery, focused JVM tests, and real API28 shared-DataStore isolation tests. No UI and no changes to PrayerTimeCalculator, Location behavior, or existing Location keys.",
    "2. **STEP 2 — Prayer settings persistence: CLOSED.** `PrayerSettingsRepository` is implemented on the exact same existing Preferences DataStore instance used by Location (the current `name = \"location\"` store remains unchanged), with Prayer-only keys `prayer.method`, `prayer.asr`, `prayer.high_latitude_rule`, and `prayer.offset.*`. First use materializes the complete canonical MWL/STANDARD/AUTOMATIC/zero-offset record; partial or malformed Prayer data is recovered atomically to that same complete canonical record without clearing or rewriting Location entries. Focused JVM coverage and real Android 9/API28 shared-DataStore instrumentation passed on exact clean technical commit `ffb391846481a38cca0786be238bddae912ba862` in definitive run `33338723836` / job `99330268698`, including **35/35 instrumentation tests, 0 failed and 0 skipped**. No UI, `PrayerTimeCalculator`, Location behavior, or existing Location keys were changed.",
    "STEP 2 sequence row",
)

replace_once(
    "STEP 2 is explicitly authorized and in progress. It is limited to Prayer settings persistence and verification; STEP 3 schedule orchestration must not begin until STEP 2 is closed and explicitly confirmed. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.",
    "STEP 2 is **CLOSED** after exact-SHA persistence verification and promotion. STEP 3 schedule orchestration remains **NOT STARTED** and requires separate explicit authorization before any implementation begins. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.",
    "STEP 2 authorization paragraph",
)

replace_once(
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 2 IN PROGRESS.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **IN PROGRESS** → STEP 3 schedule orchestration — NOT STARTED → STEP 4 presentation/countdown — NOT STARTED → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP.",
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 2 CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **NOT STARTED** → STEP 4 presentation/countdown — NOT STARTED → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP. No STEP 3 work is authorized by the STEP 2 closure.",
    "current milestone sequence",
)

closure_evidence = """#### STEP 2 closure evidence

STEP 2 was closed only after a clean technical candidate was rebuilt directly above the STEP 2 spec-first commit and passed a new gate on its exact SHA. Definitive technical commit `ffb391846481a38cca0786be238bddae912ba862` passed workflow run `33338723836` / job `99330268698`: `testDebugUnitTest` passed, `assembleDebug` passed, and Android 9/API28 `connectedDebugAndroidTest` started and finished **35/35 tests with 0 failed and 0 skipped**. The workflow also restored and verified the frozen GeoNames asset and preserved the existing COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy checks.

The real persistence instrumentation uses `PreferenceDataStoreFactory` and passes the **same `DataStore<Preferences>` instance** to the Prayer and existing Location repositories. It verifies canonical first-use materialization, restart persistence, custom settings, positive/negative/zero offsets, dedicated recovery cases for partial records, unknown enum values and wrong stored types, and explicit isolation in both directions: Prayer reads/writes preserve existing `location.*` entries, and Location reads/writes preserve existing `prayer.*` entries. Recovery rewrites only the complete Prayer record and never calls a DataStore-wide clear.

Diagnostic history is retained for traceability but is not definitive gate evidence: the first attempt of run `33337223313` timed out in ddmlib while installing the APK and executed **0 tests**; its rerun then exposed only a JUnit test-harness signature error (`@After tearDown()` inferred a non-void return). That teardown was corrected without changing production code, and corrected development run `33338267865` passed **35/35 tests, 0 failed and 0 skipped** before the clean exact-SHA candidate received the independent definitive gate above.

No STEP 2 production change touched `PrayerTimeCalculator`, Location behavior, existing Location keys, or UI. The documented MWL/STANDARD/AUTOMATIC bootstrap-default limitation remains tracked until the separately scoped Prayer settings UI exists. STEP 3 is **NOT STARTED**.

"""
anchor = "#### Recalculation policy\n"
if text.count(anchor) != 1:
    raise SystemExit(f"closure evidence anchor: expected exactly one match, found {text.count(anchor)}")
if "#### STEP 2 closure evidence" in text:
    raise SystemExit("closure evidence already present")
text = text.replace(anchor, closure_evidence + anchor, 1)

changelog_entry = """### 2026-08-31 — Prayer Engine + Location integration STEP 2 CLOSED after exact-SHA persistence gate

STEP 2 Prayer settings persistence is **CLOSED**. Clean technical commit `ffb391846481a38cca0786be238bddae912ba862`, built directly above spec-first commit `e10b7cc2780556ca3635ac47f5057059f2720755`, passed definitive workflow run `33338723836` / job `99330268698`: `testDebugUnitTest` and `assembleDebug` were successful, and Android 9/API28 `connectedDebugAndroidTest` completed **35/35 tests with 0 failed and 0 skipped**. The implementation shares the exact existing Preferences DataStore instance/file used by Location without renaming or migrating it, persists only the approved `prayer.*` keys, materializes the complete MWL/STANDARD/AUTOMATIC/zero-offset default on first use, and atomically recovers malformed or partial Prayer records to that canonical value. Real shared-DataStore instrumentation proves Prayer↔Location isolation in both directions rather than assuming isolation from prefixes. The earlier ddmlib install timeout in run `33337223313` executed zero tests and was infrastructure-only; its rerun exposed a JUnit teardown-signature issue in the new test harness, corrected without production-code changes before corrected development run `33338267865` and the definitive clean-candidate gate. No UI, `PrayerTimeCalculator`, Location behavior, or existing Location keys changed. STEP 3 remains **NOT STARTED** and is not authorized by this closure.

"""
change_anchor = "## 17. Change log\n\n"
if text.count(change_anchor) != 1:
    raise SystemExit(f"change log anchor: expected exactly one match, found {text.count(change_anchor)}")
if "2026-08-31 — Prayer Engine + Location integration STEP 2 CLOSED" in text:
    raise SystemExit("STEP 2 closure changelog already present")
text = text.replace(change_anchor, change_anchor + changelog_entry, 1)

required = [
    "MILESTONE OPEN / STEP 2 CLOSED",
    "STEP 2 — Prayer settings persistence: CLOSED",
    "ffb391846481a38cca0786be238bddae912ba862",
    "33338723836",
    "99330268698",
    "35/35",
    "STEP 3 — schedule orchestration: **NOT STARTED**",
    "STEP 3 is **NOT STARTED**",
]
for marker in required:
    if marker not in text:
        raise SystemExit(f"missing required closure marker: {marker}")

path.write_text(text, encoding="utf-8")
