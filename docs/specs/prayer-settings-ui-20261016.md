# SPEC — Prayer settings UI — 2026-10-16

Base runtime: `3bd015a592640bfa8da1ef9fb534f7764e242ca1`

## Goal
Expose the prayer-calculation settings that already exist in the domain/persistence layer through a clear user-facing UI, following the approved "B — Completa ma ordinata" direction.

## Requirements
1. The Orari screen has an obvious settings action that opens a dedicated "Impostazioni orari" page.
2. The page exposes and persists:
   - calculation method;
   - Asr method (Standard/Hanafi);
   - high-latitude rule;
   - manual minute corrections for Fajr, Sunrise/Alba, Dhuhr, Asr, Maghrib, Isha.
3. Manual corrections are adjustable with explicit minus/plus controls and are bounded to a safe UI range of -60..+60 minutes.
4. A "Ripristina predefiniti" action restores the existing canonical defaults:
   - Muslim World League;
   - Asr Standard;
   - high-latitude Automatic;
   - all adjustments 0.
5. Changes persist through the existing PrayerSettingsRepository and immediately feed the existing prayer schedule flow; no second source of truth.
6. On the Orari page, non-zero manual corrections are shown next to/under the affected prayer time.
7. The prayer sound dialog keeps Adhan / Android ringtone / Silent and per-prayer volume behavior unchanged, but makes the selected Android ringtone name explicit and provides a clear "Cambia suoneria" action.
8. If Android ringtone profile is selected without an explicit URI, the UI identifies it as the device/default alarm ringtone rather than presenting "Suoneria telefono" as if it were a concrete sound name.
9. Existing alarm/Adhan behavior must not regress, including:
   - natural Adhan auto-dismiss;
   - Makkah Takbir x2;
   - independent per-alert volume;
   - personal alarms remain active until Stop/Snooze;
   - existing prayer scheduling and persistence.
10. Existing offline Quran, location, Qibla and permissions behavior must remain unchanged.

## UX
- Dedicated page title: "Impostazioni orari".
- Sections: Metodo di calcolo, Metodo Asr, Alte latitudini, Correzioni manuali.
- Reset action visible at the bottom.
- Navigation back returns to Orari.
- Current Orari screen remains compact and focused on today's times.

## Tests
- Unit test settings mutations/reset preserve all fields and bounds.
- Android UI test verifies settings page controls and manual correction interaction.
- Android UI test verifies Orari shows non-zero corrections.
- Android UI test verifies selected Android ringtone title is visible and "Cambia suoneria" is exposed.
- Existing prayer/alarm suites remain green on API28 and API36.

## Release discipline
Candidate must be exactly one commit and a direct child of this SPEC commit. Full exact-SHA static/build + API28 + API36 gate is required before any main promotion or S25 prerelease.
