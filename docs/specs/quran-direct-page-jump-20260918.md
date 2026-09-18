# SPEC — Quran direct page jump — 2026-09-18

## Baseline
- Base `main`: `4588088ae459b7ac8811fed359e83192bb0681bd`.
- This cycle is a narrow user-visible Quran usability improvement.
- Preserve the physically validated stable behavior and all unrelated app features.

## Goal
Allow a reader to jump directly to an exact Mushaf page from the Quran index without searching by surah or navigating through Juz/Hizb.

## Required behavior
1. In the Quran explorer/index, expose a compact direct-page control with a page input and an explicit action.
2. Accept only decimal page numbers and treat the valid domain as pages **1 through 604**.
3. The action is enabled only when the parsed page is in `1..604`.
4. Selecting a valid page closes the explorer and opens exactly that page.
5. The same control must work in:
   - Hafs Uthmani,
   - Hafs Tajwid,
   - Warsh Nafi.
6. Existing per-mode state behavior remains intact: the destination becomes the visited/current page through the existing reader flow; bookmark namespaces remain unchanged.
7. Do not change Quran corpus text, page artwork, Tajwid rules, Warsh assets, page count, bookmark schema, fullscreen behavior, prayer/location/Qibla/alarm features, permissions, Home, or Settings.

## UI / accessibility contracts
- Keep the control bilingual via the existing `appText` path.
- Add stable test tags for the input and action.
- The control must fit inside the existing explorer without replacing Surah/Juz/Hizb/Bookmarks/Recent navigation.

## Verification
- Add an Android instrumentation regression that opens the explorer and jumps to an exact page in Hafs, Tajwid, and Warsh.
- Candidate must be exactly one commit and a direct child of this SPEC.
- Run exact-SHA static/build + API28 full suite + API36 Quran/permission gate before any promotion.
- Promote `main` only after all required jobs are green, using non-forced fast-forward.
- If an APK is produced for physical validation later, use the persistent Arihna signer and verify the published asset after redownload.
