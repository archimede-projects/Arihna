# Arihna — ChatGPT continuity state

> Persistent cross-chat handoff on dedicated branch `chat-context`. Never merge this branch into `main`.

## Mandatory protocol
1. Read this file first for every Arihna prompt.
2. Live-verify GitHub before claiming current SHA/PASS/release/APK state.
3. After every user prompt, update this file with request, actual work, verified identifiers, failures, current state and exact next action.
4. Never move `main` for continuity updates and never merge `chat-context` into `main`.
5. Never store secrets.
6. If GitHub write access is unavailable, say so.

## Current release state — 2026-10-21 (Planning PRERELEASE, NOT STABLE)
- GitHub live main **`4e1bbd7514977bea373a7940c4b17adfb63902d2`**, exact candidate commit for Planning, promoted only after completed/success full static/API28/API36 gate via `force=false`. Its parent is SPEC `8379f0ab35cb2b93cca320b8377b92f782a2e016`, grandparent original stable runtime `adc40017694af03723c7c57afb996306c0ddcd39`.
- **User-test prerelease** `planning-4e1bbd75-20261021`, release ID `419224971`, non-draft, prerelease=true, asset `arihna-planning.apk` ID `661575911`, **356755290 bytes**, SHA256 **`44f625d164aad43700b598a34ae61adfa567d1c56d0170160062104e6604a48c`**, signer cert SHA256 `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`, target the live main candidate. Fully re-downloaded and SHA/size/cert verified by successful release workflow.
- Direct S25 APK: https://github.com/archimede-projects/Arihna/releases/download/planning-4e1bbd75-20261021/arihna-planning.apk
- Release page: https://github.com/archimede-projects/Arihna/releases/tag/planning-4e1bbd75-20261021
- Full exact SHA gate `39553889831` completed/success: static/JVM/build `119069035244` success, full Android API28 `119069035635` success, Android API36 Planning and legacy `119069035557` success. Release driver workflow run `39554936021` job `119072403227` completed/success. Only user Galaxy S25 **physical PASS is still outstanding**.
- **Current physically validated STABLE remains previous `arihna-stable-adc40017-20261017`** APK SHA256 `d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`, size 356542262, unmodified and rollback-safe. Never confuse main new runtime or Planning prerelease with physically validated stable before user PASS.
- NEXT ACTION: hand verified prerelease download to user for Galaxy S25 real tests: Planning Home entry even without location; add/check/edit/delete task; create date-specific quiet reminder and test with 2–3min future; date-specific full ringtone alarm with independent volume Stop/Snooze and no unintended repeat; persist across reopening/reboot; check old adhan/custom alarms, Quran, prayer/location. If user says PASS, promote **these exact published APK bytes** into a new stable without rebuild and reverify SHA/signature and preserve old stable. If user reports failure, fix with sibling candidate from the SAME SPEC and full exact-SHA gate; do not rewrite old candidate.

## Historical physically validated baseline — 2026-10-17 (still current stable until Planning PASS)
- User explicitly confirmed **PASS tutto** after physically testing APK-size-optimized S25 prerelease on Galaxy S25. This is the physical validation for the exact prerelease asset `apk-icons-adc40017-20261017/arihna-apk-icons.apk`.
- As of October 17 live main was `adc40017694af03723c7c57afb996306c0ddcd39`; see current release state above for live October 21 main.
- **Current physically validated STABLE**: `arihna-stable-adc40017-20261017`, release id `415664924`, runtime `adc40017694af03723c7c57afb996306c0ddcd39`, asset `arihna.apk` id `649912545`, size `356542262` bytes, SHA-256 `d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`, signer SHA-256 `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`.
- Stable asset: https://github.com/archimede-projects/Arihna/releases/download/arihna-stable-adc40017-20261017/arihna.apk
- **Zero rebuild stable promotion:** stable copied physically validated prerelease bytes and redownloaded, SHA/firma/size checked; published stable and prerelease SHA identical.
- Prior stable `arihna-stable-3492add5-20261016` remains untouched/available as rollback; APK SHA `c5d05fb7d4ac9b386ef1bd105a87c677f9691a922361ec9430985f3346dac64e`.
- **NEXT ACTION:** maintain current stable; await user's next product objective. Do not start unrelated runtime modification spontaneously. For any future code change, create a new SPEC off current main, unique single-commit candidate direct child, run exact-SHA static/API28/API36 gate, then S25 prerelease and physical PASS before next stable. Keep continuity edits only on chat-context, never main.


