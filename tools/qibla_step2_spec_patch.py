from pathlib import Path

SPEC = Path("PROJECT_SPEC.md")
text = SPEC.read_text(encoding="utf-8")
old_step = "2. **STEP 2 — pure bearing engine: NOT STARTED.** Implement Kaaba constant, pure `QiblaBearingCalculator`, angle utilities and JVM golden/error tests only. No Android sensors or Compose."
new_step = "2. **STEP 2 — pure bearing engine: AUTHORIZED / IN PROGRESS.** Implement Kaaba constant, pure `QiblaBearingCalculator`, angle utilities and JVM golden/error tests only. No Android sensors or Compose. Authorization was explicitly given on 2026-09-02 after STEP 1 promotion; implementation must remain within this exact scope."
old_tail = "STEP 2 is **NOT STARTED** and requires separate authorization after this spec-only commit is reviewed/promoted. No Qibla production code, sensor registration, UI, dependency or permission change is authorized by STEP 1."
new_tail = "STEP 2 is **AUTHORIZED / IN PROGRESS** after explicit user authorization on 2026-09-02. Only the pure bearing engine, angle utilities and JVM tests are authorized. No Android sensor registration, Compose Qibla UI, Location-provider change, dependency addition or permission change is authorized in STEP 2. STEP 3 remains **NOT STARTED** and requires separate authorization after STEP 2 closure."
if old_step not in text:
    raise SystemExit("STEP 2 sequence marker not found")
if old_tail not in text:
    raise SystemExit("STEP 2 authorization tail not found")
text = text.replace(old_step, new_step, 1).replace(old_tail, new_tail, 1)
SPEC.write_text(text, encoding="utf-8")
