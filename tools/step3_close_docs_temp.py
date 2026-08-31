from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text()

old = "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 3 IN PROGRESS.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **IN PROGRESS** → STEP 4 presentation/countdown — **NOT STARTED** → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP. STEP 3 is explicitly authorized; no STEP 4 work is authorized before STEP 3 closure and confirmation."
new = "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 3 CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **CLOSED** → STEP 3 schedule orchestration — **CLOSED** → STEP 4 presentation/countdown — **NOT STARTED** → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP. STEP 3 is closed on clean technical commit `2476ae86f585a6849f6f2104cddd215c6abf7d0f` after definitive exact-SHA run `33357287019` / job `99381759479`; no STEP 4 work is authorized without separate confirmation."

count = text.count(old)
if count != 1:
    raise SystemExit(f"Expected exactly one milestone-sequence match, got {count}")

text = text.replace(old, new, 1)

if "MILESTONE OPEN / STEP 3 IN PROGRESS" in text:
    raise SystemExit("Residual current STEP 3 IN PROGRESS marker remains")
if "STEP 3 schedule orchestration — **IN PROGRESS**" in text:
    raise SystemExit("Residual STEP 3 sequence IN PROGRESS marker remains")
if "STEP 4 presentation/countdown — **NOT STARTED**" not in text:
    raise SystemExit("STEP 4 NOT STARTED invariant missing")

path.write_text(text)