## 2026-10-19 — Roadmap requested, no implementation authorized
- User prompt: "Prossimi obiettivi?" Request is planning/advice, not approval to modify runtime.
- Live verified main `adc40017694af03723c7c57afb996306c0ddcd39`, stable `arihna-stable-adc40017-20261017` non-prerelease release ID 415664924, APK 356542262 bytes, SHA256 `d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`. Prerelease still published and historical stable preserved. Open GitHub issues = 0. Source `PROJECT_SPEC.md` confirms offline-first, no paid APIs or Play Store publication, APK distribution by Releases, persistent signer and SPEC-first development.
- Suggested user-selectable roadmap (NOT started): (1) hardening/reliability of Adhan/personal alarms around reboot, DST/timezone changes and permission/battery scenarios; (2) local export/import backup of user settings, alarms, prayer corrections, bookmarks/history (no cloud or paid services); (3) Quran reading usability: search surah/verse, navigation/bookmarks and accessibility without altering sacred content; (4) additional APK optimization from current ~356.54 MB via audited R8/resource/code shrink only as separate SPEC; preserve all 604+604 offline Mushaf pages, GeoNames and audio; (5) polish UI/UX and accessibility including RTL/night mode where needed. Do not imply features are absent or malfunctioning; each is potential improvement to assess.
- Recommendation is to start with alarm/Adhan robustness; user decides. No SPEC, candidate, gate, release, main edit or stable replacement was performed this turn.
- Exact next action: await selection; then draft new SPEC on a branch off current main and obtain scope if required, proceed with unique candidate direct child of SPEC, full exact-SHA static/API28/API36 and prerelease/physical PASS before stable. Continue to update this file on chat-context after each turn.

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

## Historical stable baseline (2026-09-21 — superseded)
- Stable tag: `arihna-stable-977fbde9-20260921`
- Runtime: `977fbde989facc3b392f6af5a65449a5322d58c6`
- Release id: `392867609`
- Asset: `arihna.apk`
- Size: `386945577`
- SHA-256: `002d640a734eadf46b141b4489ae3c26d2803d9d49b4bacc772f7c678ab20813`
- Signer cert SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- User physically validated the page-jump runtime PASS on Galaxy S25.
- Historical statement (superseded): see **Current validated state — 2026-10-17** at top.

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

## Historical user request / actual work (2026-10-09)
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

## Historical next action (2026-10-16 — superseded)
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

## 2026-10-17 — APK-size icons S25 prerelease PUBLISHED and reverified (definitive turn close)
- Release driver workflow run 39014823058, job 117344657991, completed/success. Built exact main runtime adc40017694af03723c7c57afb996306c0ddcd39 with frozen GeoNames and pinned original persistent signer. Verified actual debug APK package/SDK/certificate and released as S25 prerelease.
- Released tag apk-icons-adc40017-20261017, release ID 415662217, draft=false, prerelease=true, target_commitish=adc40017694af03723c7c57afb996306c0ddcd39.
- Asset id 649899990, name arihna-apk-icons.apk, size 356542262 bytes; SHA-256 d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383.
- Verified in completed/success release workflow by redownloading published APK and comparing SHA256, size, publisher metadata, tag target, and apksigner signer certificate. Verified signer cert SHA-256 1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa.
- Direct artifact: https://github.com/archimede-projects/Arihna/releases/download/apk-icons-adc40017-20261017/arihna-apk-icons.apk
- Release page: https://github.com/archimede-projects/Arihna/releases/tag/apk-icons-adc40017-20261017
- The release build's size matches 356542262 byte preflight/gate exactly; signed APK hash is different from unsigned/ephemeral-signer preflight SHA; canonical published prerelease SHA is d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383.
- Historical stable arihna-stable-3492add5-20261016 SHA c5d05fb7d4ac9b386ef1bd105a87c677f9691a922361ec9430985f3346dac64e remains untouched and physically validated; do not replace until Galaxy S25 physical PASS. Updated main runtime is not a physically validated stable.
- Exact next step: User installs linked S25 prerelease and tests launch/screens/navigational rounded icons, Quran Hafs 604 pages, Warsh 604 pages, Imlai, Adhan, alarms/vintage 4-second sound, independent volumes, prayers, Qibla, GeoNames and permissions. Collect clear S25 PASS/FAIL. Only after user confirms physical PASS, publish stable from these exact verified prerelease bytes with no rebuild, reverify stable SHA256 = d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383, same signer, tag target and asset contents. Never touch existing stable bytes.

