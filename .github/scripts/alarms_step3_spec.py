from pathlib import Path

path = Path('PROJECT_SPEC.md')
text = path.read_text()
marker = '#### Alarms milestone sequence\n'
insert = '''#### STEP 2 closure / STEP 3 authorization — APPROVED 2026-09-04

Alarms STEP 2 domain + persistence is **CLOSED** on exact technical commit `7579490c58e354a5423e76d4d8ad4e1f7749dfac`, built directly above STEP 1 spec commit `4fdbc9b68e1e6637133b6e68ef73a194b78aa92a`. Definitive full gate run `33888892314` / job `101075412204` checked out that exact SHA, restored and verified the frozen GeoNames asset, passed unfiltered `testDebugUnitTest`, passed `assembleDebug`, and completed Android 9/API28 `connectedDebugAndroidTest` with **53/53 tests, 0 failed and 0 skipped**. The implementation added only Arihna-owned Alarm domain/persistence/pure resolver code, shared-DataStore wiring and focused tests. No `AlarmManager`, manifest alarm permission/receiver, Prayer integration, notification delivery or Compose UI was introduced.

STEP 3 Android scheduler + capability is now authorized with this exact scope:

- Introduce `AlarmPlatformScheduler`/equivalent as the only owner of Android `AlarmManager` and `PendingIntent` mechanics. Production scheduling is a single future `RTC_WAKEUP` occurrence using `setExactAndAllowWhileIdle` on API 23+; no repeating API and no inexact fallback.
- On API 31+ the scheduler must check `canScheduleExactAlarms()` before any exact call and return an explicit `NeedsExactAlarmAccess`/equivalent result when denied. API28 remains `Ready` without Special App Access. Provide an Arihna-owned capability reader and a settings-intent factory for `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; STEP 3 must not launch that settings flow automatically because there is still no alarm UI.
- Manifest changes are limited to `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED` plus the minimal explicit receiver declarations needed by this step. `USE_EXACT_ALARM`, `POST_NOTIFICATIONS`, battery-optimization exemptions, wake locks, foreground services and new dependencies remain forbidden in STEP 3.
- A scheduled occurrence uses an explicit `AlarmOccurrenceReceiver` `PendingIntent` whose identity is stable per `alarmId`, while extras carry `alarmId`, rule revision, trigger epoch and occurrence token. Cancel uses the same stable identity. Stale extras must never change the identity of another alarm.
- Add a minimal `AlarmSystemEventReceiver`/equivalent for `BOOT_COMPLETED`, device wall-clock/timezone changes, `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, and app replacement where supported. Receiver work must be bounded via `goAsync`/coroutine delegation. STEP 3 may expose the reconciliation trigger contract, but actual rule/Prayer reconciliation is STEP 4 and must not be silently implemented here.
- Occurrence envelope decoding/validation may be implemented and unit-tested in STEP 3, but there is **no user-visible delivery** yet: no notification channel, sound, full-screen UI or notification permission. No production code may schedule an alarm merely at app startup in this step; scheduling is exercised only through the platform boundary/tests until STEP 4/5 provide real callers.
- Preserve `applicationId = com.archimedeprojects.arihna`, existing COARSE-only Location policy, Prayer/Qibla behavior and all existing persistence namespaces. No change to Prayer calculation, Prayer schedule, Location/cache, Qibla, or their UI.

Required STEP 3 tests: pure/JVM fake-gateway coverage proving API28-ready/API31+-denied capability behavior, exact-call-only when allowed, zero schedule call when exact access is denied, no inexact fallback, deterministic `PendingIntent` identity/envelope and cancel symmetry; Android 9/API28 instrumentation proving manifest/component availability and the exact scheduling/cancel path does not crash without Special App Access. Modern-API denied/granted emulator coverage remains mandatory at the definitive STEP 6 gate, with focused unit/API policy coverage added now.

STEP 4 is not started by this authorization. STEP 3 must pass the full exact-SHA regression gate before non-forced promotion to `main`.

'''
if marker not in text:
    raise SystemExit('milestone marker missing')
text = text.replace(marker, insert + marker, 1)
old = '''1. **STEP 1 — spec-first architecture/policy: CLOSED by this documentation step.** No runtime change.
2. **STEP 2 — domain + persistence: NOT STARTED.** Implement Alarm domain, deterministic rule persistence and pure occurrence resolution only; no `AlarmManager`, manifest permission/receiver or UI yet.
3. **STEP 3 — Android scheduler + capability layer: NOT STARTED.** Add exact-alarm platform adapter, permission/capability handling, receivers and focused API coverage; no Prayer integration/UI yet.
4. **STEP 4 — Prayer-linked reconciliation: NOT STARTED.** Connect the scheduler to the already-closed `PrayerScheduleRepository` without changing Prayer or Location behavior.
5. **STEP 5 — functional alarm UI + notification/sound profiles: NOT STARTED.** Minimal usable Prayer/custom alarm controls, rationale/capability UI and approved channel-backed sound profiles.
6. **STEP 6 — definitive regression/package gate: NOT STARTED.** Full exact-SHA regression including API28, focused modern-API exact-alarm coverage, identity/signing checks and persistent-debug Galaxy S25 prerelease.
7. **STEP 7 — Galaxy S25 reliability validation + documentation-only closure: NOT STARTED.** Hardware validation first; closure commit after explicit physical PASS, with no runtime change.

STEP 2 is not authorized by STEP 1 closure. Stop after the STEP 1 documentation commit until the next objective is explicitly approved.
'''
new = '''1. **STEP 1 — spec-first architecture/policy: CLOSED.** No runtime change.
2. **STEP 2 — domain + persistence: CLOSED.** Exact runtime `7579490c58e354a5423e76d4d8ad4e1f7749dfac`; gate `33888892314` / `101075412204`, API28 53/53, 0 failed, 0 skipped.
3. **STEP 3 — Android scheduler + capability layer: AUTHORIZED / IN PROGRESS.** Exact-alarm platform adapter, capability handling, minimal receivers and focused API coverage only; no Prayer integration/UI/notifications.
4. **STEP 4 — Prayer-linked reconciliation: NOT STARTED.** Connect the scheduler to the already-closed `PrayerScheduleRepository` without changing Prayer or Location behavior.
5. **STEP 5 — functional alarm UI + notification/sound profiles: NOT STARTED.** Minimal usable Prayer/custom alarm controls, rationale/capability UI and approved channel-backed sound profiles.
6. **STEP 6 — definitive regression/package gate: NOT STARTED.** Full exact-SHA regression including API28, focused modern-API exact-alarm coverage, identity/signing checks and persistent-debug Galaxy S25 prerelease.
7. **STEP 7 — Galaxy S25 reliability validation + documentation-only closure: NOT STARTED.** Hardware validation first; closure commit after explicit physical PASS, with no runtime change.
'''
if old not in text:
    raise SystemExit('sequence block missing')
text = text.replace(old, new, 1)
path.write_text(text)
