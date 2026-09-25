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
- Candidate = exactly one commit, direct child of SPEC; failed replacements must be sibling candidates from the same SPEC.
- Full exact-SHA gates include API28 and API36.
- Promote `main` only after green gates, via non-forced fast-forward.
- Changed user-facing runtimes go to S25 prerelease first.
- Use persistent Arihna signer; redownload published APK and verify SHA-256 + signer before handoff/stable claims.
- Preserve unrelated features unless SPEC explicitly changes them.

## Current main / stable baseline
- Live-verified 2026-09-21: `main = 977fbde989facc3b392f6af5a65449a5322d58c6`
- Message: `feat(quran): add direct page jump`
- User physically validated the complete S25 page-jump checklist PASS.
- New stable tag: `arihna-stable-977fbde9-20260921`
- Release id: `392867609`
- Title: `Arihna — Stable — S25 validated`
- Target: `977fbde989facc3b392f6af5a65449a5322d58c6`
- `draft=false`, `prerelease=false`
- Published: `2026-09-21T10:29:24Z`
- Asset id `578804583`, name `arihna.apk`
- Size: `386945577` bytes
- SHA-256: `002d640a734eadf46b141b4489ae3c26d2803d9d49b4bacc772f7c678ab20813`
- Signer cert SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- Download: https://github.com/archimede-projects/Arihna/releases/download/arihna-stable-977fbde9-20260921/arihna.apk
- This stable APK is byte-identical to the prerelease physically validated on Galaxy S25.

### Stable publication evidence
- Driver: `driver/stable-977fbde9-release-20260921`
- Workflow commit: `cad34bf53807eaa2e53c1ea8869b922bf7167781`
- Run: `35588967090` — completed/success
- Job: `106298770917` — completed/success
- Workflow verified runtime/gate/source prerelease, downloaded and verified physically validated APK bytes, published stable, redownloaded stable asset and reverified size/SHA/signer/metadata.

## Quran page-jump cycle — closed
- SPEC: `e680659976010d8e8bfe2e62da862567230b5554`
- Candidate/runtime: `977fbde989facc3b392f6af5a65449a5322d58c6`
- Exact-SHA gate `35325258945`: success
  - static/build `105536706783`
  - API28 `105536706741`
  - API36 `105536706685`
- S25 prerelease `quran-page-jump-977fbde9-20260918` was physically validated PASS by the user.
- Stable publication is now complete.

## Product definition: القرآن بالرسم الإملائي
User clarified that the former “Facile” concept means exactly:

`القرآن بالرسم الإملائي`

It is a **writing/orthography presentation**, not a separate reading mode and not simplified/paraphrased Quran content.

Required product semantics:
- preserve Quran wording, ayah identity/order and semantics;
- use an authoritative imla'i orthography source;
- retain normal pageable/swipeable Quran navigation, index, direct page jump, bookmarks, history, fullscreen and persisted page state;
- Hafs/Tajwid/Warsh remain reading/riwaya identities;
- do not apply imla'i silently to Tajwid or Warsh without separately verified support;
- do not invent ad-hoc text simplification.

## Imla'i SPEC — created, no runtime change yet
- Branch: `spec/quran-imlai-writing-20260921`
- SPEC commit: `3da807cef157d57a54bc426295eb54f66830158d`
- Parent: `977fbde989facc3b392f6af5a65449a5322d58c6`
- File: `docs/specs/quran-imlai-writing-20260921.md`
- Message: `spec(quran): define imlai writing presentation`
- This commit is documentation-only and is NOT on `main`.
- SPEC requires authoritative source provenance/license, 114 surahs / 6,236 Hafs ayat structural integrity, deterministic surah:ayah mapping, legacy EASY migration safety, normal pageable navigation, regression coverage, exact-SHA API28/API36 gates and S25 prerelease validation.

## Latest user request / actual work
2026-09-25 user asked: `Ci sei?`

Actually done:
- Read continuity from `chat-context`.
- Live-verified `main = 977fbde989facc3b392f6af5a65449a5322d58c6`.
- Live-verified stable release `arihna-stable-977fbde9-20260921` remains published, non-prerelease, targeting the validated runtime, with asset `arihna.apk` SHA-256 `002d640a734eadf46b141b4489ae3c26d2803d9d49b4bacc772f7c678ab20813`.
- Live-verified SPEC branch `spec/quran-imlai-writing-20260921` at commit `3da807cef157d57a54bc426295eb54f66830158d`, message `spec(quran): define imlai writing presentation`.
- Live-read `docs/specs/quran-imlai-writing-20260921.md`; it contains the pinned product definition and source-integrity/navigation requirements.
- No runtime code, `main`, release, CI or APK state changed this turn.

## Exact next action
- Steps 1 and 2 requested earlier are both complete.
- Next engineering action is source discovery/verification for an authoritative Quran corpus in `الرسم الإملائي`, including provenance and redistribution/license terms.
- Only after source verification, create candidate 1 as exactly one commit direct child of SPEC `3da807c...`, then exact-SHA API28/API36 gates, promotion if green, signed S25 prerelease, post-publish verification, and physical user validation.