## 2026-10-17 — User PASS + definitive exact-bytes stable promotion (complete)
- New prompt: "Pass tutto". Interpreted as explicit real-world Galaxy S25 physical validation PASS for the released APK-size optimized prerelease `apk-icons-adc40017-20261017`. All prior tests user was asked to run passed.
- Live verified before release: main=`adc40017694af03723c7c57afb996306c0ddcd39`, prerelease id 415662217, target main, prerelease=true, asset `arihna-apk-icons.apk` id 649899990, digest `sha256:d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`, size 356542262 bytes; original stable release id 414962535 remained untouched.
- Reused prior proven stable-promotion GitHub Actions workflow template, adapted on independent release driver branch `driver/stable-adc40017-20261017` (driver created directly from runtime; not merged); workflow at `.github/workflows/stable-adc40017-20261017.yml`, workflow commit `62ee9ad834335b5246f69e644fbbceb5123a7b62`.
- Stable promotion Actions run `39015519830`, job `117346732192`: **completed/success**, including exact main SHA/source release metadata check, prerelease asset download, SHA256/size/signing verification, stable release creation from the downloaded asset **without rebuild**, stable redownload and full SHA256/size/signature/metadata re-verification. Previous stable digest+size remained unchanged, `PREVIOUS_STABLE_PRESERVED=PASS` in logs.
- Definitive stable tag: `arihna-stable-adc40017-20261017`, release ID `415664924`, draft=false, prerelease=false, target `adc40017694af03723c7c57afb996306c0ddcd39`.
- Definitive stable asset: `arihna.apk`, asset ID `649912545`, size `356542262` bytes, digest `sha256:d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`; signer cert SHA256 `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`. Same exact SHA, size and signer as physically validated prerelease.
- Stable download: https://github.com/archimede-projects/Arihna/releases/download/arihna-stable-adc40017-20261017/arihna.apk
- Stable release: https://github.com/archimede-projects/Arihna/releases/tag/arihna-stable-adc40017-20261017
- New 356542262 vs prior stable 387885070: APK saving **31342808 bytes (~31.34 decimal MB, 8.08%)**, DEX saving 31336352 bytes, 1227 protected offline assets byte-identical to validated prior stable preflight. 604 Hafs/604 Warsh, GeoNames, Quran/Imlai, prayers, Adhan, vintage audio and all validated UI preserved.
- Historical stable `arihna-stable-3492add5-20261016` (digest `c5d05fb7d4ac9b386ef1bd105a87c677f9691a922361ec9430985f3346dac64e`, size 387885070) **unchanged** as rollback.
- No candidate, SPEC or main write needed for promotion; continuity updated only here on branch chat-context. No current outstanding gate or publishing task.
- Exact next action: project stays on validated stable `adc40017`; await user-directed next objective, using SPEC-first workflow and existing rollback rules.

