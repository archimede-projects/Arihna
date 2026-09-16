# Arihna — ChatGPT continuity state

> Persistent cross-chat handoff file on dedicated branch `chat-context`. It must never be merged into `main`.

## Mandatory protocol

1. At the start of every Arihna chat, read this file first from `archimede-projects/Arihna`, branch `chat-context`.
2. Treat it as handoff context, but verify live GitHub state before claiming current SHAs, PASS, releases or APK verification.
3. After every user prompt, replace/update this file on `chat-context` before the final answer. Record: latest request, what was actually done, verified identifiers/results, failures/problems, current state and exact next action.
4. Never move `main` merely for continuity updates. Never merge `chat-context` into `main`.
5. Never store passwords, tokens, private keys or sensitive data here.
6. If write access is unavailable, say so explicitly instead of pretending this file was updated.

## User working style

- Language: Italian.
- Execute directly and keep status concise.
- During long GitHub/CI work, give brief progress updates while working.
- Do not claim PASS, release readiness or APK verification without real evidence.
- User performs final physical Android validation on a Galaxy S25.
- When context is sufficient, proceed without unnecessary questions.

## Engineering / release discipline

- SPEC-first.
- Runtime candidate = exactly one commit, direct child of its SPEC.
- Failed replacement candidates must be siblings from the same SPEC, not descendants of a failed candidate.
- Full exact-SHA gates include API 28 and API 36.
- Promote `main` only after required gates are green, via non-forced fast-forward.
- Use the persistent Arihna signer.
- S25 validation builds are GitHub prereleases.
- Before handoff: redownload published APK and verify SHA-256 and signer.
- Preserve unrelated features unless SPEC explicitly changes them.

## Repository

- Repo: `archimede-projects/Arihna`
- Runtime branch: `main`
- Continuity branch: `chat-context`
- Continuity file: `docs/CHATGPT_CONTINUITY.md`

## Current live-verified runtime/release state

Rechecked on GitHub 2026-09-16 in the current chat:

- `main`: `c07663fcc61101a75e2b78ba550ccc6f60e38b35`
- Runtime message: `feat(home): expand daily inspiration variety compact preview`
- Parent SPEC: `08eb114b6eec46ceee2cef65451302ac03b7e0fa`
- SPEC message: `spec(home): expand daily inspiration variety`
- Runtime is a single direct child of the SPEC.
- Candidate identity: candidate 2; exact-SHA gate workflow explicitly checks out `c07663fcc61101a75e2b78ba550ccc6f60e38b35` and verifies parent/lineage against the SPEC.
- Exact-SHA gate run `35083033004`: live status `completed`, conclusion `success`.
  - static/build job `104751281456`: `success`
  - API28 job `104751281445`: `success`
  - API36 job `104751281189`: `success`
- Continuity from the completed release flow records API28 = 94 tests, 0 failures/errors/skips and API36 = 11 tests, 0 failures/errors/skips. In this chat, raw job logs were not available through the connector, so those counts were not independently re-read; the live job conclusions and exact-candidate workflow definition were reverified.
- Release workflow run `35107380266`: live status `completed`, conclusion `success`.
- Release job `104831994168`: `success`; the live job step list shows successful `Verify APK package SDK assets signer and digest`, `Publish verified prerelease`, and `Redownload and verify published APK and release metadata` steps.
- Release tag: `home-daily-inspiration-variety-c07663fc-20260916`
- Release targets runtime `c07663fcc61101a75e2b78ba550ccc6f60e38b35`, `draft=false`, `prerelease=true`.
- Exactly one release asset:
  - `arihna-home-daily-inspiration-variety.apk`
  - bytes: `386929193`
  - live GitHub asset digest: `sha256:9043369fe0e35f9c7059345169294d3c756d2ff8e6e6df861295966f14f58c3f`
  - URL: `https://github.com/archimede-projects/Arihna/releases/download/home-daily-inspiration-variety-c07663fc-20260916/arihna-home-daily-inspiration-variety.apk`
- Persistent signer certificate SHA-256 used/verified by the successful release workflow: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- Package: `com.archimedeprojects.arihna`; minSdk 28, targetSdk 37, compileSdk 37.
- Physical Galaxy S25 validation remains user-side until reported.

## Latest shipped feature

Home daily inspiration / positive daily action:

- Curated inspiration corpus expanded to at least 60 unique days.
- Positive daily actions expanded to at least 30.
- Home preview limited to compact two-line text with ellipsis.
- Full content remains available in the detail dialog.
- Narrow scope preserved Prayer, Location, Qibla, alarm, Quran and permission behavior.

## Important preserved project history

Previously integrated work includes Quran/Hafs/Tajwid/Warsh, Home/Hijri, alarms, location, prayer and Qibla. Inspect GitHub history for exact implementation details; do not casually rewrite those areas during narrow tasks.

Persistent signer source historically used:
- branch `location-step5-device-test`
- commit `ce23a7f78695be95c7f8dfd2bcedbe22544da94c`
- keystore SHA-256 `bc9057f26ad6de7efb70a5df06effb1ed259f1d015c3f062d0f757b3f0983b72`

Frozen GeoNames asset historically used:
- release `settings-s25-premium-3f28b6f0-20260906`
- APK `arihna-settings-s25-premium.apk`
- `cities.db` SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`

## Latest user request / what was done

2026-09-16: User opened a continuation chat and required reading this file first, live GitHub verification, then continuation without restarting or asking for a recap. User also made permanent the rule that this file must be updated after every prompt.

Actually done this turn:
- Opened the repository with GitHub write access confirmed.
- Read this continuity file in full from `chat-context`.
- Reverified live `main`, SPEC/runtime parentage, candidate-2 exact-SHA gate run/jobs, release workflow/job, release tag/target/prerelease state, single APK asset, size and GitHub SHA-256 digest.
- Reverified from the live release workflow definition + successful step metadata that persistent signer verification and published-APK redownload verification were part of the successful release job.
- Attempt to fetch raw API28 job logs through the connector was rejected by the connector endpoint policy; therefore test counts were retained only as prior verified continuity data, not claimed as freshly re-read.
- No app/runtime code or `main` changes were made.

## Current state / exact next action

- Project remains at the verified daily-inspiration candidate-2 prerelease above.
- No unfinished code change is pending from the previous chat.
- Exact next action: continue from this runtime/release baseline with the user's next Arihna request, or record the Galaxy S25 physical validation result when the user supplies it. Do not alter `main` merely because a new chat started.
