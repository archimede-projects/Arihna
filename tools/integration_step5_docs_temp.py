from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one occurrence, found {count}: {old[:120]!r}")
    text = text.replace(old, new, 1)


replace_once(
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 4 CLOSED",
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 5 CLOSED",
)

replace_once(
    "No Compose UI and no modification to `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or `AppContainer` wiring was introduced in STEP 4. **STEP 5 remains NOT STARTED and requires separate authorization.**",
    "No Compose UI and no modification to `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or `AppContainer` wiring was introduced in STEP 4. STEP 5 was separately authorized afterward and is now **CLOSED** under the evidence below; STEP 6 remains **NOT STARTED**.",
)

step5_evidence = """#### STEP 5 closure evidence

STEP 5 functional Home Prayer Schedule panel is **CLOSED** on clean technical commit `8b6a9c56a85e791691c41aa3d775788026b76bf0`, built directly above the STEP 4 docs-closed `main` commit `faebc922f04fe91576fe7987c593d1bdd1b46e15`. The final candidate changes only `ArihnaApp.kt`, `ArihnaNavHost.kt`, the new `HomePrayerScheduleScreen.kt`, and `HomePrayerScheduleScreenAndroidTest.kt`; no `PrayerScheduleViewModel`, `PrayerScheduleRepository`, Prayer Settings, Location, Prayer Engine, or `AppContainer` source was changed.

Definitive exact-SHA workflow run `33362751963` / job `99397193018` checked out `8b6a9c56a85e791691c41aa3d775788026b76bf0`, restored and verified the frozen GeoNames asset, preserved the `ACCESS_COARSE_LOCATION`-only / no-FINE / no-BACKGROUND / no-Play-Services policy checks, passed unfiltered `testDebugUnitTest`, passed `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **39/39 tests, 0 failed and 0 skipped**.

The bootstrap Home now consumes the already-closed `PrayerScheduleViewModel` through composition-root wiring in `ArihnaApp`/`ArihnaNavHost` and renders only the approved functional panel scope: readable active location/source context, next prayer name/time, live countdown, the complete current-day prayer schedule, and calculation method as secondary non-editable information. `NoLocation` shows an explicit location-required message plus a CTA to the existing Location panel in `Impostazioni`; `CalculationUnavailable` and `Loading` never expose fabricated prayer values or a misleading `--:--` placeholder. The four new Compose instrumentation cases cover NoLocation + CTA + absence of prayer values, Ready location/next prayer/countdown/full-day rendering, controlled calculation-unavailable UI, and Loading with no prayer values.

Diagnostic history is retained for traceability but is not definitive gate evidence: the first clean sibling candidate `db401c9022110e6f80eb7ebaec5c354ea16e42ad` passed host unit tests and `assembleDebug` in run `33362170081`, then failed before instrumentation execution because the new test source imported two Compose-test APIs unavailable to the resolved test surface (`assertDoesNotExist` and `onNode`). Production code was unchanged; only the test assertions were corrected, and the final candidate was rebuilt again directly above `faebc922f04fe91576fe7987c593d1bdd1b46e15` before receiving the independent 39/39 exact-SHA gate above.

The clean technical commit was promoted to `main` by non-forced fast-forward only after the exact-SHA gate passed. **STEP 6 full Prayer + Location + Integration regression remains NOT STARTED and requires separate authorization; no STEP 6 work is begun by this STEP 5 closure.**

"""
replace_once("#### Recalculation policy\n", step5_evidence + "#### Recalculation policy\n")

replace_once(
    "5. **STEP 5 — functional Home panel: NOT STARTED.** Render the minimal Prayer Schedule UI without final Hero Dashboard scope.",
    "5. **STEP 5 — functional Home panel: CLOSED.** Clean technical commit `8b6a9c56a85e791691c41aa3d775788026b76bf0`; definitive exact-SHA run `33362751963` / job `99397193018` passed `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 instrumentation with **39/39 tests, 0 failed and 0 skipped**. The bootstrap Home renders the approved minimal Prayer Schedule panel and preserves controlled no-location/unavailable/loading states without fabricated values.",
)

replace_once(
    "STEP 2 Prayer settings persistence, STEP 3 schedule orchestration, and STEP 4 presentation/countdown are **CLOSED** after their independent exact-SHA gates and promotion. STEP 5 functional Home panel remains **NOT STARTED** and requires separate explicit authorization. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.",
    "STEP 2 Prayer settings persistence, STEP 3 schedule orchestration, STEP 4 presentation/countdown, and STEP 5 functional Home panel are **CLOSED** after their independent exact-SHA gates and promotion. STEP 6 full Prayer + Location + Integration regression remains **NOT STARTED / NEXT** and requires separate explicit authorization. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.",
)

