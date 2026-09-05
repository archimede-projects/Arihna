import glob
import sys
import xml.etree.ElementTree as ET

expected_min = int(sys.argv[1])
required = sys.argv[2:]
files = glob.glob("app/build/outputs/androidTest-results/connected/debug/**/*.xml", recursive=True)
assert files, "no connected test XML"
tests = failures = errors = skipped = 0
names = []
for file in files:
    root = ET.parse(file).getroot()
    tests += int(root.attrib.get("tests", 0))
    failures += int(root.attrib.get("failures", 0))
    errors += int(root.attrib.get("errors", 0))
    skipped += int(root.attrib.get("skipped", 0))
    names.extend(tc.attrib.get("classname", "") for tc in root.iter("testcase"))
print(f"CONNECTED_TOTAL tests={tests} failures={failures} errors={errors} skipped={skipped}")
assert tests >= expected_min, (tests, expected_min)
assert failures == 0 and errors == 0 and skipped == 0
for needle in required:
    assert any(needle in name for name in names), f"missing connected class {needle}"
