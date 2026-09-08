# SPEC — Quran Muṣḥaf premium — 2026-09-08

## Baseline

- Stable runtime / physical Galaxy S25 PASS: `d66d87615584bdb43721db751a54339ded9607f8`.
- This SPEC MUST be a direct child of that baseline.
- The implementation candidate MUST be a direct single-parent child of this SPEC commit.
- `main` MUST remain on the stable baseline until the exact candidate SHA passes the complete gate.

## Goal

Raise the Quran Muṣḥaf reader from a functional page viewer to a premium, book-like reading experience while preserving all already validated Quran, RTL, bookmark, index, zoom and persistence contracts.

## Source integrity / licenses

- Quran textual source remains pinned to Tarteel/Tanzil commit `a5284b17034d36567e4a4bac982a17ba56837448` with acceptance: 6236 verses, 114 surahs, 30 juz, >=60 hizb, Tanzil CC BY 3.0.
- Muṣḥaf visual source remains `batoulapps/quran-svg` pinned at `78d97544bfdc57e9f04bc97ace3f857ed972d772`, exactly 604 SVG pages plus metadata, MIT.
- The visual styles introduced by this cycle MUST only change Arihna presentation around the same verified page SVG. They MUST NOT be described as a different Quran text, Tajwīd edition or a separately sourced Madinah edition.

## Premium reader acceptance

### 1. Page-first composition

- The Muṣḥaf page must visually dominate the available reader area.
- Use the source page aspect ratio (510.23599 / 729.448) rather than an arbitrary scale crop.
- Remove the previous fixed `graphicsLayer(scaleX = 1.22f, scaleY = 1.22f)` normal-reader enlargement.
- Keep margins narrow and intentional, with a book-like paper treatment rather than a large empty card around the SVG.
- No Quran glyphs may be clipped by normal presentation.

### 2. Two presentation styles using the same verified SVG

Persist a reader presentation preference with exactly these user-facing modes:

- `Classico` — warm paper, subtle outline/shadow/book-edge treatment.
- `Pulito` — minimal paper/background, reduced decoration.

The SVG page bytes and calligraphy stay identical in both modes. The preference survives app restart.

### 3. Immersive reading chrome

- Existing real fullscreen remains the implementation model; do not revert to an Android `Dialog`.
- Fullscreen starts with compact controls visible for discoverability.
- A single tap on the page toggles reader chrome visible/hidden.
- When chrome is hidden, the page is the dominant full-screen content and app navigation/system bars remain hidden.
- Back always exits fullscreen reliably.
- Top chrome contains: close, current Surah/page context, bookmark.
- Bottom chrome contains discreet Juz/Hizb status and a short RTL/zoom hint.
- Controls must not obscure Quran text more than necessary and must remain >=48dp touch targets.

### 4. Fullscreen zoom / pan / RTL

Preserve the physically validated behavior:

- RTL page progression independent of app language.
- Pinch zoom 1x..5x.
- Pan while zoomed.
- Pager disabled while zoomed, enabled again at 1x.
- Changing page resets zoom/pan.

### 5. Metadata and persistence

- Normal reader and fullscreen show current Surah, page, Juz and Hizb in a compact hierarchy.
- Existing bookmarks, last page, recent pages and EASY/HAFS mode persistence remain compatible.
- Add a persistent visual-style preference without invalidating existing stored keys.

### 6. Rendering performance

- Avoid reparsing the same SVG page repeatedly during ordinary page revisits within one app process.
- Add a bounded in-memory cache for rendered Muṣḥaf page drawables/pictures.
- Asset load/parsing remains off the main thread.

### 7. Accessibility / semantics

Add stable semantics/test tags for at least:

- `quran-style-classic`
- `quran-style-clean`
- `quran-premium-page-frame`
- `quran-fullscreen-chrome`
- `quran-fullscreen-page-context`
- `quran-fullscreen-bottom-context`

Existing tags for fullscreen, close, bookmark, RTL pager and zoomable pages remain intact.

## Non-goals

- No Tajwīd colors in this cycle.
- No new Quran textual or visual source.
- No change to Quran verse corpus acceptance.
- No redesign of the Index/Surah/Juz/Hizb explorer beyond regressions required by this reader change.
- No change to Adhan, prayer calculation, location, alarm, Hijri calendar or app identity/SDK contracts.

## Gate

The exact candidate SHA must pass before promotion:

1. static contract assertions + JVM tests + debug APK build;
2. API28 full instrumentation suite, zero failures/errors/skips, including existing fullscreen/index tests and new premium-reader interactions;
3. API36 location permission matrix/regression suite;
4. APK assertions: package `com.archimedeprojects.arihna`, minSdk 28, compile/target 37, frozen GeoNames SHA256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`, 604 Muṣḥaf pages, Quran corpus acceptance, no forbidden manifest regressions.

Only an all-green exact-SHA gate permits non-forced fast-forward promotion of `main`. Then build with the frozen persistent signer, publish a GitHub prerelease, redownload the public APK and verify bytes plus signer before providing it for the next physical Galaxy S25 test.
