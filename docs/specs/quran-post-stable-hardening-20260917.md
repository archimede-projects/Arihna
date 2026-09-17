# Quran post-stable hardening — 2026-09-17

## Baseline

- Previous verified runtime: `be4af47d4f5b267e47eac09d761775a2d99bf0db`.
- Stable release: `arihna-stable-be4af47d-20260917`.
- Physical Galaxy S25 smoke and stable install/open sanity are already PASS by user report.

## Goal

Add deterministic regression coverage for long-session Quran reader robustness without adding product features or changing intended runtime behavior.

## Required behavior

1. Repeated switching among Hafs, Tajwid and Warsh must not crash or cross-contaminate per-mode page state.
2. Hafs and Warsh bookmarks must remain independent across repeated mode switches.
3. Tajwid page bookmarks/history must remain independent from Hafs/Warsh page bookmarks/history.
4. Repeated page-history writes must remain bounded to the existing 8-entry MRU contract, deduplicated and clamped to valid page indices `0..603`.
5. Repeated fullscreen open/close cycles must return the immersive callback to `false` after close and must not lose the active mode/page state.
6. Existing Quran assets, 604-page mapping, Hafs/Tajwid/Warsh behavior, bookmarks, fullscreen, disclaimer, Home, Prayer, Location, Qibla, alarms and permissions must remain unchanged.

## Candidate scope

Prefer test-only hardening. If the new regressions expose a deterministic product bug, stop and create a separate corrective candidate from this same SPEC; do not silently broaden this candidate.

Expected candidate file scope for the first attempt:

- `app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranPostStableHardeningAndroidTest.kt`

No production source, assets, manifest, Gradle configuration or unrelated tests may change in candidate 1.

## Regression requirements

Candidate 1 must add Android instrumentation coverage that:

- stress-writes Hafs, Warsh and Tajwid page histories over many iterations and verifies last-page separation, MRU max size 8, uniqueness and valid bounds;
- stress-switches the Compose reader repeatedly across Hafs → Tajwid → Warsh → Hafs and verifies each mode restores its own previously persisted page;
- verifies Hafs, Warsh and Tajwid bookmarks remain independent after repeated switching;
- repeatedly opens/closes fullscreen readers and verifies immersive state is reset after close;
- completes without uncaught exception or timeout on API28 and API36.

## Gate

Before any promotion:

- candidate must be exactly one commit and a direct child of this SPEC;
- exact candidate SHA must be checked out by CI;
- `main` must still equal the previous verified runtime while the gate runs;
- changed file scope must exactly match candidate scope;
- unit tests, AndroidTest compile and debug APK build must pass;
- full connected suite must pass on API28 with zero failures/errors/skips;
- Quran hardening instrumentation plus existing Tajwid corpus regression must pass on API36;
- package/SDK/manifest/GeoNames contracts must remain unchanged.

## Promotion / release policy

If candidate 1 is test-only and all gates pass, it may be fast-forward promoted to `main` as repository hardening without producing a new user APK, because Android test sources are not packaged in the app. The already physically validated stable APK remains the user baseline unless production runtime bytes change.