## 2026-10-20 — Detailed explanation objective 1 (alarm and Adhan reliability)
- User asked: "Spiegami bene obiettivo 1", meaning explain the previously suggested alarm/Adhan robustness goal, **not** authorization to implement it.
- Checked live GitHub `main` still `adc40017694af03723c7c57afb996306c0ddcd39`; fully read persisted continuity. Current physically validated stable unchanged: `arihna-stable-adc40017-20261017` APK SHA-256 `d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`.
- Inspected current source on main: `AlarmReceivers.kt`, `AlarmReconciliationTrigger.kt`, `AlarmReconciler.kt`, `AndroidAlarmPlatform.kt`, `AlarmPlatformScheduler.kt`, `AlarmNotifications.kt`, `AlarmOccurrenceHandler.kt`, `AlarmOccurrenceResolver.kt`, `AlarmSnooze.kt`, `AlarmRingingService.kt`, `AlarmRingingActivity.kt`, `AlarmsViewModel.kt`, `AppContainer.kt`, `AndroidManifest.xml`; enumerated extensive existing Android/JVM alarm tests.
- Verified already implemented: system event receivers for BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, exact-alarm permission change, MY_PACKAGE_REPLACED trigger alarm reconciliation; exact `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP,...)` with `canScheduleExactAlarms()` check; persistent rules; stale-event tokens/revision validation; Android notification permission check, HIGH importance channels and foreground playback; fullscreen/overlay/ringing controls; prayer-specific and personal-alarm-specific volume; custom alarm local-time DST gap/overlap deterministic behavior; snooze 5min.
- Proposed objectives for hardening, **not confirmed defects**: test real Samsung S25 background/doze/locked device overnight, reboot/clock/timezone/DST/location/prayer offset changes, exact alarm/notification/fullscreen permissions revoked/regranted, app update, parallel alarms, delivery failure and durable reschedule; add actionable readiness indicators/diagnostics and safe recovery only for demonstrable gaps. Review async receiver lifetime/exception recovery and startForegroundService failure paths. Never falsely guarantee behavior after a user force-stops the app, blocks permissions or OEM aggressively restricts background tasks.
- Android official documentation consulted: `https://developer.android.com/develop/background-work/services/alarms?hl=it` and `https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start`; current exact-alarm/background FGS rules confirm battery/policy caveats and exceptions for user-requested exact alarms.
- Scope excludes changing approved Adhan Makkah x2, 4-second vintage ringtone, custom alarm stop/snooze, independent volumes, prayer calculation methods/offset behavior, UI/Quran/offline content. No new SPEC, branch, code, main change, gate or release started.
- Proposed process if user approves: define SPEC from current main; build baseline diagnostic matrix before editing; address verified weaknesses only using one direct-child candidate per SPEC; full exact-SHA static/API28/API36 gates; S25 prerelease manual validation on locked phone/overnight/reboot/permissions; promote non-forced and only stable after explicit user PASS. Preserve old stable.
- Exact next action: finish explanatory answer, await the user's decision whether to start this goal and choose priority if desired; always maintain chat-context only.

## 2026-10-20 — One-off calendar date reminder / dated alarm question
- Prompt: "Se volessi mettere un promemoria per il 23 dicembre come faccio? O una sveglia in quella data?" in context of Arihna goal 1. User is asking whether a one-off **specific future calendar date** reminder or full ringing alarm exists; not a command to schedule a reminder via ChatGPT or an approval to implement a new feature.
- Live GitHub verification: main `adc40017694af03723c7c57afb996306c0ddcd39`, physically validated stable `arihna-stable-adc40017-20261017`, non-prerelease with asset SHA256 `d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`.
- Inspected main sources: `AlarmsPlaceholderScreen.kt` UI editor allows TimePicker, name, weekdays, sound, volume but **does not have date chooser**; empty weekdays displays "Singola". `AlarmModels.kt` `AlarmDefinition.Custom` stores `label`, `LocalTime`, `Set<DayOfWeek>` only — no LocalDate. `AlarmOccurrenceResolver.resolveCustom` when weekdays empty chooses next trigger today/tomorrow, otherwise next matching weekday in coming 7 days. `AlarmsViewModel.saveCustom` has no calendar date argument. Thus cannot schedule for fixed 2026-12-23 in current Arihna; misinterpreting "Singola" as future dated alarm is unsafe. A Wednesday weekly repeat would fire before Dec 23, not a valid workaround.
- Response: explain limitation clearly and advise using Samsung Reminder / Calendar on phone now for exact date. Product idea: add two distinct one-off date + time modes: (1) quiet notification reminder with message, (2) full-screen ringing dated alarm with independent ringtone/volume and Stop/Snooze. Preserve existing existing next-occurrence "Singola", weekly alarms, prayer/Adhan functionality; add date picker and persist absolute LocalDate and LocalTime (with explicit timezone/DST policy); automatically close/disarm after delivery and avoid duplicate delivery. Note notification permissions and Android background restrictions. Product should use separate SPEC-first milestone if user opts in; no implementation or scheduling done.
- Current status: planning/question only; no SPEC, candidate, main modification, new APK, automation or release. Current physically validated stable unchanged.
- Next action: ask whether user wants support for specific-date reminders/alarms added to Arihna; if approved, create SPEC off current main, single direct-child candidate, API28/API36/static gates, prerelease S25, user PASS, identical-bytes stable. Continue writing continuity exclusively to chat-context.

