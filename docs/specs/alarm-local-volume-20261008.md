# SPEC — Per-alarm / per-Adhan volume only

Date: 2026-10-08

Base runtime:
`2182e303afde1e3823af03cbb8d8afabf3541c52`

## User requirement
Arihna must not expose or modify one global alarm-volume control.

Volume must be controlled only at the individual alert level:
- each prayer / Adhan keeps its own independent 0–100% playback volume;
- each custom alarm gets its own independent 0–100% playback volume.

## Required behavior
1. Remove the global alarm-volume slider/card from Settings.
2. Arihna must no longer write Android `AudioManager.STREAM_ALARM` from the Settings UI.
3. Preserve existing per-prayer / per-Adhan volume behavior and storage.
4. Add a 0–100% volume control to the custom-alarm editor.
5. Persist custom-alarm volume with the alarm rule.
6. Editing one custom alarm must not change any other alarm or prayer volume.
7. Existing persisted alarm-rule formats must migrate deterministically; rules without a stored custom volume default to 100%.
8. Playback applies the stored custom volume as local MediaPlayer gain only for that alarm instance.
9. Snooze preserves the same per-alarm playback volume.
10. Preserve alarm timing, recurrence, enable/disable, ringtone selection, overlay/fullscreen behavior, notifications, prayer scheduling, Adhan variants, Quran, Location, Qibla and daily inspiration unchanged.

## Persistence
- Advance the alarm-rule codec version to a new version that stores `playbackVolumePercent`.
- Continue decoding legacy V1 and V2 rules.
- Legacy V1/V2 rules migrate with `playbackVolumePercent = 100`.
- Clamp persisted/input volume to the valid 0–100 range.

## UI
- Settings must not show `Volume sveglia` / global phone alarm-volume text or slider.
- Custom alarm editor shows `Volume di questa sveglia` with current percentage and a continuous 0–100 slider.
- Existing prayer sound dialog remains the place to control each prayer/Adhan volume independently.

## Validation
- Unit tests cover V1/V2 migration and V3 round-trip of independent custom volumes.
- Android UI tests confirm the global Settings control is absent.
- Android UI tests confirm the custom-alarm editor exposes and saves its own volume.
- Existing prayer-volume tests remain green.
- Exact-SHA gates must pass on API28 and API36 before promotion.
- Publish a Galaxy S25 prerelease after green gates.
- Physical S25 validation should verify that changing one custom alarm or one prayer volume does not change another alert.
