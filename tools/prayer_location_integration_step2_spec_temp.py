from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")

replacements = {
    "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 1 SPEC CLOSED":
        "### 5.3 Prayer Engine + Location integration — MILESTONE OPEN / STEP 2 IN PROGRESS",
    "2. **STEP 2 — Prayer settings persistence: NOT STARTED / NEXT.** Implement `PrayerSettingsRepository`, same-DataStore Prayer keys/default initialization and focused tests.":
        "2. **STEP 2 — Prayer settings persistence: IN PROGRESS.** Implement `PrayerSettingsRepository` with the exact same existing Preferences DataStore instance used by Location (the current `name = \"location\"` store is not renamed or migrated), Prayer-only keys `prayer.method`, `prayer.asr`, `prayer.high_latitude_rule`, and `prayer.offset.*`, canonical-default materialization/recovery, focused JVM tests, and real API28 shared-DataStore isolation tests. No UI and no changes to PrayerTimeCalculator, Location behavior, or existing Location keys.",
    "No STEP 2 implementation may begin until STEP 1 is explicitly confirmed closed. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.":
        "STEP 2 is explicitly authorized and in progress. It is limited to Prayer settings persistence and verification; STEP 3 schedule orchestration must not begin until STEP 2 is closed and explicitly confirmed. Qibla, notifications/AlarmManager, adhan audio, custom alarms, Quran and the definitive dashboard remain separate milestones.",
    "- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is NOT STARTED / NEXT and requires explicit authorization after this STEP 1 commit.**":
        "- Seven-step sequence is approved. **STEP 2 Prayer settings persistence is IN PROGRESS** after explicit authorization; STEP 3 remains NOT STARTED and requires confirmation after STEP 2 closure.",
    "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 1 SPEC CLOSED.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **NOT STARTED / NEXT** → STEP 3 schedule orchestration — NOT STARTED → STEP 4 presentation/countdown — NOT STARTED → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP.":
        "6. **Current: Prayer Engine + Location integration — MILESTONE OPEN / STEP 2 IN PROGRESS.** Seven-step sequence approved: STEP 1 spec-first — **CLOSED** → STEP 2 Prayer settings persistence — **IN PROGRESS** → STEP 3 schedule orchestration — NOT STARTED → STEP 4 presentation/countdown — NOT STARTED → STEP 5 functional Home panel — NOT STARTED → STEP 6 full Prayer+Location+Integration regression — NOT STARTED → STEP 7 docs-only closure — NOT STARTED → STOP.",
}

for old, new in replacements.items():
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one occurrence, found {count}: {old[:120]!r}")
    text = text.replace(old, new, 1)

marker = "## 17. Change log\n\n"
if text.count(marker) != 1:
    raise SystemExit("Change-log marker missing or duplicated")
entry = """### 2026-08-30 — Prayer Engine + Location integration STEP 2 Prayer settings persistence authorized\n\nSTEP 2 is explicitly authorized after STEP 1 approval. Implement `PrayerSettingsRepository` on the **same existing Preferences DataStore instance used by Location**; the current DataStore file/name (`location`) is deliberately left unchanged to avoid a Location migration, while Prayer persistence is isolated by the new keys `prayer.method`, `prayer.asr`, `prayer.high_latitude_rule`, `prayer.offset.fajr`, `prayer.offset.sunrise`, `prayer.offset.dhuhr`, `prayer.offset.asr`, `prayer.offset.maghrib`, and `prayer.offset.isha`. First use must atomically materialize the canonical MWL + STANDARD + AUTOMATIC + zero-offset settings. Any partial set, invalid enum value, or plausible type corruption in an expected Prayer key must recover atomically to that complete canonical default without clearing or rewriting Location keys. Custom settings and positive/negative/zero offsets must round-trip. Verification requires focused JVM tests plus real Preferences DataStore instrumentation on Android 9/API28, including explicit two-way isolation: Prayer operations preserve existing Location entries, and Location operations preserve Prayer entries. No UI, no `PrayerTimeCalculator` changes, no Location behavior/key changes, and no STEP 3 orchestration work are authorized in STEP 2.\n\n"""
text = text.replace(marker, marker + entry, 1)

path.write_text(text, encoding="utf-8")
