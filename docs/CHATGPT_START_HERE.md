# Arihna — ChatGPT START HERE

This file is the entry point for any new ChatGPT conversation about the Arihna project.

## Repository
- GitHub: `archimede-projects/Arihna`
- Continuity branch: `chat-context`
- Runtime branch: `main`

## Mandatory startup procedure
Before answering any Arihna request:

1. Read this file from branch `chat-context`.
2. Read `docs/CHATGPT_CONTINUITY.md` from branch `chat-context`.
3. Treat `docs/CHATGPT_CONTINUITY.md` as the authoritative cross-chat handoff for:
   - current runtime/release state;
   - validated and failed candidates;
   - Galaxy S25 physical-validation status;
   - current product requirements;
   - exact next action;
   - release/signing/test discipline.
4. Live-verify relevant GitHub state before claiming current SHA, CI PASS, release, APK, signer or stable status.
5. Do the requested work.
6. Before replying, update `docs/CHATGPT_CONTINUITY.md` on branch `chat-context` with:
   - what the user asked;
   - what was actually done;
   - verified SHA/run/release identifiers;
   - any failures/problems;
   - current state;
   - exact next action.
7. Never write continuity files to `main`.
8. Never merge `chat-context` into `main`.
9. Never store secrets/tokens/private keys in continuity.
10. Do not claim physical Android PASS unless the user explicitly reports it from the Galaxy S25.

## Engineering rules
- SPEC-first for runtime/code changes.
- Shipping candidate = exactly one commit, direct child of its SPEC.
- Failed replacement candidates are siblings from the same SPEC.
- Exact-SHA gates include API28 and API36.
- Promote `main` only after required gates are green.
- User-facing runtime changes go to an S25 prerelease first.
- Use the persistent Arihna signer.
- Redownload published APKs and verify SHA-256 + signer before handoff/stable claims.
- When a user has physically validated a prerelease, prefer publishing stable from those exact APK bytes rather than rebuilding.
- Preserve unrelated stable features unless the SPEC explicitly changes them.

## User communication preference
- Italian.
- Concise and operational.
- During long CI/release work, give short progress updates.
- Consultant-ready operational responses should be delivered as one single copyable block.

## Minimal prompt for a new chat
The user can simply write:

`Apri il repo archimede-projects/Arihna, branch chat-context. Leggi docs/CHATGPT_START_HERE.md e docs/CHATGPT_CONTINUITY.md, verifica lo stato live su GitHub e riprendi esattamente da lì. Aggiorna la continuità dopo ogni mio prompt.`

That is sufficient to reconstruct the project state before continuing.
