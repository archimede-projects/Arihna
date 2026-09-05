from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text()

marker = "#### Alarms milestone sequence\n"
if marker not in text:
    raise SystemExit("Alarms milestone sequence marker not found")

section = r'''#### STEP 7 physical FAIL remediation — APPROVED 2026-09-05

Galaxy S25 physical validation of prerelease `alarms-s25-310413e8-20260905` is **FAIL**. The user observed that the current `Suono` action does not expose a meaningful sound-selection interaction, no Adhan choice is visible, and a real custom-alarm test did not audibly ring. The current notification-only delivery is therefore not accepted as the final alarm experience. STEP 7 remains open and the Alarms milestone must not be closed from the previous automated evidence.

This physical FAIL explicitly authorizes one corrective runtime round before STEP 7 can be revalidated. The correction is limited to alarm delivery/ringing, full-screen alarm presentation, sound selection and the Alarms-screen visual treatment. Prayer calculation, Prayer Schedule inputs, Location behavior, Qibla math/sensors, exact-occurrence resolution, stale-token validation, recurrence semantics, persistence isolation and existing exact-alarm scheduling semantics must remain unchanged except where the sound-profile schema must add the new Adhan value.

**Real ringing session — required:**
- A validated exact-alarm occurrence must no longer rely on notification-channel sound alone as the ringing engine. After existing occurrence-token validation, Arihna starts a bounded, user-stoppable ringing session dedicated to that occurrence.
- Background audio on target API37 must comply with Android 17 background-audio hardening: while ringing in background Arihna uses a `mediaPlayback` foreground service and alarm audio marked with `AudioAttributes.USAGE_ALARM`. The service exists only for an actively validated alarm and stops when the user stops/dismisses the ringing session, when playback finishes where appropriate, or at a bounded safety timeout; no always-on service, polling loop or unrelated background media behavior is authorized.
- This correction authorizes only the minimal manifest additions required for that bounded session: `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, plus the service declaration with `foregroundServiceType="mediaPlayback"`.
- `SYSTEM_DEFAULT` uses the device alarm ringtone as an actual ringing source rather than depending solely on the notification channel. It must be audible under normal alarm-volume/device policy and keep ringing until stopped or the bounded safety timeout.
- `SILENT` remains intentionally silent but still produces the urgent alarm surface/notification.
- A Prayer-linked alarm may use `ADHAN`. The bundled Adhan recording is the Wikimedia Commons file `Muslim calling to prayer.ogg`, author `Aishatu98`, dated 2026-07-15, released under CC0 1.0. Bundle the verified source artifact locally so Adhan playback is offline and record its provenance in-repository. Do not synthesize, truncate, splice or silently alter religious audio merely to fit the UI. The exact bundled byte hash must be recorded by the technical gate.

**Full-screen alarm presentation — required:**
- Add a dedicated alarm-ringing Activity reached through a high-importance `CATEGORY_ALARM` notification full-screen intent. This correction explicitly supersedes the earlier STEP 1/STEP 5 exclusion of full-screen intents for the Alarms milestone.
- Declare `USE_FULL_SCREEN_INTENT`. On supported Android versions, detect whether full-screen-intent access is currently usable (`NotificationManager.canUseFullScreenIntent()` on API34+) and expose a user-triggered CTA to `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` when access is unavailable. Never claim full-screen readiness while Android reports it unavailable.
- The ringing Activity must use the platform lock-screen/turn-screen-on APIs, show the alarm/prayer identity and current time, and provide an obvious **Stop** action. It must not fabricate Quran/hadith/religious quotations.
- Android remains authoritative about presentation: with permission and a locked device, the full-screen Activity is the intended path; on an unlocked modern device Android may render the urgent expanded/heads-up form instead of forcibly replacing the foreground screen. Arihna must still ring and provide a working stop action in that system-governed case.
- No unrelated lock-screen takeover, overlay permission, accessibility trick, battery-optimization exemption or device-admin behavior is authorized.

**Sound selection + WOW UI — required:**
- Replace the current binary `Suono` toggle with an explicit, polished selection surface (Material dialog/bottom sheet or equivalent) that makes the current selection and available choices obvious before committing a change.
- Prayer-linked alarms offer at least **Adhan**, **Suono sveglia di sistema**, and **Silenzioso**. Newly created Prayer-linked rules default to **Adhan** after this correction. Existing persisted rules keep their current profile until the user changes them.
- Custom alarms offer at least **Suono sveglia di sistema** and **Silenzioso**; Adhan is not required for custom alarms in this corrective scope.
- `AlarmSoundProfile` adds `ADHAN`. Existing deterministic `alarm.rules.v1` persistence must remain backwards-compatible with stored `SYSTEM_DEFAULT` and `SILENT` rows; no destructive migration and no new persistence dependency are allowed.
- Restyle the Alarms surface to the established Arihna visual language already proven in Qibla: emerald/gold hierarchy, rounded cards, clear selected states and a subtle Islamic visual motif where appropriate. The result must remain legible and operational on the Galaxy S25, not merely decorative. Preserve accessibility semantics/test tags for the functional controls.

**Delivery/cancellation correctness:**
- Existing stale/duplicate protection remains authoritative: a stale PendingIntent must never start audio or a full-screen ringing Activity.
- The stop action must stop audio/foreground service and cancel the active ringing notification without deleting or disabling a recurring rule. A validated one-shot custom rule may still auto-disable only after the validated occurrence is accepted for delivery according to the existing one-shot contract.
- Reconciliation after validated delivery remains required so the next recurrence is scheduled exactly once.

**Corrective gate before a replacement S25 APK:**
1. exact corrective technical SHA with this spec commit as direct parent;
2. unfiltered `testDebugUnitTest` and `assembleDebug`;
3. full Android 9/API28 connected regression, zero skipped, preserving all existing suites;
4. focused modern-API coverage for notification permission, exact-alarm capability, full-screen-intent capability/settings path and the target-API37 ringing-service/audio contract;
5. tests proving `ADHAN` persistence round-trip plus backward decode of the old two sound profiles, explicit sound-picker selection, full-screen PendingIntent wiring, stop/cancel behavior and stale occurrence rejection before ringing;
6. frozen GeoNames SHA, applicationId and unrelated permission/dependency policy checks;
7. persistent Arihna debug signing, APK SHA-256 and a **new** prerelease tag/asset. The failed `alarms-s25-310413e8-20260905` prerelease stays historical and must not be reused as proof of a PASS.

After the corrective gate and replacement prerelease are green, stop again for a new physical Galaxy S25 Pass/Fail. Required revalidation includes a short custom alarm while the screen is locked (audible ring + full-screen alarm surface + Stop), a Prayer-linked alarm using Adhan, full-screen-access denial/recovery, screen-off delivery, and no duplicate/stale delivery. Only an explicit physical **Pass** after that replacement APK may authorize the final documentation-only Alarms milestone closure.

'''