## 2026-10-21 — Follow-up "?" x2: answer exact-date reminder/sveglia question promptly
- User sent "?" twice after asking how to create a reminder or alarm for December 23 in Arihna; the previous response had been delayed. Respond directly, avoid unnecessary re-questions.
- Live verified GitHub main `adc40017694af03723c7c57afb996306c0ddcd39` and still-validated stable `arihna-stable-adc40017-20261017`, APK SHA-256 `d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`.
- Independently reconfirmed on main `AlarmModels.kt`, `AlarmOccurrenceResolver.kt`, `AlarmsPlaceholderScreen.kt`: personal alarm has `LocalTime` + optional `Set<DayOfWeek>`, UI has TimePicker/day selectors but no date picker, `Singola` triggers once at next occurrence today/tomorrow, not a specified future LocalDate. Cannot program exact 2026-12-23 date in current Arihna. Using a recurring Wednesday is NOT a substitute for 2026-12-23.
- In visible user answer, explained distinction and workaround via Samsung Reminder/Calendar for exact date. Proposed future separate single-date note/notification mode and single-date full ringing alarm with independently selected sound/volume, Stop/Snooze, automatically disarm once delivered. Clarified neither feature is yet implemented.
- No automation/reminder was scheduled; no user-requested time or text to schedule. No app implementation authorized; no changes to main, current stable APK or release. Continuity update only on chat-context.
- Next action: user decides whether to approve dated reminders/date-specific alarms as new milestone; if approved, SPEC-first off current main, single-direct-child candidate, exact-SHA gates API28+API36, prerelease S25 physical validation, stable promotion from exact validated bytes.

## 2026-10-21 — User proposes dedicated Planning screen; SPEC CREATED (runtime untouched)
- The immediately preceding user explicitly approved two features: one-off reminder notification and one-off date-specific ringing alarm. This turn user asked: "Oppure è meglio aggiungere una nuova schermata dedicata al planning, ad esempio le cose da fare, promemoria." Interpreted as a design pivot to a unified Planning screen rather than scattering features inside the existing Alarms editor.
- Inspected live main `adc40017694af03723c7c57afb996306c0ddcd39`, `ArihnaNavHost.kt` + `TopLevelNavigation.kt`: there are SIX existing bottom destinations (Home, Orari, Qibla, Corano, Sveglie, Impostazioni). Seventh persistent icon would overcrowd them. Decision: add a new Planning route accessible via Home shortcut/card, retain existing bottom nav and legacy alarms.
- Proposed Planning groups: Da fare (basic local checkable task, optional due date); Promemoria (explicit LocalDate+LocalTime, quiet notification containing title/message, no ringtone/fullscreen); Sveglie con data (explicit LocalDate+LocalTime, existing ringtone/independent volume, Stop/Snooze and one-off behavior). Group by Oggi, Prossimi, Completati. Preserve old Singola next-occurrence and weekday alarms and every Adhan/prayer/audio/Quran/offline behavior.
- **Engineering action actually done this turn:** Created SPEC branch `spec/planning-reminders-dated-20261021` from unchanged main. In exactly one documentation-only commit `8379f0ab35cb2b93cca320b8377b92f782a2e016`, added `docs/specs/planning-reminders-dated-20261021.md` (7153 chars, blob aa8bef12e8e1359dcc19dfae54012999488bc198) and updated root `PROJECT_SPEC.md`. Live GitHub compare verifies ahead_by=1, behind_by=0, exactly those 2 files. No runtime code or APK produced.
- SPEC includes date/time data persistence, DST gap/overlap/timezone rules, idempotent exact scheduling/reboot/permission policies, quiet notification separation from ringing service, no seventh bottom tab, regression matrix and SPEC-first candidate release discipline.
- At end of turn live recheck: main still `adc40017694af03723c7c57afb996306c0ddcd39`, physically validated stable `arihna-stable-adc40017-20261017` asset 356542262 bytes digest `sha256:d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`; unchanged. `chat-context` only holds continuity, never merge to main.
- **No candidate, static/API28/API36 gate, new prerelease or stable for Planning yet.** Do not say Planning is implemented. The next engineering step, within the user's approved feature direction, is an isolated implementation/preflight directly from this SPEC, followed by one single-commit candidate direct child of SPEC and exact-SHA static/API28/API36 gates before promotion; user validation on S25 required. Do not ask user to repeat previously approved scope.

