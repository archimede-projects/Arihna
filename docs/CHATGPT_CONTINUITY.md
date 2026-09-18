# Arihna — ChatGPT continuity state

> Persistent cross-chat handoff file on dedicated branch `chat-context`. It must never be merged into `main`.

## Mandatory protocol

1. At the start of every Arihna chat, read this file first from `archimede-projects/Arihna`, branch `chat-context`.
2. Verify live GitHub state before claiming current SHAs, PASS, releases or APK verification.
3. After every user prompt, update/replace this file on `chat-context` before the final answer with: latest request, what was actually done, verified identifiers/results, failures/problems, current state and exact next action.
4. Never move `main` for continuity updates. Never merge `chat-context` into `main`.
5. Never store passwords, tokens, private keys or sensitive data here.
6. If write access is unavailable, say so explicitly.

## User working style

- Language: Italian; operational and concise.
- Proceed directly when context is sufficient; avoid unnecessary questions.
- During long GitHub/CI work, give brief progress updates.
- Never claim PASS/release/APK verification without evidence.
- Final physical Android validation is performed by the user on a Galaxy S25.
- Be transparent about incomplete or failed steps.

## Engineering / release discipline

- SPEC-first for runtime/code changes.
- Candidate = exactly one commit, direct child of its SPEC; replacement candidates after failure must be siblings from the same SPEC.
- Full exact-SHA gates include API28 and API36.
- Promote `main` only after required gates are green, via non-forced fast-forward.
- Use the persistent Arihna signer for user APKs.
- User-facing changed runtimes go to an S25 validation prerelease first.
- Before APK handoff, redownload the published APK and verify SHA-256 + signer.
- Preserve unrelated features unless the SPEC explicitly changes them.

## Prior stable baseline

- Stable tag: `arihna-stable-be4af47d-20260917`
- Runtime: `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- APK `arihna.apk`, size `386929193` bytes
- SHA-256 `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`
- Signer cert SHA-256 `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- User physically validated the broader Galaxy S25 smoke and clean stable install/open.
- This remains the last physically validated stable user baseline until the new page-jump runtime is validated.

## Post-stable Quran hardening — completed

- SPEC `50471062331499be5364cbebbfe4dc6f9b084610`
- test-only candidate/main lineage `4588088ae459b7ac8811fed359e83192bb0681bd`
- gate `35210107445`: completed/success
- Added stress regressions for long histories, repeated Hafs/Tajwid/Warsh switching, bookmark separation, fullscreen cycles and state restoration.
- No production behavior changed in that cycle.

## Quran direct page jump — current changed runtime

User request on 2026-09-18: `Procedi` after agreeing to move to the first user-visible post-hardening Quran improvement.

### SPEC
- branch `spec/quran-direct-page-jump-20260918`
- SHA `e680659976010d8e8bfe2e62da862567230b5554`
- parent/base `4588088ae459b7ac8811fed359e83192bb0681bd`
- goal: add direct navigation to exact Mushaf page 1..604 from the Quran explorer, preserving all existing navigation and unrelated app features.

### Candidate / main
- branch `candidate/quran-direct-page-jump-v1-20260918`
- candidate SHA `977fbde989facc3b392f6af5a65449a5322d58c6`
- direct child of SPEC; exactly one candidate commit
- changed candidate files only:
  1. `app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt`
  2. `app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranFullscreenAndroidTest.kt`
- UI: Quran index now has a bilingual `Vai a pagina` / direct-page field with numeric keyboard, valid domain 1..604, explicit enabled `Vai` action, stable tags `quran-page-jump-input` and `quran-page-jump-go`.
- A valid selection closes the explorer and routes to the exact page using the existing reader state flow.
- Regression `directPageJumpWorksAcrossHafsTajwidAndWarsh` jumps to page 321 in Hafs, 77 in Tajwid and 604 in Warsh and verifies persisted page indices.
- No Quran corpus/artwork/Tajwid-rule/bookmark-schema/fullscreen/Prayer/Location/Qibla/alarm/Home/Settings/permission changes.

### Exact-SHA gate
- driver `driver/quran-direct-page-jump-v1-gate-20260918`
- driver commit `e130718e3ca444dcae687a94423b9e5b9bfeb8ee`
- run `35325258945`: completed/success
- static/build job `105536706783`: success
- API28 full-suite job `105536706741`: success
- API36 Quran + hardening + Tajwid + permission-matrix job `105536706685`: success
- Do not invent raw test counts beyond configured gate invariants.

### Promotion
- Before promotion, live `main` was rechecked at `4588088...`.
- Promoted with `force=false`.
- Live recheck confirms current `main = 977fbde989facc3b392f6af5a65449a5322d58c6`.

### S25 validation prerelease
- release driver `driver/quran-direct-page-jump-v1-release-20260918`
- release driver commit `d3ebba79c6d2c28db9fb0b75df7affd6aa6e8390`
- release workflow run `35326339850`, job `105540154182`: completed/success
- workflow verified promoted runtime + definitive gate, restored frozen GeoNames and persistent signer, built exact runtime, verified APK, published prerelease, redownloaded it and reverified published SHA/size/signer/metadata.
- tag `quran-page-jump-977fbde9-20260918`
- title `Arihna — Quran page jump — S25 validation`
- release id `391334028`
- target `977fbde989facc3b392f6af5a65449a5322d58c6`
- `draft=false`, `prerelease=true`
- single asset `arihna-quran-page-jump.apk`
- asset id `572225953`
- size `386945577` bytes
- SHA-256 `002d640a734eadf46b141b4489ae3c26d2803d9d49b4bacc772f7c678ab20813`
- signer certificate expected/verified: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- download URL: `https://github.com/archimede-projects/Arihna/releases/download/quran-page-jump-977fbde9-20260918/arihna-quran-page-jump.apk`
- release page: `https://github.com/archimede-projects/Arihna/releases/tag/quran-page-jump-977fbde9-20260918`

## Current state / exact next action

- Repository/CI status: no known blocker; direct-page runtime is promoted and exact-SHA gate is green.
- User-facing status: new runtime is **not yet physically validated**. The prior stable APK remains the last physically validated stable baseline.
- Exact next action: user installs the `quran-page-jump-977fbde9-20260918` prerelease on Galaxy S25 and validates the new Quran index direct-page control in Hafs, Tajwid and Warsh, including edge pages 1 and 604.
- If physical validation is clean, publish/advance the new runtime to stable without unrelated feature changes, with the usual post-publication APK redownload/signature verification.
- If physical validation fails, record exact mode/page/screenshot where possible and create a sibling corrective candidate from the same SPEC; do not blindly amend the failed candidate.
