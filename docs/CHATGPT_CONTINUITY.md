# Arihna — ChatGPT continuity state

> Persistent cross-chat handoff on dedicated branch `chat-context`. Never merge this branch into `main`.

## Mandatory protocol
1. Read this file first for every Arihna prompt.
2. Live-verify GitHub before claiming current SHA/PASS/release/APK state.
3. After every user prompt, update this file with request, actual work, verified identifiers, failures, current state and exact next action.
4. Never move `main` for continuity updates and never merge `chat-context` into `main`.
5. Never store secrets.
6. If GitHub write access is unavailable, say so.

## User working style
- Italian, concise, operational.
- Proceed directly when context is sufficient.
- Give brief progress updates during long CI/release work.
- Never claim PASS/release/APK verification without evidence.
- Physical Android validation is performed by the user on Galaxy S25.
- Consultant-ready replies should be delivered in one single copyable block.

## Engineering / release discipline
- SPEC-first for runtime/code changes.
- Candidate = exactly one commit, direct child of SPEC; failed/superseded replacements must be sibling candidates from the same SPEC.
- Full exact-SHA gates include API28 and API36.
- Promote `main` only after green gates, via non-forced fast-forward.
- Changed user-facing runtimes go to S25 prerelease first.
- Use persistent Arihna signer; redownload published APK and verify SHA-256 + signer before handoff/stable claims.
- Preserve unrelated features unless SPEC explicitly changes them.

## Last physically validated stable baseline
- Stable tag: `arihna-stable-977fbde9-20260921`
- Runtime: `977fbde989facc3b392f6af5a65449a5322d58c6`
- Release id: `392867609`
- Asset: `arihna.apk`
- Size: `386945577`
- SHA-256: `002d640a734eadf46b141b4489ae3c26d2803d9d49b4bacc772f7c678ab20813`
- Signer cert SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- User physically validated the page-jump runtime PASS on Galaxy S25.
- This remains the stable user baseline until the new Imlāʾī runtime is physically validated.

## Product definition: القرآن بالرسم الإملائي
- Former “Facile” means exactly Quran in **الرسم الإملائي**.
- It is a writing/orthography presentation, not a separate recitation mode and not paraphrased/simplified Quran content.
- Preserve Quran wording, ayah identity/order and semantics.
- Keep normal 604-page navigation, swipe, index, direct page jump, bookmarks/history, fullscreen and persisted page state.
- Hafs/Tajwid/Warsh remain distinct reading/riwaya identities.
- Imlāʾī must not be silently applied to Tajwid or Warsh without separately verified support.

## Imlāʾī SPEC
- Branch: `spec/quran-imlai-writing-20260921`
- SPEC commit: `3da807cef157d57a54bc426295eb54f66830158d`
- Parent stable runtime: `977fbde989facc3b392f6af5a65449a5322d58c6`
- File: `docs/specs/quran-imlai-writing-20260921.md`

## Source verification
Selected source:
- Tanzil Quran Text — **Simple, Version 1.1**
- Tanzil documents Simple as Imla'ei script.
- License: Creative Commons Attribution 3.0; verbatim redistribution allowed with attribution/link, text changes prohibited.
- Primary docs: `https://tanzil.net/docs/Quran_Text_Types`, `https://tanzil.net/docs/Text_License`, `https://tanzil.net/download/`

Pinned reproducible mirror used for CI/build:
- repository: `dotquran/corpus`
- commit: `c23f5cec2e95e253dc450bd0f34d09e37ba40fac`
- path: `src/resources/simple.txt`
- Git blob: `b7b0b3db111cf183d1439ff76dc38d61d743592d`

Verified before candidate and again in CI:
- 6,236 ayat
- 114 surahs
- 6,236 unique `surah:ayah` keys
- first key `1:1`
- last key `114:6`
- key sequence exactly matches Arihna's pinned Hafs Uthmani structure
- bundled file retains Tanzil copyright/license notice
- provenance documented in `docs/quran/IMLAI_SOURCE.md`

## Imlāʾī candidates
### Candidate v1 — superseded before gate
- branch: `candidate/quran-imlai-writing-v1-20260925`
- SHA: `40ad3ccf4bcc00131fe33824529c0eefe18bfdfa`
- direct child of SPEC
- superseded during pre-gate review because one new test needed explicit Tajwid bookmark-state reset for deterministic isolation.
- v1 was not promoted.

### Candidate v2 — definitive
- branch: `candidate/quran-imlai-writing-v2-20260925`
- SHA: `0d7d12c2e89a8012446bde6967d366f31ca8e77e`
- direct child of SPEC `3da807c...`
- exactly one candidate commit
- message: `feat(quran): add imlai writing presentation`

Changed candidate files:
1. `app/build.gradle.kts`
2. `app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranFullscreenAndroidTest.kt`
3. `app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranImlaiAndroidTest.kt`
4. `app/src/main/java/com/archimedeprojects/arihna/feature/quran/MushafRepository.kt`
5. `app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranCorpus.kt`
6. `app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt`
7. `docs/quran/IMLAI_SOURCE.md`