## 2026-10-21 — User "Procedi": Planning implementation in gate (intermediate verified)
- User explicitly authorized implementing the previously SPECed new Planning screen with offline tasks, date/time quiet reminders and dated full ringing alarms.
- Followed `spec/planning-reminders-dated-20261021` commit `8379f0ab35cb2b93cca320b8377b92f782a2e016` (one doc-only commit from original main `adc40017694af03723c7c57afb996306c0ddcd39`).
- Isolated staging/preflight branch `driver/planning-implementation-20261021` had staging commits `53f537ee734e0f1afba660107f04f2e08d45aa83` and `26bc2491987ac522c67984fc20fffa0208f97ce3`; staging contains build-only preflight workflow, not candidate. Preflight run v1 `39552969635` job `119066112036` **completed/success**. Preflight v2 `39553464590` job `119067664935` **completed/success**, additionally compiling new Android instrumentation tests.
- Implemented isolated `app/.../feature/planning/PlanningModels.kt` for typed items TASK/REMINDER/ALARM, versioned DataStore persistence independent of legacy alarm codecs, date/time and DST resolution; `PlanningScheduler.kt` scheduling exact RTC_WAKEUP/allow-while-idle with confirmation and quiet channel or shared `AlarmRingingService` with ringtone/volume; `PlanningScreen.kt` for listing, creating, editing, checking, deleting, calendar/time picking and selecting classic/Adhan/Android/silent ringtone; Home shortcut accessible even without a location; NavHost route; app startup and system-event reconciliation; manifest receiver and snooze integration. JVM `PlanningModelsTest.kt` and Android `PlanningAndroidTest.kt` added.
- Candidate branch `candidate/planning-v1-20261021` at **`4e1bbd7514977bea373a7940c4b17adfb63902d2`**; exact single commit directly from SPEC `8379f0ab...`, 12 runtime/test files, no generated offline assets touched. GitHub compare confirms ahead_by=1 behind_by=0.
- Full exact SHA gate driver `driver/planning-v1-gate-20261021` workflow commit `7c61556b9a42a22dd15dc5ab3a3e050823602b95`, path `.github/workflows/planning-exact-sha-gate.yml`, run **`39553889831`** launched and in progress (static build job `119069035244`, API28 full `119069035635`, API36 planning and legacy `119069035557`). No gate PASS can be claimed before all 3 jobs and run completed/success. Branch main and physically validated stable remain unchanged until gate green. Gate verifies immutable offline assets.
- Prepared but NOT launched release workflow blob `f44f3049a211a03c8a2346b14ef36d9fb1aa4c7f` adapted from prior signed-release process; tag would be `planning-4e1bbd75-20261021`, APK `arihna-planning.apk`, requires completed/success gate jobs, runtime promoted to main, persistent debug signer and redownload sha/cert verification. Workflow must not be created/pushed until full gate PASS and non-forced main FF.
- Exact next action: await gate run `39553889831`, inspect statuses and failures. If any fail, do NOT promote; fix on staging, recreate a **sibling** candidate direct child of same SPEC and repeat full exact-SHA gate. If all PASS, live verify main unchanged and non-force fast-forward to candidate, launch prerelease only with signed/redownload checks, ask user for real Galaxy S25 validation before any stable promotion. Preserve previous stable `arihna-stable-adc40017-20261017` forever as rollback.

