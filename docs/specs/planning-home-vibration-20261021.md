# SPEC — Planning Home shortcut visibility navigation icon distinction, and vibration-only mode — 2026-10-21

## Baseline and physical feedback
- Source runtime: `4e1bbd7514977bea373a7940c4b17adfb63902d2`, tested Galaxy S25 Planning prerelease `planning-4e1bbd75-20261021`. User explicitly said **"PASS tutto"** for functions but reported two UX defects: the Planning shortcut is hidden at the bottom of the Home scroll, and the visual icons for Sveglie and Orari di preghiera look nearly identical.
- This is a follow-up **small independent SPEC** from currently promoted Planning runtime, not an amendment of the historical Planning SPEC or prerelease bytes.
- Existing physically validated stable `arihna-stable-adc40017-20261017` must remain available, and the previously tested Planning prerelease remains immutable.

## Added user authorization — vibration only for all alarm families
- User additionally requests **"attivare vibrazioni al posto di suoneria/adan per sveglie, per adan per tutto"**. Interpret as an explicit **Solo vibrazione** sound choice available per existing personal one-shot/weekday alarm, per individual prayer/Adhan alarm Fajr/Dhuhr/Asr/Maghrib/Isha, and per Planning dated ringing alarm. Do not silently enable for all users; previously persisted selections and volumes must retain their exact behavior by default.
- Persist vibration-only as a first-class `AlarmSoundProfile.VIBRATION`, keeping all historical V1/V2/V3 AlarmRule rows decodable with unchanged format; dated Planning type's persisted sound enum accepts new value. Do not change prayer-volume repository or global Android alarm volume. In each sound selection/editor add Italian/Arabic `Solo vibrazione` choice with clear selected state.
- At delivery, a vibrator starts a repeating, audible-free vibration pattern via `VibratorManager/Vibrator` with the required Android VIBRATE permission, and stops immediately on Stop/Snooze/service destruction. Keep screen/notification/ringing controls intact; Snooze after 5 minutes must also vibrate rather than play audio. For prayer alerts, vibration should automatically end after a bounded 30–45 seconds (not keep vibrating indefinitely); personal alarms should keep existing stop/snooze and safety timeout rules. On devices without haptic hardware, indicate unavailability rather than silently promising vibration.
- Do not play Adhan or Android/vintage ringtone while in vibration-only mode; do not alter selected prayer Makkah x2 rendition in non-vibration mode. Vibration must not write/change STREAM_ALARM volume or overwrite another alarm's sound/volume.
- IMPORTANT: prior v1 Planning user PASS covers **existing sound features only**, not vibration or updated UI. A fresh S25 physical PASS must validate all three changes together.

## Exact UI changes
1. On Home, place a visible, branded Planning entry **immediately beneath the top ARIHNA header/date/location**, ahead of the next-prayer hero, weekly sections, inspiration and quick actions. It must be visible upon initial Home display without scrolling on Galaxy S25. Keep a large enough tap target; content can read `Planning · Da fare e promemoria`; preserve Arabic localization.
2. In Home states Loading, NoLocation and CalculationUnavailable, keep a Planning entry **immediately below the ARIHNA brand heading**, independent of location setup and without scrolling.
3. Remove the bottom-of-Home Planning entry; do not duplicate the shortcut.
4. Keep current six bottom navigation destinations and their semantics. Distinguish visually **Orari** from **Sveglie**: use already-vendored `Icons.Rounded.Mosque` for the `Prayers` tab and retain `Icons.Rounded.Alarm` for the `Alarms` tab. This retains Material icon-size optimization; no new icon dependencies or changes to nonrequested destinations.
5. No changes to Planning domain/scheduling, prayer calculation, Adhan, ringing/sound, Quran, offline assets, permission handling or existing data and navigation behavior. Preserve date-specific reminders and alarms and installed users' stored entries.

## Acceptance and gate
- Compose Android tests assert `home-open-planning` is displayed **before any scrolling** in Ready and NoLocation/Loading/unavailable modes, button invokes callback, and Home's existing landmark test remains valid.
- Gate static contract checks Orari uses Mosque, Sveglie uses Alarm, no new extended Material icons dependency, all source paths within narrow UI+test scope.
- Candidate is exactly one commit direct child of this SPEC commit; gate static/JVM, Android API28 full regression, Android API36 existing Quran/prayer/alarm/Planning/UI matrix. Only all-green exact-SHA candidate can non-force fast-forward main.
- Publish signed Galaxy S25 prerelease using proven persistent signer, redownload and verify APK SHA256, size and signer. Require a **new explicit physical PASS for corrected UI APK** before stable, which must copy precisely those tested bytes (never rebuild to make stable).
