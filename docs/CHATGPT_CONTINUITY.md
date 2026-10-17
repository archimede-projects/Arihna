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
- In the next chat, bootstrap from this continuity file and continue the APK-size optimization stream.
- First inventory all Material icon imports in main source and resolve the exact official source definitions/version.
- Then implement on a candidate branch from SPEC `e623ba867e6e56c7bc9c6ac7b69cb28ac03e6aea`, keeping exactly one candidate commit direct child of SPEC.
- Measure before/after APK size + DEX totals and run full exact-SHA static/API28/API36 gate before promotion.
- Never touch the validated stable bytes; stable `c5d05fb7...` remains rollback/reference baseline.

## 2026-10-17 — APK-size Material icons optimization (live update)
- User requested to resume from existing SPEC; never touch physically validated stable bytes and never merge chat-context into main.
- Live GitHub verification: main is 3492add5cb3f74b5ffe4e477055be7aa676fb6c4; spec/apk-size-icons-20261016 is e623ba867e6e56c7bc9c6ac7b69cb28ac03e6aea, parent main. Stable release arihna-stable-3492add5-20261016 id 414962535 is non-prerelease; asset arihna.apk id 647334340, 387885070 bytes, digest SHA-256 c5d05fb7d4ac9b386ef1bd105a87c677f9691a922361ec9430985f3346dac64e (GitHub release metadata). Stable release body asserts physical S25 validation and signer 1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa. Stable not modified.
- Gradle live compose BOM 2026.08.00, officially maps androidx.compose.material:material-icons-extended and core to 1.7.8.
- Automated complete main-source inventory (120 Kotlin files): 24 used Rounded icons in 8 files: Alarm, ArrowBack, AutoAwesome, Bookmark, BookmarkBorder, ChevronLeft, ChevronRight, Close, Explore, Fullscreen, History, Home, KeyboardArrowLeft, LocationOn, MenuBook, Mosque, MyLocation, Notifications, Refresh, Schedule, Search, Settings, Share, VolumeOff. Quran unused import AutoStories (will drop).
- Official Google Maven 1.7.8 sources.jar downloaded in run v1 with SHA-256: material-icons-core=14db888d2f698178799a4c18a58bb134b3e98063c6748c443b339044e0145432, material-icons-extended=77b7e782a519a3e074fc91e09d59334ac1b815edd87d2e89c0c9ca7c95678e93.
- Created isolated preflight driver branch driver/apk-size-icons-preflight-20261017 from SPEC (driver is NOT candidate). It contains tools/icons/apk_size_preflight.py and .github/workflows/apk-size-icons-preflight.yml.
- Driver first run 39013520361 failed conservatively during source selection for ArrowBack: matched both normal Rounded and AutoMirrored Rounded source. No candidate created. Adjusted source selector to exact /icons/rounded/ to exclude autoMirrored.
- Driver v2 head a9b05dafcecf8a0b3c0cd340f16c6937e526d42a, run 39013593570: started, result not yet verified at time of this continuity update.
- Preflight protocol before a candidate: exact source inventory, fetch official AndroidX 1.7.8 icons and copy only extended-used source, replace dependency with icons-core, preserve icon shape, restore frozen GeoNames, build unit/AndroidTest/APK, redownload stable unchanged (verify digest), byte-compare protected offline assets, measure APK and DEX before/after, require material reduction, only then create candidate as single direct child of SPEC.
- main remains untouched. No gate PASS, candidate, S25 prerelease or new stable may be asserted until GitHub confirms.
- Exact next action: inspect v2 run 39013593570 and its logs. If preflight fails, fix in driver and rerun (never alter existing candidate); if it passes, verify new candidate head and parent, then launch full exact-SHA static/API28/API36 gate. Update this same continuity file after any new verified result.

