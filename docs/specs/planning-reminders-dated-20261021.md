# SPEC — Arihna Planning: attività, promemoria, sveglie con data precisa — 2026-10-21

Base: physically validated stable runtime `adc40017694af03723c7c57afb996306c0ddcd39`, tag `arihna-stable-adc40017-20261017`. This branch contains **requirements only**, no runtime code.

## Product decision and motivation
On 2026-10-21 the user explicitly approved two capabilities: a reminder notification and a full ringing alarm for a specified future calendar date (example: 23 December). Subsequently the user suggested grouping personal tasks and reminders into a dedicated Planning screen. Use **Planning** as a coherent, accessible home for date-based personal organization rather than overloading the prayer/Adhan UI or adding a seventh permanent bottom-navigation icon.

The current Personal Alarms editor supports a LocalTime and optional weekdays, not a calendar date. Existing `Singola` means the next occurrence today/tomorrow, **not** December 23; never present it as a future-dated alarm.

## User-facing design
1. Add a distinct **Planning** screen, reachable via a clear shortcut/card on Home. Preserve current six bottom navigation destinations and all their existing behavior.
2. Planning contains **Da fare**, **Promemoria**, and **Sveglie con data** in a unified list grouped **Oggi / Prossimi / Completati** (or an equivalent legible presentation). Each row identifies its type, title, scheduled date/time where applicable and completion/status.
3. **Da fare:** create/edit/delete/complete and uncomplete a local text task with optional due date (no automatic ringing). Keep it lightweight; no cloud account or external calendar integration.
4. **Promemoria:** create/edit/delete a title/message with **explicit calendar date and clock time** (day, month, year). At that instant, deliver a discreet Android notification containing the message, not full-screen and not the alarm ringtone. User can mark it completed; prevent duplicate delivery. If permission is missing, visibly disclose inability to notify.
5. **Sveglia con data:** create/edit/delete a label, explicit future LocalDate + LocalTime, chosen supported ringtone/profile, independent volume percent 0..100. At occurrence, deliver via existing alarm UI with **Interrompi** and **Rinvia 5 min**; use existing approved audio/ringing implementation, do not fork it. This is a one-time event; it must not ring on earlier matching weekdays or repeat next year. Snooze remains possible after first delivery; preserve one-shot revision rules.
6. Existing **Sveglie** screen remains the place for existing single-next-occurrence, recurring weekday alarms, and prayer/Adhan alerts. Planning-date entries may be managed in Planning without changing how the existing screen lists or edits legacy alarms.
7. Date/time picker must clearly show selected day/month/year and time. Present a saved-item confirmation with its **actual scheduled date and time**. Past date/time blocked with actionable message; no silent fallback to tomorrow or next week.
8. User-visible text in Italian (and Arabic localization patterns where available), RTL/layout/large font support, Arihna's approved emerald/cream/gold palette. Avoid icon-library size regression.
9. Planning data remains available **fully offline**, persists across process death/restart and app updates, and is not silently replaced by phone/cloud calendar events.

## Domain / time / lifecycle requirements
10. Model three explicit distinct item types with stable IDs and versions; persist actual **LocalDate and LocalTime** (plus type and all options) for dated reminders and alarms. Keep planner data separate from existing alarm-rule V1/V2/V3 migration semantics. Any addition of an `AlarmDefinition.Dated` subtype must preserve old codecs/UI and migrate no legacy rule to an arbitrary date.
11. Interpret a date-time as the **device's local civil date and time**. On timezone change, preserve requested wall-clock date/time and recompute the next trigger. For nonexistent DST local times, resolve to first valid instant after gap; for ambiguous local times use a consistent deterministic first occurrence. Document behavior; test examples.
12. Dated scheduling must support months ahead (23 December from October) and persist across reboot, clock/date or timezone change, application update and notification/exact alarm permission transition where Android allows. Do **not** schedule only through a UI-scope timer.
13. Reconcile idempotently; revision/token identity prevents duplicate/stale delivery. Mark one-off items delivered/completed according to actual outcome, **not just attempted delivery**. If Android delivery is blocked, keep explicit NeedsAction/failed status without claiming success.
14. Reuse Arihna's existing Android `AlarmManager`, `SCHEDULE_EXACT_ALARM`, notification permission/channel and reconciliation architecture where possible. A quiet reminder uses a dedicated notification channel and does not start the full ringing service or change `STREAM_ALARM`. Dated ringing alarm uses the proven foreground playback/fullscreen/STOP/SNOOZE path. Battery/system force-stop limits must be stated honestly.
15. Preserve supported time-zone/polar conditions and prayer correctness; do not assign default city or coordinates for personal Planning tasks.
16. Keep notification content private as feasible (consider lockscreen presentation), avoid pretending reminder is a prayer notification or treating task completion as a religious action.

## Explicit non-regressions
- Preserve approved Makkah two-Takbir, 4-second classic/vintage ringtone, independent alarm and prayer volume, Adhan natural end auto-dismiss, full ringing custom alarms until Stop/Snooze, Asr methods, corrections, location, qibla, GeoNames, complete offline Hafs+Warsh pages, Imla'i and Tajwid.
- Do not remove/alter the prior physically validated stable APK or change app package name/signing.
- No unnecessary increase of APK size; retain prior icon optimization.

## Test plan and release
- JVM tests: dated resolution incl. 2026-12-23 months-ahead, past-date refusal, year boundary, DST gap/overlap, timezone reschedule, persistence round-trip, edits/disable/delete, stale tokens and idempotent reconcile, repeated reminders disallowed in first iteration, existing weekly/next-occurrence semantics unchanged.
- Android tests: Planning navigation accessible from Home, task CRUD + completed view, dated reminder picker and quiet notification channel/no ringing, dated alarm uses existing ringtone/volume + Stop/Snooze, permission denied/regranted, reboot/application update event handling and no duplicated alerts, preservation of legacy Alarms/Adhan.
- Candidate must be exactly one commit direct child of this SPEC; superseding candidates sibling from this SPEC. Full exact-SHA static/unit/APK and Android API28 full + API36 matrix gates before main non-force fast-forward.
- Publish Galaxy S25 validation prerelease using persistent signer; redownload verify exact APK bytes/hash/certificate. Require explicit real Galaxy S25 PASS before stable. Stable must copy **exact same prerelease APK bytes without rebuild**, previous stable preserved.
