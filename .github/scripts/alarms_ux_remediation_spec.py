from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")
marker = "### Alarms STEP 7 — Galaxy S25 UX/full-screen remediation approved 2026-09-05"
if marker in text:
    raise SystemExit("marker already present")

appendix = r'''

### Alarms STEP 7 — Galaxy S25 UX/full-screen remediation approved 2026-09-05

Physical validation of corrective runtime `3c0e73bb82a4a28806039dc4189ddc07090bcfd1` on the primary Samsung Galaxy S25 remains **FAIL / milestone not closed**. Audio delivery and the ringing Activity are demonstrably reachable, but the Activity did not open automatically from the locked-screen delivery path; the user had to open the notification first. The same validation also exposed approved user-centred UX corrections. This section supersedes only the relevant Alarms presentation/interaction details; all existing Alarm domain, stale-token, exact scheduling, PrayerSchedule, Location, Qibla, GeoNames, signing and gate contracts remain unchanged unless explicitly listed below.

#### Authorized remediation scope — exhaustive

No product behavior outside this list is authorized by this round.

1. **Personal alarm time entry**
   - Remove manual `HH:mm` text entry as the normal creation/editing interaction.
   - Creating or editing a personal alarm must use an Android/Material-style clock time picker in 24-hour device-local civil time.
   - The picker must return a validated local hour/minute to the existing custom-alarm domain; no new interpretation of timezone/DST policy is introduced.

2. **Personal alarm editing**
   - Existing custom alarms must be editable without delete/recreate.
   - Editing preserves the stable `alarmId`, increments the existing rule revision/generation through the repository update path, invalidates the previous occurrence token, cancels/reconciles the old OS occurrence and schedules the revised future occurrence exactly once.
   - Editable values in this round are the already-owned custom-alarm fields: local time, label, weekday recurrence, sound profile and enabled state. No new recurrence model is authorized.

3. **Sound selection / phone ringtone**
   - The user-facing sound chooser must expose exactly the existing approved semantic choices relevant to the alarm context: `Adhan`, `Suoneria telefono`, `Silenzioso`.
   - `Suoneria telefono` must be genuinely selectable through the Android alarm-ringtone picker (`RingtoneManager.ACTION_RINGTONE_PICKER` / `TYPE_ALARM` or tested equivalent platform contract), rather than being a non-interactive generic label.
   - Persist the chosen platform ringtone URI/name with an Arihna-owned representation using the existing Preferences DataStore; no new database, serializer or dependency.
   - Playback must use the selected valid alarm ringtone URI when available. If the persisted platform ringtone is no longer readable/available, fall back transparently to the current Android default alarm ringtone; never invent or bundle an unrelated tone.
   - `Adhan` continues to use the already-approved bundled offline Adhan artifact; `Silenzioso` continues to produce notification/full-screen presentation without audio.

4. **Surface ownership / information architecture**
   - `Sveglie` contains **personal/custom alarms only**. Remove the five prayer reminder rows and readiness/permission panel from the Alarms surface.
   - Prayer-linked reminder configuration moves to the already-existing `Orari` / Prayer Schedule surface, adjacent to the five obligatory prayer rows already shown there. This is presentation/reconfiguration ownership only: calculation, selected location, `PrayerScheduleRepository`, prayer identifiers, offsets and scheduler semantics must not change.
   - Tapping a prayer reminder control may configure only its existing enable/sound choice in this round; no new prayer calculation or religious content is authorized.
   - Notification permission, exact-alarm access and full-screen-intent capability/status belong under `Impostazioni`, not in the operational Alarms list.

5. **Settings capability management and physical test actions**
   - Add an Alarms/notifications settings group exposing actual current state for: notification permission, exact-alarm access and full-screen-intent access.
   - On modern Android, full-screen status must be based on the real platform capability (`NotificationManager.canUseFullScreenIntent()` where available). Do not display `Pronto/Consentito` merely because the manifest permission exists.
   - When full-screen access is unavailable and Android exposes the management page, provide the explicit settings path using `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` (or the appropriate supported equivalent). No unrelated system settings navigation.
   - Add exactly two real-device diagnostic actions in Settings:
     - `Test sveglia (1 minuto)`
     - `Test Adhan (1 minuto)`
   - Each test must schedule a real Arihna alarm occurrence for approximately one minute in the future through the same production scheduler/delivery/ringing path, not merely play an audio preview. The first uses the configured/default phone alarm sound; the second uses the bundled Adhan.
   - A pending diagnostic test may be cancelled before firing. Diagnostic test occurrences must be isolated from persisted user alarm rules and prayer reminder rules; they must not create, mutate, delete, disable or advance a user-owned alarm rule.

6. **Automatic full-screen ringing correction**
   - A validated alarm occurrence with the required notification/exact/full-screen capabilities must present Arihna's ringing Activity automatically from the lock-screen/screen-off delivery path; the user must not have to tap the notification to reach it.
   - Preserve category `ALARM`, the dedicated high-importance ringing notification/channel contract, and a `fullScreenIntent` wired to the ringing Activity. The ringing Activity must continue to use the supported `setShowWhenLocked` / `setTurnScreenOn` behavior (or tested equivalents) and keep a clear `Stop` control.
   - The runtime must not claim automatic full-screen readiness when platform access is absent. In that state, retain controlled notification/audio delivery where Android permits it and surface the missing capability in Settings.
   - Do not add a full-screen bypass, accessibility abuse, overlay permission, device-admin behavior, battery-optimization exemption, always-on foreground service, or other mechanism outside Android's supported alarm/full-screen APIs.

#### Approved UX direction

The approved visual direction is the reviewed Arihna emerald/gold concept: personal-alarm list with a prominent `Nuova sveglia` action and explicit `Modifica`; clock-based create/edit flow; explicit sound sheet; prayer reminders integrated into `Orari`; capabilities and one-minute test actions in `Impostazioni`; full-screen ringing remains Arihna emerald/gold with a dominant `Stop` action. Implementation must remain practical on the Galaxy S25 and may use existing Material/Compose components rather than reproducing decorative mockup pixels literally.

The mockups are interaction/layout references, not authorization for new product features. In particular, decorative text, motivational quotes, snooze, new alarm categories, new religious content, new settings, new sounds or extra navigation destinations are **not authorized** unless already present in the runtime before this section. `Posticipa 5 min` shown in a concept image is not authorized by this round and must not be implemented unless separately approved.

#### Regression and replacement-APK gate

Before a replacement Galaxy S25 APK may be published, the exact technical candidate must have this spec commit as direct parent and pass:

1. unfiltered `testDebugUnitTest`;
2. `assembleDebug`;
3. full Android 9/API28 `connectedDebugAndroidTest` with zero failed/errors/skipped, preserving all prior suites;
4. focused modern-API coverage (API36 where available in CI) for notification permission, exact-alarm capability and **real full-screen-intent capability/status/settings path**;
5. focused tests proving custom-alarm create/edit preserves `alarmId` and advances revision, old occurrence cancellation/stale-token rejection, clock-picker value mapping, phone alarm-ringtone selection/persistence/fallback, separation of personal alarms from Prayer reminders, and isolation/cancellation of the two one-minute diagnostic test occurrences;
6. frozen GeoNames SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`;
7. `applicationId = com.archimedeprojects.arihna` and no unauthorized dependency/permission drift;
8. persistent Arihna debug certificate fingerprint `13:97:00:8C:1F:96:2D:BB:D3:6D:D8:A8:EA:02:16:AF:DD:06:E4:B2:B3:E0:8B:C0:F6:D4:B5:43:44:D7:B0:FA`;
9. APK SHA-256 + GitHub prerelease asset verification.

After the gate, stop for a new Galaxy S25 physical validation. At minimum the user must be able to run the two one-minute Settings tests and verify automatic full-screen presentation with the screen locked/off, real phone-ringtone playback, offline Adhan playback and `Stop`. The Alarms milestone remains open until an explicit physical `Pass` is received. No later product milestone is authorized by this remediation.
'''

path.write_text(text.rstrip() + appendix + "\n", encoding="utf-8")
