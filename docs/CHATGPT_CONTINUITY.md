# Arihna — ChatGPT continuity state

> Persistent cross-chat handoff file. This file lives only on branch `chat-context` and must not be merged into `main` as part of app runtime work.

## Mandatory protocol for every future chat

1. At the start of every new Arihna chat, read this file from repository `archimede-projects/Arihna`, branch `chat-context`, path `docs/CHATGPT_CONTINUITY.md` before doing project work.
2. Treat this file as continuity/handoff context, but verify live GitHub state before making factual claims about current branches, runs, releases, SHAs, artifacts or test results.
3. After **every user prompt**, update this same file on branch `chat-context` before the final answer. Keep it concise and current. Record the latest user request, what was actually done, verified identifiers/results, and the exact next action. Do not store passwords, tokens, private keys, personal secrets or sensitive data.
4. Never modify `main` merely to update chat context. Context commits belong only on `chat-context`.
5. If GitHub write access is unavailable in a future chat, state that the continuity file could not be updated in that turn; do not pretend it was updated.
6. When the user opens a fresh chat and pastes the bootstrap prompt, read this file first, then continue directly without asking them to repeat history unless essential information is genuinely missing.

## User working style

- Language: Italian.
- Wants direct execution and concise status updates, not long explanations.
- For long GitHub/CI work, provide short progress updates while actually working.
- Do not claim PASS, release, APK readiness or verification without real evidence.
- User validates final Android behavior physically on a Galaxy S25.
- When enough context exists, proceed instead of asking unnecessary questions.

## Engineering / release discipline

- SPEC-first.
- Runtime candidate should be exactly one commit and a direct child of its SPEC.
- Failed runtime replacements must be siblings from the same SPEC, not children of the failed candidate.
- Run full exact-SHA gates including API 28 and API 36.
- Promote `main` only after all required gates are green, using non-forced fast-forward.
- Use the persistent Arihna signer.
- Publish as prerelease when producing S25 validation builds.
- Redownload the published APK and verify SHA-256 and signer before handoff.
- Preserve unrelated features unless the SPEC explicitly changes them.

## Repository

- Repo: `archimede-projects/Arihna`
- Runtime branch: `main`
- Continuity branch: `chat-context`
- Continuity file: `docs/CHATGPT_CONTINUITY.md`

## Current verified runtime state

As of 2026-09-16 after the latest completed release flow:

- `main`: `c07663fcc61101a75e2b78ba550ccc6f60e38b35`
- Parent SPEC: `08eb114b6eec46ceee2cef65451302ac03b7e0fa`
- Runtime commit message: `feat(home): expand daily inspiration variety compact preview`
- Exact-SHA definitive gate run: `35083033004` — PASS
  - static/build job: `104751281456` — PASS
  - API 28 job: `104751281445` — PASS; 94 tests, 0 failure/error/skipped
  - API 36 job: `104751281189` — PASS; 11 tests, 0 failure/error/skipped
- Release workflow run: `35107380266` — PASS
- Release tag: `home-daily-inspiration-variety-c07663fc-20260916`
- Release is a GitHub prerelease, draft=false, exactly one APK asset.
- APK name: `arihna-home-daily-inspiration-variety.apk`
- APK download URL: `https://github.com/archimede-projects/Arihna/releases/download/home-daily-inspiration-variety-c07663fc-20260916/arihna-home-daily-inspiration-variety.apk`
- APK SHA-256: `9043369fe0e35f9c7059345169294d3c756d2ff8e6e6df861295966f14f58c3f`
- APK bytes: `386929193`
- Persistent signer certificate SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- Package: `com.archimedeprojects.arihna`
- minSdk 28, targetSdk 37, compileSdk 37.
- Published APK was redownloaded and the SHA-256, size and signer were reverified successfully.
- Physical Galaxy S25 validation is still user-side until the user reports the result.

## Latest shipped feature

Daily inspiration / positive daily action on Home:

- Expanded curated inspiration corpus to at least 60 unique days.
- Expanded positive daily actions to at least 30.
- Home preview constrained to a compact two-line presentation with ellipsis.
- Full content remains available in the detail dialog.
- Release contracts and exact-SHA API28/API36 gates passed.

## Important preserved project history

The repository already contains the previously integrated Quran/Hafs/Tajwid/Warsh, Home/Hijri, alarms, location, prayer and Qibla work. For exact implementation or historical release details, inspect GitHub history rather than guessing. Do not regress or casually rewrite those areas when handling a narrowly scoped request.

Persistent signer source historically used:
- branch: `location-step5-device-test`
- commit: `ce23a7f78695be95c7f8dfd2bcedbe22544da94c`
- keystore SHA-256: `bc9057f26ad6de7efb70a5df06effb1ed259f1d015c3f062d0f757b3f0983b72`

Frozen GeoNames asset historically used:
- release: `settings-s25-premium-3f28b6f0-20260906`
- APK: `arihna-settings-s25-premium.apk`
- `cities.db` SHA-256: `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`

## Latest user request / handoff

2026-09-16: User asked to move to a new chat because the current chat has become slow. They requested a prompt that allows the new chat to continue exactly where this one stopped, plus a persistent GitHub file that is read in every new chat and updated after every future user prompt.

Action taken:
- Created dedicated branch `chat-context` from current `main`.
- Created this continuity file and established the update protocol above.

Next action:
- In the new chat, read this file first.
- Continue from the verified APK/release state above and from whatever the user asks next, likely S25 validation feedback or the next requested feature/fix.