## 2026-10-17 — APK-size icon candidate v1 created; full gate launched
- Driver v2 run 39013593570 and job 117341027038: completed/success. Unit tests + AndroidTest compile + assembleDebug PASS. Official 1.7.8 source extraction PASS.
- Verified preflight APK baseline physically validated stable: 387885070 bytes, sha256 c5d05fb7d4ac9b386ef1bd105a87c677f9691a922361ec9430985f3346dac64e.
- New debug APK on candidate: 356542262 bytes, sha256 0b4b7110d3376b554585e1272819f18eadb764546d4c9038354185e611c99547 (preflight run artifact measurement, not yet released or signer validated). APK savings 31342808 bytes (8.08%, 31.34 decimal MB).
- Stable ZIP DEX remeasured in preflight: 68935380 bytes compressed/raw, 20 DEX files; new ZIP DEX 37599028 bytes compressed/raw, 20 files; savings 31336352 bytes. The preflight's directly measured stable DEX differs slightly from 68830988 earlier SPEC estimate; prefer actual recomputed zip measurement with defined method.
- 1227 protected offline APK entries (Hafs, Warsh, Quran, GeoNames, raw audio) all compared byte-for-byte by SHA-256 and PASS.
- Unique 24 used Rounded icon definitions: 10 from material-icons-core:1.7.8 transitive explicit dependency and 14 vendored verbatim from official material-icons-extended:1.7.8 sources.jar. Unused AutoStories import removed.
- Candidate branch: candidate/apk-size-icons-v1-20261017; candidate SHA adc40017694af03723c7c57afb996306c0ddcd39, exactly one commit direct child of SPEC e623ba867e6e56c7bc9c6ac7b69cb28ac03e6aea (compare ahead_by=1 behind_by=0). 18 changed files, including 14 exact icons, dependency swaps and provenance docs.
- Candidate documentation: docs/size/MATERIAL_ICONS_SOURCE.md with official source hashes, complete inventory, provenance, exact before/after measures, protected asset audit.
- Before gate, live main still 3492add5cb3f74b5ffe4e477055be7aa676fb6c4, stable untouched.
- Gate driver from candidate: driver/apk-size-icons-v1-gate-20261017; workflow commit 801792aee11cb2e5388e59e482d62e4a85c6cac1; workflow .github/workflows/apk-size-icons-exact-sha-gate.yml; run 39014049568 queued at last check. Three jobs: exact-SHA static/JVM/build/size, API28 full Android suite, API36 Quran/prayers/alarms/permissions matrix.
- NO gate PASS, no main promotion, no S25 prerelease and no stable replacement yet; all depend on full gate success.
- Exact next action: inspect run 39014049568 job statuses and logs; if all PASS then non-force fast-forward main from current stable baseline to verified candidate, publish S25 prerelease with persistent signer, verify redownload digest/certificate, await Galaxy S25 physical PASS before stable. If any gate fails, fix via sibling candidate from SPEC; never rewrite or merge speculative candidate. Keep updating chat-context only for continuity.

## 2026-10-17 — APK icons full exact-SHA gate PASS, main non-force promoted, prerelease started
- Full gate run 39014049568 completed/success; three jobs all completed/success: static/JVM/build/size 117342356971, API28 full Android suite 117342356826, API36 Quran/prayers/alarms/permissions 117342357108.
- Static gate confirmed generated debug APK 356542262 bytes, compressed/raw DEX 37599028 bytes across 20 dex files. Exact candidate SHA adc40017694af03723c7c57afb996306c0ddcd39.
- Before promotion reverified old main 3492add5cb3f74b5ffe4e477055be7aa676fb6c4 and completed/success gate, then updated main using force=false and expected_sha old baseline. Operation returned success; live GitHub branch/main now adc40017694af03723c7c57afb996306c0ddcd39.
- Historic physically validated stable release arihna-stable-3492add5-20261016, APK SHA c5d05fb7d4ac9b386ef1bd105a87c677f9691a922361ec9430985f3346dac64e remains untouched; do not treat updated main as a new physically validated stable.
- Created release driver branch driver/apk-size-icons-v1-release-20261017 from promoted candidate. Driver workflow commit 3423f2dfb29e0ea815d1c2bee775db588f9b5b1f, path .github/workflows/apk-size-icons-v1-release.yml, adapted from verified Asr release process. Reuses persistent signer and frozen GeoNames, checks exact promoted SHA and gate jobs, builds, verifies APK cert/SDK, publishes only S25 prerelease, redownloads same published APK and checks digest/cert/size.
- Release workflow run 39014823058 began in_progress. Expected tag apk-icons-adc40017-20261017 and expected asset arihna-apk-icons.apk, BUT no release or asset may be called published or verified until run success and live release check. The signer will be verified against SHA256 1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa.
- Exact next action: watch run 39014823058 until completion and inspect logs; if complete/success, inspect release JSON and exact digest/size/signer verified in workflow; provide download link to user for Galaxy S25 physical test and keep previous stable until S25 PASS. If failure, investigate and repair release driver, never touch stable or rebuild the already published stable.
