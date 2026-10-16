# SPEC — Asr selected state + approved vintage ringtone — 2026-10-16

Base runtime: `e8733b4b7702b99efd1f8886dd67b86a029e5319`

## User feedback
Galaxy S25 validation of prayer settings is functionally PASS, with one UI defect: Standard/Hanafi do not visually indicate the selected Asr method.

The user also explicitly requests integration of the previously approved 4-second vintage alarm-clock sound.

## Requirements

### A. Asr selected state
1. On `Impostazioni orari`, Standard and Hanafi must have clearly distinct selected/unselected visuals.
2. The visual selection must always reflect the persisted `settings.asrMethod`.
3. Switching methods must update the highlight immediately and remain persisted after leaving/reopening.
4. Keep existing calculation behavior unchanged.

### B. Arihna vintage ringtone
1. Add an app-owned personal-alarm sound named `Sveglia classica Arihna`.
2. Audio content must be EXACTLY the previously approved preview:
   - first 4.0 seconds of Freesound 219244 / ZyryTSounds;
   - no speed or pitch modification;
   - approved preview SHA-256 `43fe360338ad312448b4b4bdf69f02e06db4b8d32af21a1c137d70c54b868ccd`.
3. Build must fetch the pinned approved preview asset from Arihna release tag `audio-preview-alarm-clock-first4-20261014`, verify the exact SHA-256, and package it as an app raw resource. Build must fail on digest mismatch.
4. Personal alarm editor must expose three sound choices:
   - `Sveglia classica Arihna`;
   - Android/system ringtone picker;
   - Silent.
5. Android ringtone picker must remain available and unchanged as a separate option.
6. The Arihna vintage sound must loop continuously while the personal alarm is ringing.
7. Per-alarm volume applies exactly as for other personal alarm sounds.
8. Stop, Snooze, replacement, service stop/failure/destroy must stop/release vintage playback and preserve existing alarm-stream lease restoration.
9. Silent behavior remains unchanged.
10. Prayer/Adhan sound paths, Makkah x2, Adhan auto-dismiss, prayer scheduling, Quran, Qibla, location and permissions must not regress.
11. Persisted existing alarms must remain readable with no data loss. New vintage selection must persist/restart safely.
12. Add provenance documentation: Freesound 219244, author ZyryTSounds, CC0; GitHub/Arihna release used only as pinned byte transport.

## Tests
- Asr UI test verifies selected visual semantics/state changes between Standard and Hanafi.
- Personal alarm UI test verifies vintage option is present/selectable, persists save payload, and Android picker remains available.
- Persistence/codec test verifies vintage profile round-trip and older records still decode.
- Playback unit/instrumentation test verifies vintage profile resolves to bundled raw resource, loops, obeys per-alarm volume, and stops through existing stop/snooze lifecycle.
- Static build contract verifies exact vintage source URL/tag and approved SHA-256.
- Full exact-SHA static/build + API28 + API36 gate before promotion.

## Release discipline
- Candidate is exactly one commit direct child of this SPEC.
- Do not alter the approved audio content.
- Promote only after full green gate.
- Publish verified S25 prerelease, redownload it, verify SHA-256 + persistent signer before handoff.
