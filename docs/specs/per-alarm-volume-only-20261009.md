# SPEC — Per-alarm / per-Adhan volume only

Date: 2026-10-09

Base runtime:
`2182e303afde1e3823af03cbb8d8afabf3541c52`

## User requirement
Arihna must not expose or modify one global alarm-volume control.

Volume must be controlled only at the individual alert level:
- every prayer / Adhan keeps its own independent 0–100% playback volume;
- every personal custom alarm gets its own independent 0–100% playback volume.

## Required behavior
1. Remove the global alarm-volume UI from Settings.
2. Arihna must no longer change Android `STREAM_ALARM` from Settings.
3. Preserve the existing per-prayer / per-Adhan volume controls and persistence.
4. Add a 0–100% volume control to the personal-alarm editor.
5. Persist personal-alarm volume as part of each alarm rule.
6. Existing persisted custom alarms migrate deterministically to 100% volume without losing label, time, recurrence, enabled state or ringtone selection.
7. Changing a personal alarm's ringtone/sound must preserve that alarm's volume.
8. Playback of a personal alarm uses that rule's persisted volume.
9. Prayer-linked playback continues to use the prayer-specific volume repository and must remain independent between prayers.
10. Volume changes must not alter another prayer or another custom alarm.
11. Preserve alarm scheduling, snooze/stop/full-screen/overlay behavior, all Adhan variants, daily inspiration, Quran, Location and Qibla.

## Persistence
- Advance alarm-rule serialization format to a new version carrying `playbackVolumePercent`.
- V1/V2 alarm rows remain readable and default to 100%.
- New writes use the new format.
- Range is clamped/validated to 0–100.

## Validation
- Unit tests cover V1/V2 migration and new-format round trip including volume.
- Android UI test confirms Settings no longer exposes a global alarm-volume control.
- Android UI test confirms personal-alarm editor exposes and saves independent volume.
- Existing per-prayer volume tests remain green.
- Alarm delivery tests verify per-rule playback gain is propagated.
- Exact-SHA gates must pass on API28 and API36 before promotion.
- Publish a Galaxy S25 prerelease after green gates.
- Physical S25 validation must confirm independent volume behavior before stable publication.
