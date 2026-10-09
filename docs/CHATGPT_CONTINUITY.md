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
2026-10-09 user confirmed the current Makkah two-takbir behavior is now perfect on Galaxy S25, then requested a product change: there must be no global alarm-volume control; volume must be controlled independently for each personal alarm and each prayer/Adhan.

Actually done / live-verified:
- User feedback is treated as physical Galaxy S25 PASS for the Makkah two-takbir prerelease.
- Live `main` was verified at `2182e303afde1e3823af03cbb8d8afabf3541c52` before starting the new change.
- Inspected current implementation:
  - prayer/Adhan volume already persists independently per prayer (0–100%);
  - personal alarms had `AlarmRule.playbackVolumePercent` available at delivery time but it was not persisted/exposed and custom delivery defaulted to 100%;
  - Settings exposed a global Android `STREAM_ALARM` volume slider.
- Created SPEC branch `spec/per-alarm-volume-only-20261009`.
- SPEC commit: `c2d9a227068b7e7a7fb9e64e0f1f244ee094dab7`.
- SPEC file: `docs/specs/per-alarm-volume-only-20261009.md`.
- Candidate v1 `f2157b67d9b25c94186c27d8f530116159342d4c` was superseded before gate because pre-gate review found one historical codec test still treating V3 as unknown.
- Definitive candidate v2:
  - branch `candidate/per-alarm-volume-only-v2-20261009`;
  - SHA `b7b92ceb72e90748f29132889a5ec64a4c066eff`;
  - exactly one commit, direct child of SPEC;
  - message `feat(audio): make volume per alarm only`.
- Candidate v2 behavior:
  - removes global alarm-volume UI from Settings;
  - Settings no longer changes Android `STREAM_ALARM`;
  - adds 0–100% volume slider to each personal alarm editor;
  - persists personal-alarm volume in alarm-rule format V3;
  - V1/V2 persisted alarms migrate to 100% without losing existing fields;
  - changing sound/ringtone preserves the rule volume;
  - custom alarm delivery uses that rule's persisted volume;
  - prayer/Adhan delivery continues using the separate per-prayer volume repository.
- Added/updated unit and Android UI regressions for persistence, migration, custom editor volume, removal of global UI and delivery propagation.
- Gate driver:
  - branch `driver/per-alarm-volume-only-v2-gate-20261009`;
  - workflow commit `73634e835b4a78b8a6cc2b6a611ad578a4faa2f6`;
  - run `37926520862`.
- Current gate status at last check: in progress.
  - static/build job `113806665782`: in progress; exact lineage/scope contract already PASS.
  - API28 job `113806666291`: in progress; exact candidate check PASS.
  - API36 job `113806665596`: in progress; exact candidate check and setup PASS; emulator tests running.
- `main` has NOT been promoted and remains unchanged while the gate is running.

## Exact next action
- Wait for exact-SHA gate run `37926520862` to complete.
- If all static/build, API28 and API36 jobs PASS, live-recheck `main` is still `2182e303afde1e3823af03cbb8d8afabf3541c52`, then promote candidate v2 `b7b92ceb72e90748f29132889a5ec64a4c066eff` with non-forced fast-forward.
- After promotion, publish an S25 prerelease for the per-alarm volume change, redownload and verify APK SHA-256 + persistent signer + metadata.
- Physical S25 validation for the new change must confirm: no global volume control, independent custom-alarm volumes, independent per-prayer/Adhan volumes, and no cross-effect between alerts.