## 2026-10-21 — Planning gate complete, main promoted, signed S25 prerelease PUBLISHED (definitive)
- New user authorization "Procedi": implement SPEC-led Planning: tasks, quiet date-specific reminder notifications and one-off dated full-screen ringing alarms.
- SPEC branch `spec/planning-reminders-dated-20261021`; SPEC commit `8379f0ab35cb2b93cca320b8377b92f782a2e016`; doc `docs/specs/planning-reminders-dated-20261021.md` plus PROJECT_SPEC update, parent `adc40017694af03723c7c57afb996306c0ddcd39`.
- Isolated implementation preflight staging driver `driver/planning-implementation-20261021`, latest `26bc2491987ac522c67984fc20fffa0208f97ce3`, run v1 `39552969635` and v2 `39553464590`, **both completed/success**, JVM+androidTest compile+APK generation. Stage APK 356755290 bytes.
- Candidate `candidate/planning-v1-20261021` SHA `4e1bbd7514977bea373a7940c4b17adfb63902d2`, one commit directly from SPEC, exactly 12 code/test files, no offline resources altered; 3 new production feature files, 2 test files, 7 integration code/manifest files. Legacy alarm rule codecs unchanged; existing six bottom destinations preserved; Home opens Planning without configuring location.
- Full gate driver `driver/planning-v1-gate-20261021`, workflow commit `7c61556b9a42a22dd15dc5ab3a3e050823602b95`, run `39553889831`: **completed/success**, all jobs green: static/JVM/AndroidTest-compile/build `119069035244`, API28 complete suite `119069035635`, API36 Planning/legacy tests+permissions `119069035557`. Build size 356755290 (prior stable 356542262, +213028 bytes).
- Before promotion rechecked main `adc40017694af03723c7c57afb996306c0ddcd39`, stable published SHA and gate success. Promoted main via `update_ref(force=false, expected_sha=old)` to `4e1bbd7514977bea373a7940c4b17adfb63902d2`; live reverified GitHub branch/main equals promoted candidate.
- Release driver `driver/planning-v1-release-20261021`, workflow commit `8340b6c57c42bfe0b93f4ddc59aa3dfe06cbfe14`, workflow `.github/workflows/planning-v1-release.yml`, run `39554936021`, job `119072403227` **completed/success**. Verified green gate exact job IDs, runtime/main lineage, fixed GeoNames, original persistent signing key; built signed candidate APK, verified apksigner, SDK/package; published prerelease, re-downloaded and confirmed identical digest/size/certificate/tag target.
- Published S25 prerelease `planning-4e1bbd75-20261021`, release id `419224971`, draft=false, prerelease=true, target `4e1bbd7514977bea373a7940c4b17adfb63902d2`; asset `arihna-planning.apk` ID `661575911`, 356755290 bytes; verified SHA256 `44f625d164aad43700b598a34ae61adfa567d1c56d0170160062104e6604a48c`; verified signer SHA256 `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`.
- S25 APK https://github.com/archimede-projects/Arihna/releases/download/planning-4e1bbd75-20261021/arihna-planning.apk
- Prior physically validated stable `arihna-stable-adc40017-20261017` remains unchanged (356542262 bytes, SHA256 `d9b00e9e85daac4ef976e281aecc08c82b2d62823af12db81b89b191e364b383`). New Planning release is NOT YET stable, not physically verified.
- Hand-off test plan: install APK over stable on Galaxy S25; open Home → Planning even without location; create/edit/complete/delete task; schedule 2–3 min ahead reminder and verify quiet notification, no ringing; schedule 2–3 min ahead dated alarm with vintage/classic/Android ringtone, volume, full ring, STOP/SNOOZE; save 23 December 2026 and verify label/date; force close/reopen normal via swipe and reboot test; legacy Adhan, volume independence, prayer settings, Quran, location; send PASS tutto or precise defect details.
- Potential polish not yet addressed by this v1: Planning reminder rows automatically become delivered/completed after actual notification; no separate manual “mark reminder complete” button yet. Do not assert one exists; add only through a later SPEC/sibling if necessary. Review Android background policies and permission warnings on S25.
- Exact next step: user physical S25 tests. If PASS, create definitive stable release from exact published APK bytes and no rebuild. If FAIL, same-SPEC sibling candidate, full gates and new S25 prerelease. Continue every-prompt continuity writes ONLY to chat-context.
