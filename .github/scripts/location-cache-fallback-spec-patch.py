from pathlib import Path

path = Path('PROJECT_SPEC.md')
text = path.read_text()
marker = '#### Permission policy\n'
title = '#### Galaxy S25 intermittent current-location fallback — APPROVED 2026-09-02\n'
if title in text:
    raise SystemExit('decision already present')
if marker not in text:
    raise SystemExit('permission marker missing')

section = '''#### Galaxy S25 intermittent current-location fallback — APPROVED 2026-09-02

The 2026-08-31 network-first one-shot correction is superseded. A later draft proposing fused-first as a deterministic fix was intentionally kept off `main` and is also superseded by the complete real-device evidence below. The production problem is now classified as **intermittent current-location availability on the primary Galaxy S25**, not as a reliably wrong provider choice or a simple timeout that can be fixed by waiting longer.

Diagnostic path and evidence, all with framework `LocationManager`, `ACCESS_COARSE_LOCATION` only, no Google Play Services Location, Galaxy S25 / SDK 36:

- Production network-first one-shot repeatedly reached Arihna's 20-second timeout with no fix.
- Parallel A/B `getCurrentLocation()` showed framework `network` returning callback `null` at about **30.018 seconds**, while framework `fused` in one run returned a genuinely current fix at **22.838 seconds** with monotonic age about **0.014 seconds**.
- Repeating the same A/B in the same app session, without closing Arihna or changing the observable environment, later produced **`null` from both providers** at about **30.020 seconds network / 30.028 seconds fused**. Therefore fused is not a deterministic success path; timeout extension alone is not an adequate reliability fix.
- A later fused callback observed with `latencyMs=42` and `elapsedAgeMs=25974` is interpreted by units literally as a callback after **0.042 seconds** carrying a fix about **25.974 seconds old**, not as a 23-second acquisition. This reinforces that callback latency and fix age are separate quantities and must not be conflated.
- Bounded `network + requestLocationUpdates`: an immediate historical callback was observed at **0.039 seconds** with age about **284.536 seconds**, was rejected by the diagnostic 10-second bound, and no fresh callback arrived before terminal timeout at **35.027 seconds**. A later repetition returned a network callback after **0.037 seconds** aged about **589.022 seconds**, then again timed out at **35.020 seconds**.
- Bounded `fused + requestLocationUpdates`: one callback after **0.038 seconds** was already about **50.088 seconds old**, was rejected by the diagnostic bound, and no new callback arrived before timeout at **35.006 seconds**.
- During the later network-updates repetition, the existing production fused foreground stream subscribed after the one-shot timeout and received a real fused fix almost immediately; that fix was about **10.415 seconds old**. This demonstrates that a useful real cached/historical fix may exist in the framework stack even when the explicit current request fails.
- The diagnostic `requestLocationUpdates` acceptance bound of 10 seconds was methodological only. It is not and must never become a production cache TTL/FRESH threshold.
- Android framework documentation explicitly permits `getCurrentLocation()` to complete with `null` when a provider cannot generate a valid current fix. `getLastKnownLocation()` may return a quite old cached location or `null`; therefore Arihna must treat last-known data as optional real cache and expose its age honestly.

Approved production strategy — **Approach B: current attempt + transparent cache fallback**:

1. Keep one explicit framework **`fused` `getCurrentLocation()`** attempt for Device refresh. Revoke network-first selection; do not add a parallel provider race in this correction.
2. Set Arihna's current-fix opportunity budget to **30 seconds**. This value is not based on an assumption that fused will succeed by 30 seconds; it is a bounded UX/power budget that gives the framework a real opportunity to return a current fix while acknowledging that it may still terminate with `null`.
3. A non-null valid result from the explicit current request enters the existing acceptance pipeline as `FRESH` input. The existing 5 km/ZoneId significance policy remains authoritative; this correction does not force persistence of insignificant movement.
4. If framework current-location returns `null`/provider-unavailable, the Android datasource immediately attempts a **last-known fallback** instead of treating the session as a hard no-location failure.
5. If the coordinator's 30-second timeout cancels the current request before a result, the coordinator explicitly asks the datasource for the same last-known fallback after cancellation. Timeout ownership remains in `LocationCoordinator`/`LocationUpdatePolicy`; the datasource does not introduce a second independent timeout.
6. System last-known candidates are real cached fixes only. Inspect the framework `fused` and `network` providers when available and choose the **most recently captured valid** candidate; do not use passive/GPS as an invented extra policy. `getLastKnownLocation()` may return `null`, so no cache is assumed to exist.
7. Compare a valid system last-known candidate with Arihna's already persisted real Device fix and use the **newer real fix** as the fallback candidate. Do not invent coordinates, city or timezone. Device timezone remains the timezone captured with the real fix according to the existing Device model/persistence policy.
8. There is **no production maximum-age TTL** in this correction. A real valid cached fix may remain usable even when it is hours or days old; its age/timestamp must be shown explicitly. If neither framework nor persisted cache exists, return the existing controlled `Unavailable` state.
9. Extend `DeviceLocationResult.Success` (or an equivalent datasource-domain result) with explicit `LocationFreshness.FRESH/CACHED` metadata so a last-known fallback can never be mislabeled as current. `LocationResolutionState.Ready` already carries freshness and remains the authoritative resolved state.
10. A CACHED fallback is allowed to produce a normal `Ready` location so prayer times continue to calculate from the last real coordinates/ZoneId. This is an intentional UX/reliability change from the current `Unavailable(cachedLocation=...)` behavior: cached real location remains usable for calculations while clearly disclosed as cached.
11. Prayer Schedule/Home must propagate Device freshness and capture time. When the active Device location is CACHED, show a visible message/badge such as **“Basato su posizione di 2 ore fa”** (or an absolute/date-style equivalent for older data). Do not show this cache-age badge for Manual location.
12. When Home is using a CACHED Device location, expose **“Aggiorna posizione”**. The action retries the existing Device current-location resolution on demand using current permission/services state; it does not introduce polling, a background service, or a new refresh cycle.
13. Keep the existing foreground significant-update stream, 15-minute minimum interval, zero provider-level distance filter, 5 km/ZoneId domain acceptance, lifecycle start/stop behavior, persistence model, manual-city path, COARSE-only permission policy, and no-Play-Services policy unchanged.
14. No FINE permission, background location, foreground location service, current-provider race, production freshness TTL, or cache expiry policy is authorized by this decision.

Freshness semantics for this correction:

- `FRESH`: result came from the explicit current-location operation in the current resolution flow and survived the existing validity/significance pipeline.
- `CACHED`: result came from framework last-known or Arihna's previously persisted real Device fix, or the existing significance policy deliberately retained the prior accepted real fix. CACHED always retains the original capture timestamp.
- Cache age shown to the user is derived from the real `LocationSource.Device.capturedAt` timestamp and the current clock; clamp negative display ages to zero rather than inventing future age.

Required implementation/gate evidence before promotion to `main`:

- exact technical SHA must descend directly from this spec-first commit;
- current one-shot selector is framework `fused`, not network-first;
- `currentFixTimeout` default is exactly 30 seconds;
- current callback `null` falls back to real last-known without mislabeling it FRESH;
- coordinator timeout also falls back to real last-known;
- newest valid real cache is selected deterministically and absent cache remains controlled unavailable;
- existing 5 km/ZoneId acceptance and 15-minute foreground-update policy remain unchanged;
- Prayer Schedule propagates CACHED metadata/capture time and Home renders the cache-age disclosure plus on-demand `Aggiorna posizione` action;
- unit regression, `assembleDebug`, and Android 9/API28 `connectedDebugAndroidTest` all pass with zero skipped tests;
- manifest/dependency policy remains `ACCESS_COARSE_LOCATION` only, no FINE/BACKGROUND and no Google Play Services Location;
- frozen GeoNames runtime asset integrity remains unchanged.

'''

