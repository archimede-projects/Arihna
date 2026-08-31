from pathlib import Path

path = Path("app/src/test/java/com/archimedeprojects/arihna/feature/prayerschedule/domain/DefaultPrayerScheduleRepositoryTest.kt")
text = path.read_text(encoding="utf-8")
old = "import kotlinx.coroutines.test.backgroundScope\n"
if text.count(old) != 1:
    raise SystemExit(f"expected exactly one backgroundScope import, found {text.count(old)}")
text = text.replace(old, "", 1)
path.write_text(text, encoding="utf-8")
