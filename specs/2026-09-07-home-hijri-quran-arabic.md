# Arihna S25 refinement spec — Arabic, Hijri, Quran reader, volume slider

Date: 2026-09-07
Base runtime: `f5478522533962f44ca9f8879f7d6a82d9c35af4`

## User-requested changes

1. Restore an elegant continuous/discrete Material 3 slider for the phone alarm volume in Settings. Remove the adjacent minus/value/plus control. Preserve the real platform alarm stream semantics, min/max bounds, persisted system volume behavior, and the visible percentage.
2. Add Arabic (`العربية`) as an in-app language alongside Italian. The choice must persist across restarts. Arabic mode must use RTL layout and translate the primary top-level UI chrome and the new functionality added by this cycle. Italian remains the default/fallback.
3. Show the current Hijri date (`Tarikh Hijri`) on Home in addition to the Gregorian date, using Android/Java Hijrah chronology. Do not invent or hard-code a civil conversion for arbitrary dates.
4. Tapping the current date block on Home opens a calendar dialog. The selected Gregorian date must also show its corresponding Hijri date in the dialog. The calendar is a viewer in this cycle and must not silently change prayer-calculation state.
5. Replace the Home quick action `Posizione` with `Corano`; tapping it navigates to the existing Quran top-level destination. Location remains visible/refreshable in the Home header and remains configurable in Settings.
6. Replace the Quran placeholder with a functional offline Quran reader that exposes two reading modes:
   - `Facile da leggere`: the verified Quran text is rendered verse-by-verse with larger type, generous spacing, and minimal metadata.
   - `Ḥafṣ / Uthmani`: the same verified, unmodified Quran text is rendered with verse numbers and explicit Juz / Hizb-quarter markers.
   These are presentation modes over one authenticated text corpus, not different Quran contents or recitations.
7. Quran text and metadata source is Tanzil Quran Text Uthmani v1.1 / Quran metadata, pinned to upstream commit `052b515f3a24dfacbe4cafc3b89f0681a447f462`. The Quran text must be bundled verbatim and unmodified. Attribution and the Tanzil license notice must ship with the app and be visible in the Quran screen. Build/gate must verify the bundled text is byte-identical to the pinned upstream file.
8. Quran reader must support all 6,236 ayat, all 114 surahs, Juz boundaries including Juz 2 at 2:142, and Hizb-quarter metadata from the pinned Tanzil metadata. It must never synthesize Quran text.

## Frozen contracts retained

- applicationId `com.archimedeprojects.arihna`
- minSdk 28, compileSdk 37, targetSdk 37
- GeoNames SHA256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`
- classic Adhan SHA256 `3b350bc210d657727578ed977e32323c06b63c2088c3ebcea9fc9afb7d487702`
- persistent signer certificate SHA256 `13:97:00:8C:1F:96:2D:BB:D3:6D:D8:A8:EA:02:16:AF:DD:06:E4:B2:B3:E0:8B:C0:F6:D4:B5:43:44:D7:B0:FA`
- keystore SHA256 `bc9057f26ad6de7efb70a5df06effb1ed259f1d015c3f062d0f757b3f0983b72`
- alarm/Adhan diagnostic delay remains 10 seconds
- no background-location permission and no `USE_EXACT_ALARM`
- explicit current-location refresh behavior from the promoted runtime must not regress
- Alba dorata visual language remains the visual baseline.

## Acceptance tests / gate

The technical candidate must be a direct child of this spec commit. Gate must run against the exact candidate SHA and include:

- static contract checks for slider present and +/- controls absent;
- Home contract: Quran quick action present, location quick action absent, current location remains in header, Hijri date present, current-date block clickable;
- Android UI test proving date click opens the calendar and Home Quran quick action invokes its destination callback;
- persisted Italian/Arabic language selector with Arabic RTL smoke coverage;
- Quran parser/unit tests proving exactly 6,236 verses, 114 surahs, Juz 1 at 1:1, Juz 2 at 2:142, and valid Hizb-quarter marker data;
- exact byte comparison of the bundled Tanzil Quran text and metadata against the pinned upstream commit plus preservation of attribution/license notice;
- JVM tests + debug build + androidTest compile;
- full API 28 instrumentation with XML validation requiring >0 tests and zero failures/errors/skips;
- API 36 location permission matrix;
- frozen package/SDK/location/alarm/audio/GeoNames contracts.

Only after the exact-SHA gate is fully green may `main` fast-forward non-forced to the candidate. Then build an APK with the frozen persistent signer, verify all frozen assets plus the Quran corpus, publish a GitHub prerelease, and stop at physical Galaxy S25 validation.