replace_once(
    "### Prayer Engine + Location integration milestone — OPEN / STEP 4 CLOSED",
    "### Prayer Engine + Location integration milestone — OPEN / STEP 5 CLOSED",
)

replace_once(
    "- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is CLOSED** after exact-SHA gate `33338723836` on `ffb391846481a38cca0786be238bddae912ba862`. **STEP 3 schedule orchestration is CLOSED** on clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive exact-SHA run `33357287019` / job `99381759479` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). **STEP 4 presentation/countdown is CLOSED** on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4` after definitive exact-SHA run `33360819887` / job `99391679780` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). STEP 5 remains **NOT STARTED** and requires separate authorization.",
    "- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is CLOSED** after exact-SHA gate `33338723836` on `ffb391846481a38cca0786be238bddae912ba862`. **STEP 3 schedule orchestration is CLOSED** on clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive exact-SHA run `33357287019` / job `99381759479` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). **STEP 4 presentation/countdown is CLOSED** on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4` after definitive exact-SHA run `33360819887` / job `99391679780` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**35/35, 0 failed, 0 skipped**). **STEP 5 functional Home panel is CLOSED** on clean technical commit `8b6a9c56a85e791691c41aa3d775788026b76bf0` after definitive exact-SHA run `33362751963` / job `99397193018` passed unit regression, `assembleDebug`, and Android 9/API28 instrumentation (**39/39, 0 failed, 0 skipped**). STEP 6 remains **NOT STARTED / NEXT** and requires separate authorization.",
)

replace_once(
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 4 CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **CLOSED** → STEP 4 presentation/countdown — **CLOSED** → STEP 5 functional Home panel — **NOT STARTED** → STEP 6 full Prayer+Location+Integration regression — **NOT STARTED** → STEP 7 docs-only closure — **NOT STARTED** → STOP. STEP 4 is closed on clean technical commit `5019906d0ef83a33d09f5a3295d736422c8af7a4` after definitive exact-SHA run `33360819887` / job `99391679780`; STEP 5 Compose/Home work remains unauthorized without separate confirmation.",
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 5 CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **CLOSED** → STEP 4 presentation/countdown — **CLOSED** → STEP 5 functional Home panel — **CLOSED** → STEP 6 full Prayer+Location+Integration regression — **NOT STARTED / NEXT** → STEP 7 docs-only closure — **NOT STARTED** → STOP. STEP 5 is closed on clean technical commit `8b6a9c56a85e791691c41aa3d775788026b76bf0` after definitive exact-SHA run `33362751963` / job `99397193018` completed **39/39 instrumentation tests, 0 failed and 0 skipped**; STEP 6 is not authorized or started by this closure.",
)

change_log = """### 2026-08-31 — Prayer Engine + Location integration STEP 5 CLOSED after exact clean-candidate gate

STEP 5 functional Home Prayer Schedule panel is **CLOSED**. Final clean technical candidate `8b6a9c56a85e791691c41aa3d775788026b76bf0` was rebuilt directly above STEP 4 docs-closed `main` commit `faebc922f04fe91576fe7987c593d1bdd1b46e15` and changes only `ArihnaApp.kt`, `ArihnaNavHost.kt`, `HomePrayerScheduleScreen.kt`, and `HomePrayerScheduleScreenAndroidTest.kt`. Definitive run `33362751963` / job `99397193018` checked out that exact SHA, restored/verified the frozen GeoNames asset, preserved the COARSE-only/no-FINE/no-BACKGROUND/no-Play-Services policy gate, passed `testDebugUnitTest` and `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **39/39 tests, 0 failed and 0 skipped**. Home now renders the approved minimal location/source, next-prayer/time/countdown, complete current-day schedule, and secondary calculation method; NoLocation routes through a CTA to the existing Location settings panel, while CalculationUnavailable and Loading expose no fabricated values. The first sibling candidate `db401c9022110e6f80eb7ebaec5c354ea16e42ad` is diagnostic only: run `33362170081` failed before instrumentation execution on two unsupported Compose-test imports; only the test source was corrected before the final clean sibling was rebuilt and gated. The final candidate was promoted by non-forced fast-forward. **STEP 6 remains NOT STARTED / NEXT and requires separate authorization.**

"""
replace_once("## 17. Change log\n\n", "## 17. Change log\n\n" + change_log)

path.write_text(text, encoding="utf-8")
print("PROJECT_SPEC.md STEP 5 closure patch applied")
