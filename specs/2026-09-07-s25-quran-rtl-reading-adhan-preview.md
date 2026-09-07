# Galaxy S25 remediation — Quran RTL reading + Adhan preview

Date: 2026-09-07
Base approved runtime: `e3350b74e927d5dbc411f85be86be95cf652e93c`

## Physical-device findings

Physical Galaxy S25 validation of the approved Premium Quran + Hijri prerelease exposed four UX defects:

1. The prayer reminder sound dialog expands all bundled Adhan variants in the first surface and selection is silent, so the user cannot audition a recitation before confirming it.
2. The Muṣḥaf pager behaves left-to-right. Quran book navigation must progress right-to-left regardless of the app language.
3. The top action labelled `Sura` is misleading because the destination also contains Juz, Hizb, bookmarks and recent pages.
4. The Muṣḥaf page is visually too small inside excessive chrome and has no immersive reading mode, pinch zoom or pan.

## Required behavior

### Adhan chooser

- The reminder sound dialog MUST expose one compact action labelled `Scegli Adhan` rather than rendering all six variants inline.
- Activating `Scegli Adhan` MUST open a dedicated Adhan list showing the six existing bundled, license-documented variants.
- Tapping an Adhan MUST immediately play an audible preview of the exact bundled raw resource used by the real alarm.
- Starting another preview MUST stop/release the previous preview first.
- Closing/dismissing/confirming the dialog, choosing phone ringtone, choosing silent, or disposing the UI MUST stop/release preview audio.
- Preview playback MUST use a user-listening/media usage rather than alarm usage so auditioning a sound does not behave like a firing alarm.
- Actual alarm playback, persistence format, alarm scheduling and all six existing raw files MUST remain unchanged.

### Quran navigation and index

- Muṣḥaf page progression MUST be explicitly RTL and MUST remain RTL even when the app UI language is Italian/LTR.
- Page 1 → page 2 progression must follow the physical Arabic-book gesture/direction, not the current LTR pager behavior.
- The misleading closed-state label `Sura` MUST be replaced by `Indice` (Arabic: `الفهرس`) or an equivalently clear global-navigation label.
- The opened index MUST clearly expose `Sure`, `Juz`, `Hizb`, `Segnalibri`, and `Recenti` before the user needs to infer where those destinations are.
- Sura text search MUST be visually associated with the Sure view rather than presented as the meaning of the whole index.
- Existing bookmarks, recents, page persistence, easy reading mode and offline behavior MUST be preserved.

### Immersive reading mode

- The Muṣḥaf reader MUST provide a visible `Lettura`/fullscreen action.
- Activating it MUST open a full-screen reading surface that hides Arihna app chrome/bottom navigation and uses edge-to-edge space.
- The full-screen page MUST support pinch-to-zoom and pan, with a bounded scale of at least 1×–4×.
- While zoomed above 1×, page paging MUST be disabled so panning does not fight the horizontal page gesture; returning to 1× re-enables paging.
- A tap-accessible close/back control and bookmark control MUST remain available in reading mode.
- Normal Muṣḥaf mode MUST reduce decorative whitespace so the Quran page occupies materially more of the available viewport.
- The existing 604 pinned SVG Muṣḥaf pages remain the accepted offline source for this remediation. Alternative visual themes may be added later without altering Quran text; any true Tajwīd-color source requires separate license verification.

## Frozen contracts

The remediation MUST preserve:

- applicationId `com.archimedeprojects.arihna`
- minSdk 28, compileSdk 37, targetSdk 37
- GeoNames SHA256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`
- persistent debug certificate SHA256 `13:97:00:8C:1F:96:2D:BB:D3:6D:D8:A8:EA:02:16:AF:DD:06:E4:B2:B3:E0:8B:C0:F6:D4:B5:43:44:D7:B0:FA`
- Quran text source commit `a5284b17034d36567e4a4bac982a17ba56837448`
- Quran acceptance counts: 6236 verses, 114 suras, 30 juz, at least 60 hizb
- 604 pinned Muṣḥaf SVG pages and their verified license
- no background-location permission and no `USE_EXACT_ALARM` permission.

## SPEC-FIRST acceptance gate

The implementation candidate MUST be the direct single-parent child of this SPEC commit. The exact candidate SHA MUST pass:

1. static contract/lineage checks and JVM/build verification;
2. API28 complete connected instrumentation suite with zero failures, errors or skips;
3. API36 location-permission matrix;
4. new static/build assertions proving explicit RTL Muṣḥaf paging, global `Indice`, immersive zoomable reader, compact `Scegli Adhan` UI and preview-player lifecycle.

Only a completely green exact-SHA gate may be promoted to `main`, and promotion MUST be a non-forced fast-forward. Any release APK must use the persistent Arihna signer, be published as a prerelease, redownloaded, SHA256-verified and `apksigner`-verified before physical Galaxy S25 validation resumes.
