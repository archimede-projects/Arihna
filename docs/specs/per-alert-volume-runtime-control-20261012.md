# SPEC — Per-alert runtime volume control — 2026-10-12

Base runtime: `c01414f82d257bcd2fa6c0a6b456d49c75f00b68`

## Problem
Physical Galaxy S25 validation showed that the per-alarm slider is persisted and the legacy global Settings control is gone, but audible playback still follows Android's alarm stream level. The current implementation only calls `MediaPlayer.setVolume()` while using `AudioAttributes.USAGE_ALARM`, which is not sufficient on the validated Samsung device.

## Required behavior
1. Keep Settings free of any global alarm-volume control.
2. Keep independent persisted 0–100% values for every personal alarm.
3. Keep independent persisted 0–100% values for every prayer/Adhan.
4. When a personal alarm or prayer/Adhan starts, apply that alert's requested percentage to Android's alarm stream for the active ringing session.
5. Use full MediaPlayer gain when the requested non-zero stream level was applied successfully; 0% must remain silent.
6. When ringing stops, snoozes, is replaced by another alarm, playback fails, or the service is destroyed, restore the pre-ring Android alarm-stream volume.
7. Restoration must be non-destructive: restore only if the current Android alarm-stream index still equals the index Arihna applied. If the user or another actor changed it while ringing, keep that newer value.
8. If Android reports fixed volume, rejects the stream change, or throws, fall back to the existing software gain instead of crashing.
9. Do not change persistence format, scheduling semantics, alarm notification/full-screen/overlay behavior, ringtone selection, recurrence, stop/snooze, Adhan variants, Quran, Qibla, Location, or Daily Inspiration.
10. Preserve Makkah Takbir behavior: exactly the bundled pair once, then stop.

## Validation
- Unit-test percentage→stream-index mapping, software-gain fallback, successful temporary application, safe restore, user-change preservation, fixed-volume and failure fallback.
- Build/unit/AndroidTest compile.
- Exact-SHA API28 full Android suite.
- Exact-SHA API36 targeted alarms/settings/Quran + permission matrix.
- New Galaxy S25 prerelease; physical PASS required before stable publication.
