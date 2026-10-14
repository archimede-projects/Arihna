# SPEC — Adhan auto-dismiss on natural completion — 2026-10-14

Base runtime: `f251a9b630781252d028055d42fa2ce7f1ab3920`

## Physical finding
Galaxy S25 validation confirms the per-alert runtime volume fix now works. A separate UI lifecycle defect was observed: when an Adhan audio file finishes naturally, the Arihna ringing UI remains visible until the user presses "Interrompi".

## Root cause
The Adhan `MediaPlayer.OnCompletionListener` releases the player and restores volume but does not end the ringing session. Therefore the foreground notification, overlay and/or fullscreen ringing activity can remain active.

## Required behavior
1. When an Adhan reaches its final natural audio completion, end the complete ringing session automatically.
2. Automatic session end must:
   - hide the in-app overlay;
   - remove the foreground/ringing notification;
   - stop the foreground service;
   - restore any temporary Android alarm-stream volume lease;
   - dismiss the Arihna fullscreen ringing activity for the same alarm occurrence if it is open.
3. Normal personal alarms using a looping system ringtone must NOT auto-dismiss on audio playback; they remain active until Stop/Snooze or the existing safety timeout.
4. Silent alarms keep existing Stop/Snooze behavior.
5. Stop and Snooze remain immediately functional for Adhan and normal alarms.
6. Dismissal signals must be scoped to the active alarm ID so an old completion cannot close a newer ringing activity.
7. Preserve all current volume behavior, prayer variants, Makkah two-Takbir exactly once, scheduling, notifications, overlay permissions, Quran, Qibla, Location and Daily Inspiration.

## Validation
- Unit test that only ADHAN is finite/auto-ending.
- Unit test alarm-ID scoped fullscreen-dismiss matching.
- Android test that an explicit matching session-ended signal closes AlarmRingingActivity while a mismatched alarm ID does not.
- Static/build + AndroidTest compile.
- Exact-SHA API28 full suite.
- Exact-SHA API36 alarms/settings/Quran/permission matrix.
- Galaxy S25 prerelease and physical validation before stable publication.
