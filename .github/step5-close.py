from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "### 5.2 Location — MILESTONE OPEN / STEP 5 IN PROGRESS",
        "### 5.2 Location — MILESTONE OPEN / STEP 5 CLOSED",
    ),
    (
        "The **Location milestone remains OPEN**. STEP 5 (Android `LocationManager` + Android permission/resolution flow) is **IN PROGRESS** under the already approved scope. STEP 6 (minimal functional Device/Manual UI) is **NOT STARTED** and must not begin until STEP 5 is closed and confirmed; STEP 7 remains the final full regression/closure gate for the milestone.",
        "The **Location milestone remains OPEN**. STEP 5 (Android `LocationManager` + Android permission/resolution flow) is **CLOSED** after definitive run `33317622881` on exact tested/promoted candidate `7f59c55da954347a1db5c17fe41c2cb07309184c`. STEP 6 (minimal functional Device/Manual UI) is **NOT STARTED / NEXT** and requires separate authorization before work begins; STEP 7 remains the final full regression/closure gate for the milestone.",
    ),
    (
        "- **STEP 5 — Android `LocationManager` + permission/resolution flow: IN PROGRESS.** Implement the real Android bridge and permission/service-state boundaries; keep timeout and significant-change acceptance in the existing domain layer; prove with host tests and API28 instrumentation before closure.",
        "- **STEP 5 — Android `LocationManager` + permission/resolution flow: CLOSED.** Exact promoted candidate `7f59c55da954347a1db5c17fe41c2cb07309184c`; definitive run `33317622881` passed `testDebugUnitTest`, `assembleDebug`, and Android 9/API28 instrumentation (21/21, 0 failed, 0 skipped), with COARSE-only/no-FINE/no-background/no-Play-Services policy intact. Real Galaxy S25 verification independently returned a successful approximate-location current fix through the unmodified production bridge.",
    ),
    (
        "- **STEP 6 — minimal functional Device/Manual Compose UI: NOT STARTED.** This work is already approved within the current Location milestone and follows STEP 5.",
        "- **STEP 6 — minimal functional Device/Manual Compose UI: NOT STARTED / NEXT.** It follows closed STEP 5 but must not begin until separately authorized.",
    ),
    (
        "4. **Current: Location (Device + manual city) — MILESTONE OPEN.** STEP 4 is closed; STEP 5 is now the active implementation objective; STEP 6/7 remain not started.",
        "4. **Current: Location (Device + manual city) — MILESTONE OPEN.** STEP 4 and STEP 5 are closed; STEP 6 is NOT STARTED / NEXT; STEP 7 remains not started.",
    ),
    (
        "5. Location sequence/status: STEP 1 spec commit — CLOSED → STEP 2 pure Kotlin domain/state/policies + fake tests — CLOSED → STEP 3 Preferences DataStore — CLOSED → STEP 4 GeoNames generation/read-only SQLite + APK-size measurement + CityRepository/timezone/data/API28 gate — **CLOSED** → STEP 5 Android `LocationManager` + permission/resolution — **IN PROGRESS** → STEP 6 minimal functional Device/Manual UI — **NOT STARTED** → STEP 7 full unit/build/API28 regression → dedicated implementation commit → STOP — **NOT STARTED**.",
        "5. Location sequence/status: STEP 1 spec commit — CLOSED → STEP 2 pure Kotlin domain/state/policies + fake tests — CLOSED → STEP 3 Preferences DataStore — CLOSED → STEP 4 GeoNames generation/read-only SQLite + APK-size measurement + CityRepository/timezone/data/API28 gate — **CLOSED** → STEP 5 Android `LocationManager` + permission/resolution — **CLOSED** → STEP 6 minimal functional Device/Manual UI — **NOT STARTED / NEXT** → STEP 7 full unit/build/API28 regression → dedicated implementation commit → STOP — **NOT STARTED**.",
    ),
]

for old, new in replacements:
    if old not in text:
        raise SystemExit(f"Missing expected STEP5 closure text: {old[:120]}")
    text = text.replace(old, new, 1)

marker = "### 2026-08-30 — STEP 5 API28 harness limit classified; real Galaxy S25 bridge verified\n"
entry = """### 2026-08-30 — Location STEP 5 CLOSED after final regression and exact-SHA promotion

STEP 5 is **CLOSED** while the overall Location milestone remains **OPEN**. Definitive workflow run `33317622881` tested exact candidate `7f59c55da954347a1db5c17fe41c2cb07309184c`: toolchain/permission policy and exact STEP 4 asset restore passed; `testDebugUnitTest` passed; `assembleDebug` passed; Android 9/API28 `connectedDebugAndroidTest` completed **21/21 tests with 0 failed and 0 skipped**. The same exact SHA was then fast-forward promoted to `location-step5`. The production bridge still requests only `ACCESS_COARSE_LOCATION`, contains no Play Services Location dependency, no background location and no foreground service; platform update requests use the 15-minute interval with no provider-level 5 km filter, leaving 5 km/ZoneId significance and the 20-second fresh-fix timeout in the domain layer. API28 synthetic COARSE delivery remains documented as a test-harness limitation; the CI cases stay active and verify real framework registration/cancellation rather than hiding the limitation with skips. Independent physical verification on a Samsung Galaxy S25 with approximate permission produced a real `SUCCESS` current fix through the unmodified bridge, 2000 m reported accuracy and `Europe/Rome`; exact coordinates are intentionally omitted. The temporary Device Test Compose hook remains only on `location-step5-device-test`/its diagnostic Release and is not part of the promoted STEP 5 branch or STEP 6. Physical Samsung significant-update movement, OEM Location Services toggle, and denied-permission UI interaction were not separately executed in this STEP; the first is a manual-device-only behavior under the 15-minute policy, while the latter UI prompt/rationale path belongs to STEP 6. STEP 6 is **NOT STARTED / NEXT** and must be separately authorized; STEP 7 remains **NOT STARTED**.

"""

if marker not in text:
    raise SystemExit("STEP5 harness changelog marker missing")
text = text.replace(marker, entry + marker, 1)
path.write_text(text, encoding="utf-8")