Implemented behavior:
- `QuranWritingStyle { MUSHAF, IMLAI }` separates writing presentation from reading/riwaya identity.
- Legacy stored `EASY` migrates deterministically to Hafs + Imlāʾī.
- UI tab now exposes `Imlāʾī / إملائي`; active header shows `Scrittura imlāʾī / الرسم الإملائي`.
- Imlāʾī uses normal 604-page Hafs page mapping with horizontal swipe.
- Keeps Hafs page state, index, direct page jump, Hafs bookmarks/history and fullscreen.
- Adds text-size controls and readable RTL text-page rendering.
- Source attribution is visible and includes a user-facing `tanzil.net` link.
- Tajwid/Warsh remain separate and unchanged in source identity.
- Old single-surah `EasyQuranReader` behavior is removed from the active product flow.

Automated Imlāʾī regressions include:
- corpus completeness/alignment;
- legacy EASY migration;
- page jump;
- bookmark namespace behavior;
- fullscreen;
- switching through Tajwid/Warsh back to Imlāʾī without losing Hafs page state;
- Arabic diacritic/non-empty rendering checks.

## Exact-SHA gate — PASS
Driver:
- `driver/quran-imlai-writing-v2-gate-20260925`
- workflow commit: `45936c0c2b9b4f8337aa00c9439f854590f0ccfe`

Run:
- `36095864897` — completed/success

Jobs:
- static/build `107947891603` — success
- API28 full suite `107947891322` — success
- API36 Quran + Imlāʾī + hardening + Tajwid + permission matrix `107947891598` — success

Static/build additionally verified the generated Imlāʾī corpus inside the APK: 114 surahs, 6,236 ayat, exact structural key alignment with pinned Hafs and Tanzil notice present.

## Promotion
- Before promotion, live `main` was rechecked at `977fbde989facc3b392f6af5a65449a5322d58c6`.
- Promoted with `force=false`.
- Live recheck confirms current `main = 0d7d12c2e89a8012446bde6967d366f31ca8e77e`.
- Parent is SPEC `3da807cef157d57a54bc426295eb54f66830158d`.

## S25 validation prerelease — published and verified
Release driver:
- `driver/quran-imlai-writing-v2-release-20260925`
- workflow commit: `f760fa2892b280c329ee8d507290f9d65ff90773`

Release workflow:
- run `36096808063` — completed/success
- job `107950673634` — completed/success
- workflow rebuilt exact promoted runtime, restored frozen GeoNames + persistent signer, verified corpus/package/SDK/signer, published prerelease, redownloaded it, and reverified digest/size/signer/metadata.

Release:
- tag: `quran-imlai-0d7d12c2-20260925`
- release id: `396319251`
- title: `Arihna — Quran Imlāʾī — S25 validation`
- target: `0d7d12c2e89a8012446bde6967d366f31ca8e77e`
- `draft=false`
- `prerelease=true`
- published: `2026-09-25T05:06:47Z`
- asset id: `587577666`
- asset: `arihna-quran-imlai.apk`
- size: `387455087`
- SHA-256: `3f66fe87d29aab6d4163620339a06908e24e4a9b7431b4af02f979f88e9183ff`
- signer cert SHA-256 verified: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- direct download: `https://github.com/archimede-projects/Arihna/releases/download/quran-imlai-0d7d12c2-20260925/arihna-quran-imlai.apk`

## Latest user request / actual work
2026-09-25 user sent `?` asking for status after `Procedi`.

Actually done:
- Read continuity from `chat-context`.
- Live-verified current `main = 0d7d12c2e89a8012446bde6967d366f31ca8e77e` (`feat(quran): add imlai writing presentation`).
- Live-verified exact-SHA gate run `36095864897` remains `completed/success`.
- Live-verified S25 prerelease workflow run `36096808063` remains `completed/success`.
- Live-verified prerelease `quran-imlai-0d7d12c2-20260925` still targets the current runtime, is `draft=false`, `prerelease=true`, and has single asset `arihna-quran-imlai.apk`.
- Live-verified asset size `387455087` and SHA-256 `3f66fe87d29aab6d4163620339a06908e24e4a9b7431b4af02f979f88e9183ff`.
- No runtime code, `main`, CI, tag, release, or APK state changed this turn.

## Current state / exact next action
- Repository/CI/release side for the Imlāʾī feature is complete and green.
- Current `main` is the new Imlāʾī runtime `0d7d12c2...`.
- Last stable remains `arihna-stable-977fbde9-20260921`; Imlāʾī is not stable yet.
- Exact next action: user installs `arihna-quran-imlai.apk` on Galaxy S25 and validates:
  1. readability/correct rendering of `الرسم الإملائي`;
  2. swipe/page progression including edge pages;
  3. index + direct `Vai a pagina`;
  4. bookmark/history;
  5. fullscreen + text-size controls;
  6. switch Imlāʾī → Hafs → Tajwid → Warsh → Imlāʾī and verify location/state stability;
  7. no clipping, crash, or obvious text corruption.
- If PASS, publish a new stable release using the physically validated prerelease bytes and reverify SHA/signer/metadata.
