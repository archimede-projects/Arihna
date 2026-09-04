from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")

old = """### 5.5 Alarms

- Custom alarms in addition to prayer-linked alarms.
- Configurable sounds.
- Prayer/custom alarm reliability must respect Android standby/doze restrictions.
"""

new = """### 5.5 Alarms — MILESTONE OPEN / STEP 1 SPEC-FIRST CLOSED

The Alarms milestone follows the closed Qibla milestone and must not reopen Prayer calculation, Location acquisition/cache, Qibla mathematics/sensors, or existing persistence policy. STEP 1 is documentation/architecture only: it authorizes no manifest, dependency, Kotlin, UI, receiver, notification-channel or runtime behavior change. The first technical candidate must be a direct child of this spec commit.

The milestone provides two user-owned alarm families:

- **Prayer-linked alarms:** each rule targets one of the five obligatory prayers already produced by the authoritative `PrayerScheduleRepository` (`Fajr`, `Dhuhr`, `Asr`, `Maghrib`, `Isha`). Sunrise remains schedule information and is not a prayer-alarm target in this milestone. A prayer rule may carry a signed minute offset; `0` means the exact calculated prayer time. The concrete UI bounds for that offset remain **Pending** until the functional UI step, but the domain/scheduler must support crossing civil-day boundaries correctly.
- **Custom alarms:** independent personal alarms defined in the device's local civil time, with user label, local time and optional weekday recurrence. No selected prayer/location city is allowed to redefine a custom alarm's wall clock. An empty recurrence set means one-shot at the next valid device-local occurrence and auto-disables after delivery; a non-empty set repeats only on the selected weekdays.

`enabled` records the user's intent and is distinct from whether Android currently allows the alarm to be scheduled. Arihna must never label an alarm as scheduled/reliable merely because its rule is enabled.

#### Alarm domain and ownership boundaries

Use Arihna-owned boundaries equivalent to:

```text
AlarmRuleRepository
- observeRules(): Flow<List<AlarmRule>>
- create/update/delete/setEnabled(...)

AlarmScheduleCoordinator
- reconcile(reason)
- observes alarm rules, PrayerScheduleState and platform capability state while the app is active

AlarmPlatformScheduler
- scheduleExact(ResolvedAlarmOccurrence)
- cancel(alarmId / occurrence token)
- capability(): AlarmCapabilityState

AlarmRule
- PrayerLinked(...)
- Custom(...)

ResolvedAlarmOccurrence
- alarmId
- ruleRevision
- triggerAt Instant
- display local date/time + ZoneId provenance
- occurrence token

AlarmCapabilityState
- Ready
- NeedsNotificationPermission
- NeedsExactAlarmAccess
- PrayerScheduleUnavailable(reason)
- PlatformUnavailable(reason)
```

Exact class names may change if these ownership rules remain intact. `PrayerScheduleRepository` remains the sole source of calculated prayer times; the alarm layer must not call Adhan directly, duplicate prayer formulas, reinterpret Location acceptance/freshness, or invent coordinates/timezones. `AlarmRuleRepository` owns user alarm rules only. `AlarmPlatformScheduler` owns `AlarmManager`/`PendingIntent` details only. Compose must not call `AlarmManager` directly.

Every persisted rule has a stable app-owned `alarmId` plus monotonically changing rule revision/generation. Every scheduled OS occurrence carries an occurrence token derived from the current rule revision and resolved trigger. A receiver must validate that token against current persisted state before user-visible delivery, so a stale `PendingIntent` left behind by a reschedule can be ignored rather than delivering a duplicate/obsolete alarm.

#### Persistence decision

Reuse the already-approved Preferences DataStore dependency; do not add Room, a second DataStore instance, a serialization library or another persistence dependency for STEP 2 without a new spec decision.

- Alarm keys/records are isolated under an `alarm.*` namespace and must not clear or rewrite Location or Prayer settings.
- Persist the **rule**, enabled state, stable id/revision and sound-profile reference; do not persist a resolved epoch timestamp as the authoritative schedule because timezone, prayer settings and selected location can make it stale.
- Collection encoding must be deterministic, versioned, lossless and migration-testable. The exact internal codec may be selected in STEP 2 if it requires no new dependency and does not leak into the domain API.
- Deleting a rule must also cancel any currently resolved OS occurrence for that id.

#### Exact-alarm platform policy

Alarm delivery is user-facing and time-sensitive, so the approved platform is Android framework `AlarmManager`; WorkManager is not the trigger mechanism for prayer/custom alarm delivery.

- On API 23+ use a one-occurrence `RTC_WAKEUP` exact alarm compatible with Doze (`setExactAndAllowWhileIdle` or the tested equivalent). Do **not** use repeating `AlarmManager` APIs as the source of recurrence; always resolve and schedule the next single occurrence, then reconcile again after delivery/change. This avoids interval drift and allows DST, timezone, prayer-setting and location changes to take effect.
- On API 31+ exact scheduling must be guarded by `AlarmManager.canScheduleExactAlarms()`.
- Arihna will declare/request **`SCHEDULE_EXACT_ALARM`**, not `USE_EXACT_ALARM`. `SCHEDULE_EXACT_ALARM` is explicit user-controlled Special App Access and fits Arihna's opt-in alarm feature; Arihna must not claim the narrower alarm-clock/calendar entitlement implied by `USE_EXACT_ALARM` merely to bypass user approval.
- Do not request the special access at app startup. When the user explicitly enables the first alarm (or taps a capability CTA), explain why exact timing is needed and then open the system `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` flow where supported.
- If exact-alarm access is absent/revoked, retain the user's configured rule but do not call an exact API and do not show it as scheduled. Surface `NeedsExactAlarmAccess` with a recovery CTA. No silent inexact fallback is approved in STEP 1 because a late alarm must not be presented as exact/reliable.
- Handle `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` by rechecking `canScheduleExactAlarms()` and reconciling current rules. Do not assume there is a symmetric reliable revoke broadcast; foreground/resume reconciliation must re-check capability because revocation cancels scheduled exact alarms.
- API 28 remains supported and uses the exact path without Android 12+ Special App Access.
- No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, no broad battery-whitelist prompt, no wake lock, no background polling loop and no always-on foreground service are approved by STEP 1.

Current Android platform evidence reviewed for this decision (2026-09-04): Android Developers `Schedule alarms` documents `SCHEDULE_EXACT_ALARM`, `canScheduleExactAlarms()`, the permission-state-change broadcast and exact-alarm behavior; Android 14 behavior changes document that `SCHEDULE_EXACT_ALARM` is denied by default for most fresh installs targeting Android 13+.

#### Notification permission and delivery contract

Alarm delivery in this milestone is a user-visible notification/alarm alert, not silent background work.

- On API 33+ declare `POST_NOTIFICATIONS`, but request it only after an explicit user action to enable alarm delivery and after a concise rationale. Never request it automatically on first launch.
- If notification permission is denied, preserve the rule but do not represent it as deliverable/scheduled; expose `NeedsNotificationPermission`. Arihna must not schedule invisible background alarm firings that the user cannot receive as an alarm notification.
- At least two semantic notification families are required: prayer alarms and custom alarms. Channel ids must be stable and migration/versioning safe.
- Configurable sound is represented by an Arihna-owned `AlarmSoundProfile` reference whose Android delivery maps to immutable notification-channel sound configuration. The concrete profile catalog/UI is **Pending** until STEP 5, but it must support at least system-default alarm sound and silent. Bundled adhan playback, long-form audio, media playback services and imported user audio are explicitly outside this milestone unless separately specified.
- No full-screen intent, lock-screen takeover or alarm-clock UI is approved in STEP 1. A later requirement for those behaviors needs a separate spec decision and Android-policy review.
- Notification text may identify the prayer/custom label and scheduled time but must not fabricate religious quotations or authoritative content.

Android Developers' notification-permission guidance reviewed for STEP 1 confirms `POST_NOTIFICATIONS` is a runtime permission on API 33+ and user denial prevents ordinary app notifications.

#### Prayer-linked scheduling semantics

Prayer-linked alarms consume the existing `PrayerScheduleRepository` output exactly.

- Use the selected location and `ZoneId` already embedded/provenanced by the authoritative Prayer schedule. Do not call `ZoneId.systemDefault()` to reinterpret a Manual/remote prayer city.
- A `Ready(CACHED)` Device location remains valid exactly as Prayer Schedule already defines it; the alarm layer must not trigger a fresh location request merely to schedule alarms.
- If Prayer Schedule is non-Ready/unavailable, keep prayer rules persisted but unscheduled and surface the controlled reason. Never substitute a default city, stale UI city, device timezone or fabricated prayer time.
- Applying a signed prayer offset may cross midnight; resolve the resulting instant in the same selected prayer `ZoneId` while preserving the schedule's source date/time semantics.
- Any accepted Prayer Schedule change caused by selected Location, Prayer settings, local date or timezone invalidates outstanding prayer occurrences and triggers idempotent reconciliation. Stale occurrences must fail the occurrence-token validation at delivery.
- Schedule only future occurrences. If the resolved time for today's target has passed, choose the next authoritative occurrence from the existing today/tomorrow schedule semantics; do not invent a multi-day prayer calculator inside the alarm layer.

#### Custom-alarm time semantics

Custom alarms intentionally follow the **device** civil timezone because they are personal wall-clock alarms and are independent of the selected prayer city.

- Recompute custom next-occurrence time after device timezone or wall-clock changes.
- For a DST spring-forward gap, resolve a nonexistent requested local time to the first valid instant after the gap and retain the user's displayed wall-clock rule unchanged for future recurrences.
- For a DST fall-back overlap, fire once using the earlier valid offset unless a later product decision explicitly chooses otherwise; never fire the same recurrence twice solely because the local clock repeats.
- One-shot custom alarms auto-disable only after a validated successful delivery occurrence; stale/ignored occurrences must not disable the rule.

#### Reconciliation and reboot/time-change events

OS alarms are ephemeral scheduling state and must be reconstructed from persisted rules.

Reconcile on at least:

1. app/process bootstrap or foreground resume;
2. rule create/update/delete/enable/disable;
3. authoritative Prayer Schedule change;
4. validated alarm delivery, to schedule the next recurrence;
5. `BOOT_COMPLETED` after device reboot;
6. system wall-clock/timezone change broadcasts relevant to the current Android API;
7. exact-alarm special-access grant/change after rechecking capability;
8. notification-permission recovery when the app returns to foreground.

A later technical step may add only the minimal manifest receivers/normal permissions required for these events (`RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`) and must keep receivers explicit/non-exported unless Android requires otherwise for a system broadcast. No receiver may perform long-running arbitrary work; use `goAsync`/bounded coroutine work for the small offline persistence/schedule reconciliation path, and introduce WorkManager only if a separately demonstrated deferrable need exists.

#### Samsung / standby reliability policy

`setExactAndAllowWhileIdle` is selected because prayer/custom alarms are user-intentioned time-sensitive events and must retain exact behavior through Doze when Android permits it. OEM behavior still requires physical verification on the primary Samsung Galaxy S25.

- Do not ask the user to disable battery optimization globally as a default requirement.
- Surface an honest troubleshooting note for Samsung sleeping/deep-sleep restrictions only if physical testing demonstrates a relevant limitation.
- STEP 7 hardware validation must include at least screen-off delivery, a short idle/Doze-oriented case feasible on-device, reboot/reschedule, permission denial/recovery and one prayer-linked plus one custom occurrence. Long-duration overnight evidence may be added where practical, but no pass may be inferred from emulator behavior alone.

#### STEP 1 test contract for later implementation

Pure/JVM coverage must eventually prove at least:

- custom one-shot next occurrence and auto-disable after validated delivery;
- daily/weekday recurrence ordering;
- device-zone change recomputation;
- DST gap and overlap rules above;
- prayer rule consumes exact existing schedule output and selected `ZoneId` without recalculating Prayer or Location;
- prayer offset crossing midnight;
- non-Ready Prayer Schedule produces configured-but-unscheduled state with no fabricated time;
- rule update/cancel/reconcile is idempotent;
- stale occurrence token cannot deliver or disable a newer rule;
- capability matrix for notification denied / exact access denied / ready;
- no silent inexact fallback when exact access is absent.

Android instrumentation must eventually prove at least:

- API28 exact scheduling/cancel `PendingIntent` identity path works without Special App Access and does not crash;
- notification channels/receiver path works on API28;
- boot/time-change receivers invoke reconciliation without duplicate delivery;
- a modern Android API (prefer API36 to match the Galaxy S25 runtime when available in CI) exercises `canScheduleExactAlarms()` denied/granted handling and API33+ notification-permission state;
- no ignored/skipped Alarm tests at the definitive gate.

Every technical Alarm step must preserve unfiltered existing regression: `testDebugUnitTest`, `assembleDebug`, Android 9/API28 `connectedDebugAndroidTest` with zero skipped tests, frozen GeoNames SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`, `applicationId = com.archimedeprojects.arihna`, and established permission/dependency policy outside the explicitly authorized Alarm permissions. A technical candidate is valid only after a full gate on its exact SHA and promotion to `main` by non-forced fast-forward.

#### Alarms milestone sequence

1. **STEP 1 — spec-first architecture/policy: CLOSED by this documentation step.** No runtime change.
2. **STEP 2 — domain + persistence: NOT STARTED.** Implement Alarm domain, deterministic rule persistence and pure occurrence resolution only; no `AlarmManager`, manifest permission/receiver or UI yet.
3. **STEP 3 — Android scheduler + capability layer: NOT STARTED.** Add exact-alarm platform adapter, permission/capability handling, receivers and focused API coverage; no Prayer integration/UI yet.
4. **STEP 4 — Prayer-linked reconciliation: NOT STARTED.** Connect the scheduler to the already-closed `PrayerScheduleRepository` without changing Prayer or Location behavior.
5. **STEP 5 — functional alarm UI + notification/sound profiles: NOT STARTED.** Minimal usable Prayer/custom alarm controls, rationale/capability UI and approved channel-backed sound profiles.
6. **STEP 6 — definitive regression/package gate: NOT STARTED.** Full exact-SHA regression including API28, focused modern-API exact-alarm coverage, identity/signing checks and persistent-debug Galaxy S25 prerelease.
7. **STEP 7 — Galaxy S25 reliability validation + documentation-only closure: NOT STARTED.** Hardware validation first; closure commit after explicit physical PASS, with no runtime change.

STEP 2 is not authorized by STEP 1 closure. Stop after the STEP 1 documentation commit until the next objective is explicitly approved.
"""

count = text.count(old)
if count != 1:
    raise SystemExit(f"expected exactly one Alarms placeholder block, found {count}")

updated = text.replace(old, new)
path.write_text(updated, encoding="utf-8")
