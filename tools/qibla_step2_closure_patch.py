from pathlib import Path

spec = Path('PROJECT_SPEC.md')
text = spec.read_text(encoding='utf-8')
old_line = "2. **STEP 2 — pure bearing engine: AUTHORIZED / IN PROGRESS.** Implement Kaaba constant, pure `QiblaBearingCalculator`, angle utilities and JVM golden/error tests only. No Android sensors or Compose. Authorization was explicitly given on 2026-09-02 after STEP 1 promotion; implementation must remain within this exact scope."
new_line = "2. **STEP 2 — pure bearing engine: CLOSED.** Clean technical commit `c4bf58ab341bc69127d9075cd093a4c4080c6062`, built directly above STEP 2 authorization commit `1062bc33eda946351c799d694c4e8eecf849ace2`, adds only the frozen Kaaba target, pure `QiblaBearingCalculator`, angle utilities and focused JVM tests. Definitive exact-SHA run `33652882463` / job `100324058113` passed the full unit regression, `assembleDebug`, and Android 9/API28 connected regression. No sensors, Compose, Location-provider behavior, dependencies or permissions changed."
old_tail = "STEP 2 is **AUTHORIZED / IN PROGRESS** after explicit user authorization on 2026-09-02. Only the pure bearing engine, angle utilities and JVM tests are authorized. No Android sensor registration, Compose Qibla UI, Location-provider change, dependency addition or permission change is authorized in STEP 2. STEP 3 remains **NOT STARTED** and requires separate authorization after STEP 2 closure."
closure = """#### STEP 2 closure evidence — 2026-09-02

- STEP 2 authorization commit: `1062bc33eda946351c799d694c4e8eecf849ace2`.
- Definitive technical commit: `c4bf58ab341bc69127d9075cd093a4c4080c6062` (`feat(qibla): implement pure bearing engine`), exactly one technical commit above the authorization spec.
- Technical scope is exactly five new files under `core/qibla` and `src/test`; no existing production source, manifest, dependency, UI, Location, Prayer or integration file changed.
- Frozen Kaaba target: `21.42251267, 39.82619741`.
- Pure great-circle initial-bearing calculation is true-north referenced, normalized to `[0°, 360°)`, rejects invalid/non-finite coordinates, and returns controlled `AtKaabaOrCoincident` for the frozen target itself.
- Focused JVM coverage validates the frozen target, approved Rome/New York/Sydney/London/Jakarta golden bearings, invalid coordinates, coincident target, angle normalization and north wrap-around relative-direction math.
- Definitive exact-SHA gate: workflow run `33652882463`, job `100324058113`, `success`, checking out exactly `c4bf58ab341bc69127d9075cd093a4c4080c6062` with parent exactly `1062bc33eda946351c799d694c4e8eecf849ace2` and `main` still at the spec SHA during the gate.
- Host unit regression: **105/105**, 0 failures, 0 errors, 0 skipped, including **7 Qibla STEP 2 cases**.
- `assembleDebug`: PASS.
- Android 9/API28 connected regression: **41/41**, 0 failures, 0 errors, 0 skipped.
- Gate reverified frozen GeoNames SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`, COARSE-only policy, absence of FINE/BACKGROUND/BODY_SENSORS, absence of Google Play Services Location and absence of ignored tests.
- No physical-device validation is required for STEP 2 because no sensor/UI behavior exists yet; Galaxy S25 validation remains mandatory at Qibla STEP 6 after the live compass exists.

STEP 2 is **CLOSED**. STEP 3 Android heading/sensor layer remains **NOT STARTED** and requires separate explicit authorization. No sensor registration, UI or new permission is started by this closure.
"""
if old_line not in text:
    raise SystemExit('STEP 2 sequence line not found')
if old_tail not in text:
    raise SystemExit('STEP 2 tail not found')
text = text.replace(old_line, new_line, 1)
text = text.replace(old_tail, closure, 1)
spec.write_text(text, encoding='utf-8')
