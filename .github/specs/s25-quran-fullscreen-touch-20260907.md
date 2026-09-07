# S25 Quran fullscreen + touch targets SPEC

Baseline runtime: `abb3af1109c115b67bfa7c47117596b6688ab16d`.

Physical Galaxy S25 findings:
- The Quran `Lettura` control is visible but tapping it produces no visible fullscreen reader.
- Bookmark controls are too difficult to tap reliably.
- Juz/Hizb controls in the Quran index are too small/dense to tap comfortably.

Acceptance criteria:
1. `Lettura` opens a real in-app immersive Quran reading mode, not a Compose Dialog overlay.
2. Immersive mode hides the normal Quran header, attribution and app bottom navigation while active, and provides an obvious close control.
3. Immersive mode preserves Quran RTL paging, bookmark toggle, pinch-to-zoom up to 5x and panning while zoomed.
4. The normal reader bookmark control has a touch target of at least 52dp.
5. Quran index Juz and Hizb selectors have at least 48dp touch height and clear selected state; bookmark/recent shortcuts are at least 56dp high.
6. Juz/Hizb destination rows remain whole-row clickable and are at least 56dp high.
7. Add an Android UI regression test that enters Quran/Hafs mode, taps `Lettura`, and asserts `quran-fullscreen-reader` is displayed; test also verifies the close control returns to normal reader.
8. Existing Quran corpus, 604-page Mushaf assets, RTL behavior, Adhan preview, location/prayer/alarm behavior and frozen signing/application values remain unchanged.
9. Candidate must be a direct child of this SPEC commit and must pass exact-SHA static/JVM build, API28 full suite and API36 location permission matrix before main promotion.
