from pathlib import Path

path = Path("app/src/test/java/com/archimedeprojects/arihna/feature/prayerschedule/domain/DefaultPrayerScheduleRepositoryTest.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    "import kotlinx.coroutines.Dispatchers\n",
    "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.ExperimentalCoroutinesApi\n",
    "ExperimentalCoroutinesApi import",
)
replace_once(
    "class DefaultPrayerScheduleRepositoryTest {\n",
    "@OptIn(ExperimentalCoroutinesApi::class)\nclass DefaultPrayerScheduleRepositoryTest {\n",
    "test class opt-in",
)
replace_once(
    '        val initial = Instant.parse("2026-01-01T22:30:00Z")\n',
    '        val initial = Instant.parse("2026-01-01T18:00:00Z")\n',
    "zone test initial instant",
)
replace_once(
    '        clock.setInstant(Instant.parse("2026-01-01T23:00:00Z"))\n        advanceTimeBy(Duration.ofMinutes(30).toMillis())\n',
    '        clock.setInstant(Instant.parse("2026-01-01T23:00:00Z"))\n        advanceTimeBy(Duration.ofHours(5).toMillis())\n',
    "zone test old-boundary advance",
)
replace_once(
    "        locationStates.value = ready(milano)\n        calculator.releaseFirst.countDown()\n",
    "        locationStates.value = ready(milano)\n        Thread.sleep(100)\n        calculator.releaseFirst.countDown()\n",
    "race observation window",
)

path.write_text(text, encoding="utf-8")