text = text.replace(marker, section + marker, 1)
text = text.replace('- fresh-fix timeout: **20 seconds**;', '- fresh-fix timeout: **30 seconds**;', 1)
text = text.replace('- The 20-second fresh-fix timeout remains owned by `LocationCoordinator`/`LocationUpdatePolicy`;', '- The 30-second fresh-fix timeout remains owned by `LocationCoordinator`/`LocationUpdatePolicy`;', 1)
text = text.replace('- 20s timeout / 5 km / 15 min; timezone change significant.', '- 30s timeout / 5 km / 15 min; timezone change significant.', 1)

changelog_marker = '### 2026-08-31 — Prayer Engine + Location integration STEP 5 CLOSED after exact clean-candidate gate\n'
if changelog_marker not in text:
    raise SystemExit('changelog marker missing')
changelog = '''### 2026-09-02 — Galaxy S25 intermittent current-location cache fallback approved

After the complete S25 diagnostic campaign, the prior network-first assumption is revoked and fused is not treated as deterministic: repeated same-session A/B runs showed both a genuinely current fused success and a later fused `null` at the framework's approximately 30-second terminal window, while network also returned `null`. Bounded request-updates probes for both providers delivered historical callbacks and failed to produce a fresh follow-up. A later production fused foreground callback demonstrated that a useful real cached fix may still exist while current acquisition fails. Approved correction is therefore Approach B: one framework fused current attempt with a 30-second coordinator-owned budget, followed on `null` or timeout by the newest valid real last-known/persisted Device fix, explicitly marked CACHED with original timestamp and no arbitrary expiry TTL. Prayer calculations may use that CACHED real location, Home must disclose its age and offer an on-demand `Aggiorna posizione` retry. Existing 5 km/ZoneId significance, 15-minute foreground updates, COARSE-only/no-Play-Services policy and no-invented-location discipline remain unchanged. Production implementation requires a new exact-SHA full gate before promotion.

'''
text = text.replace(changelog_marker, changelog + changelog_marker, 1)
path.write_text(text)
