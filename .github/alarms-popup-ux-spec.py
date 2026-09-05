from pathlib import Path

path = Path('PROJECT_SPEC.md')
text = path.read_text()
marker = '#### Galaxy S25 alarm popup/editor polish — APPROVED 2026-09-05'
if marker in text:
    raise SystemExit('spec marker already present')

section = r'''

#### Galaxy S25 alarm popup/editor polish — APPROVED 2026-09-05

Physical validation of the `e5799896c7c266cca2f15a1d2b16f68a139ec3a0` S25 build confirms that the underlying alarm rings, but the personal-alarm presentation/editor still does not match the approved user-centred interaction. This corrective round is intentionally limited to the **personal Sveglie screen and the active custom-alarm notification**; other Arihna screens are out of scope until this round is physically reviewed.

Approved corrective scope:

1. **Active custom alarm notification:** when a personal alarm is ringing while Android remains visible, Arihna must present a high-importance, ongoing alarm notification designed to surface as the platform/OEM alarm heads-up presentation, with directly actionable **`Interrompi`** and **`Rinvia`** controls. `Interrompi` stops the active ring. `Rinvia` postpones the same active alarm by **5 minutes**. Preserve the existing lock-screen/full-screen intent path for conditions where Android permits/chooses full-screen alarm presentation; do not claim control over OEM rendering beyond the Android notification APIs.
2. **No transient sound-update banner:** remove the user-visible `Suono aggiornato` message/card from the Sveglie screen. Sound selection must update the editor state without adding that unexplained status banner.
3. **Compact weekday selector:** the personal-alarm editor shows seven equal compact day controls using only the single Italian initials **`L M M G V S D`**. No stacked or three-letter weekday labels in the editor.
4. **Samsung-like personal sound row:** remove the personal-alarm editor's separate Adhan/System/Silent choice cards and remove the `Cambia suoneria` button. Replace them with one compact **`Suono`** row that shows the selected Android alarm-ringtone title, opens the Android alarm-ringtone picker when the row/title is tapped, and has an independent sound enabled/disabled switch at the trailing edge. New personal alarms default to sound enabled and the current/default Android alarm ringtone. Existing custom ringtone URI/title remains persisted through the existing Alarm rule storage.
5. **No `Silenzioso` choice for personal alarms:** `Silenzioso` must not appear as a selectable personal-alarm sound option. The trailing `Suono` switch is the only personal-alarm audio on/off control. Internally the existing `SILENT` profile may continue to represent switch-off for persistence/backward compatibility; this is not a user-facing sound choice.
6. **Adhan scope preserved elsewhere:** this editor correction does not remove or alter Prayer-linked Adhan behavior, the bundled offline Adhan artifact, Prayer scheduling, or the already-authorized Adhan diagnostic test. It only removes Adhan as a choice from the **personal custom-alarm editor**.
7. **Visual polish:** refine the personal Sveglie/editor presentation within the already approved Arihna emerald/gold/off-white language: stronger hierarchy, cleaner spacing, compact premium controls and clear tap targets. Do not redesign Home, Orari, Qibla, Corano or Impostazioni in this round.
8. Preserve `applicationId = com.archimedeprojects.arihna`, the persistent debug signing identity, existing GeoNames/Prayer/Qibla behavior, existing permissions, dependency set and DataStore namespace. No new dependency or permission is authorized.

Validation requirements for this corrective runtime:

- JVM/build regression and Android 9/API28 connected suite remain green with zero skipped tests.
- Add/update Compose coverage proving the single-letter day selector, absence of `Silenzioso`, absence of the separate `Cambia suoneria` control, and the single tappable ringtone row with a sound switch.
- Add/update notification/platform coverage proving the ringing notification is ongoing/alarm-category and exposes both `Interrompi` and `Rinvia` actions without removing the full-screen intent path.
- Add unit/platform coverage for the 5-minute snooze scheduling contract so `Rinvia` cannot be a decorative action.
- The next user checkpoint is a newly packaged Galaxy S25 prerelease APK from the exact green corrective runtime. No other screen work begins before that physical review.
'''

path.write_text(text.rstrip() + section + '\n')
