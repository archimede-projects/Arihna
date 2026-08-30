from pathlib import Path

source_path = Path("tools/step2_close_spec_temp.py")
source = source_path.read_text(encoding="utf-8")
old = '    "STEP 3 — schedule orchestration: **NOT STARTED**",\n'
new = '    "3. **STEP 3 — schedule orchestration: NOT STARTED.**",\n'
if source.count(old) != 1:
    raise SystemExit(f"expected one marker check to patch, found {source.count(old)}")
source = source.replace(old, new, 1)
exec(compile(source, str(source_path), "exec"))
