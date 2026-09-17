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
- Runtime candidate = exactly one commit, direct child of its SPEC; replacement candidates after failure must be siblings from the same SPEC.
- Full exact-SHA gates include API28 and API36.
- Promote `main` only after required gates are green, via non-forced fast-forward.
- Use the persistent Arihna signer.
- S25 validation builds are GitHub prereleases; stable release may reuse byte-identical physically validated APK bytes when runtime is unchanged.
- Before handoff, redownload the published APK and verify SHA-256 + signer.
- Preserve unrelated features unless the SPEC explicitly changes them.

## Stable verified baseline

- Repo: `archimede-projects/Arihna`
- Runtime branch: `main`
- Continuity branch: `chat-context`
- Live `main` rechecked 2026-09-17: `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- Runtime: `fix(quran): prevent Tajwid overlap crash`
- Parent SPEC: `5b6f77bf1a7ee8634e74f46918b7f7263142316b`
- Exact-SHA gate `35110928396`: completed/success; static `104844112728`, API28 `104844112681`, API36 `104844112243` all success.
- Physical Galaxy S25 stabilization smoke: user reports PASS/no blocker for Hafs/Tajwid/Warsh navigation, bookmarks/fullscreen, app reopen/cold-start behavior, Prayer, Location, Qibla, alarms and permissions.

### Stable release

- Tag: `arihna-stable-be4af47d-20260917`
- Title: `Arihna — Stable — S25 validated`
- Target runtime: `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- `draft=false`, `prerelease=false`
- Stable release workflow run: `35188236936`, completed/success
- Stable job: `105094843337`, success
- Stable asset: `arihna.apk`
- Size: `386929193` bytes
- SHA-256: `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`
- Signer certificate SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- Stable APK is byte-identical to the physically S25-validated prerelease asset `arihna-quran-tajwid-crashfix.apk`.
- The user has now installed the stable APK on the Galaxy S25 and confirmed the install/open sanity check is clean.

## Relevant correction history

The prior S25 Tajwid paging crash was caused by overlapping Tajwid spans for shadda+tanwin/contextual rules. Runtime `be4af47d...` makes the contextual rule win and includes regression coverage across all 6,236 pinned Hafs Uthmani ayat. User confirmed the crash is resolved.

The first stable-publication workflow attempt `35188163125` failed before publication because `setup-android` requested obsolete SDK package `tools`; no release was published. The release-only workflow was corrected and second run `35188236936` completed successfully, including redownload verification. No runtime/app code changed.

## Latest user request / what was done

2026-09-17: User replied `Pulita`, confirming that the stable APK installation/open check on Galaxy S25 completed cleanly.

Actually done this turn:
- Rechecked live `main`; it still points to `be4af47d4f5b267e47eac09d761775a2d99bf0db`.
- Recorded the user's clean stable install/open result.
- No runtime/app code, release, tag or `main` ref was changed.

## Current state / exact next action

- Stable milestone is now fully closed: CI verified, published stable APK verified, broader S25 smoke already passed, and the final stable install/open sanity check is clean.
- No known blocker remains.
- Exact next action: begin one narrow post-stable hardening objective, SPEC-first. Recommended first target: Quran robustness under rapid paging, Hafs/Tajwid/Warsh switching, bookmarks/fullscreen/state restoration and long-session memory/performance, while preserving the stable baseline.
- After hardening, proceed to one user-prioritized product improvement at a time.
