from pathlib import Path
import sys
import xml.etree.ElementTree as ET

files = sorted(Path("app/build/outputs/androidTest-results/connected/debug").rglob("*.xml"))
if not files:
    raise SystemExit("No connected Android test XML files found")

tests = failures = errors = skipped = 0
for path in files:
    root = ET.parse(path).getroot()
    tests += int(root.attrib.get("tests", "0"))
    failures += int(root.attrib.get("failures", "0"))
    errors += int(root.attrib.get("errors", "0"))
    skipped += int(root.attrib.get("skipped", "0"))

print(f"ANDROID_TEST_XML files={len(files)} tests={tests} failures={failures} errors={errors} skipped={skipped}")
if tests <= 0 or failures or errors or skipped:
    sys.exit(1)