if "#### STEP 7 physical FAIL remediation — APPROVED 2026-09-05" in text:
    raise SystemExit("remediation section already present")
text = text.replace(marker, section + marker, 1)

old = '''5. **STEP 5 — functional alarm UI + notification/sound profiles: CLOSED.** Exact runtime `310413e886e9d7d54ea6f86c3fc7849360069c49`; definitive gate `33952510472` / `101269810089`; API28 62/62, 0 failed, 0 skipped.
6. **STEP 6 — definitive regression/package gate: AUTHORIZED / IN PROGRESS.** Same exact STEP 5 runtime only; API28 + API36 permission/capability matrix, identity/signing checks and persistent-debug Galaxy S25 prerelease.
7. **STEP 7 — Galaxy S25 reliability validation + documentation-only closure: NOT STARTED.** Hardware validation first; closure commit after explicit physical PASS, with no runtime change.'''
new = '''5. **STEP 5 — functional alarm UI + notification/sound profiles: CLOSED, but physical delivery defect found later.** Exact runtime `310413e886e9d7d54ea6f86c3fc7849360069c49`; definitive gate `33952510472` / `101269810089`; API28 62/62, 0 failed, 0 skipped.
6. **STEP 6 — definitive regression/package gate: CLOSED for runtime `310413e886e9d7d54ea6f86c3fc7849360069c49`.** Definitive regression run `33954187010` passed API28 62/62 with 0 failed/errors/skipped plus the API36 exact-alarm/notification permission matrix; persistent-signed prerelease `alarms-s25-310413e8-20260905` was published. This automated closure did not prove hardware delivery.
7. **STEP 7 — Galaxy S25 reliability validation: PHYSICAL FAIL / CORRECTIVE ROUND AUTHORIZED.** The 2026-09-05 S25 test found no audible custom-alarm ring and inadequate sound-selection/Adhan UX. Implement only the remediation contract above, run a new exact-SHA full gate, publish a replacement S25 prerelease, and stop for a new physical Pass/Fail. Documentation-only milestone closure remains forbidden until explicit physical PASS.'''
if old not in text:
    raise SystemExit("expected milestone sequence block not found")
text = text.replace(old, new, 1)

path.write_text(text)
