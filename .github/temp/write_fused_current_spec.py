from pathlib import Path

path = Path("PROJECT_SPEC.md")
text = path.read_text(encoding="utf-8")

section_title = "#### S25 diagnostic closure and fused current-location production correction — APPROVED 2026-09-01"
if section_title in text:
    raise SystemExit("production correction section already present")

marker = "#### Permission policy\n"
if marker not in text:
    raise SystemExit("permission policy marker not found")

section = '''#### S25 diagnostic closure and fused current-location production correction — APPROVED 2026-09-01

The earlier 2026-08-31 network-first one-shot correction is **superseded** by real Galaxy S25 evidence. Production must no longer prefer `NETWORK_PROVIDER` for the explicit current-location request.

Real-device diagnostic path on the primary Galaxy S25 (SDK 36, `ACCESS_COARSE_LOCATION` only, framework `LocationManager` providers, no Google Play Services Location):

- Production `network + getCurrentLocation()` repeatedly hit Arihna's 20-second timeout; the dedicated 35-second A/B then returned `null` from framework network at **30.018 seconds**.
- In the same A/B session, framework `fused + getCurrentLocation()` returned a genuinely current coarse fix at **22.838 seconds**, with monotonic age approximately **0.014 seconds** and accuracy 2,000 m.
- `network + requestLocationUpdates` bounded diagnostic: first callback after **0.039 seconds** was the same cached network fix, aged **284.536 seconds**, therefore explicitly rejected; no fresh follow-up arrived and the probe ended `TIMEOUT` after **35.027 seconds**.
- `fused + requestLocationUpdates` bounded diagnostic: first callback after **0.038 seconds** was cached, aged **50.088 seconds**, therefore explicitly rejected; no fresh follow-up arrived and the probe ended `TIMEOUT` after **35.006 seconds**.
- Both bounded-update probes used the same controlled profile: interval **10 seconds**, minimum update interval **0 seconds**, minimum distance **0 m**, maximum update delay **0 seconds** (no batching), balanced power accuracy, duration **35 seconds**, no `setMaxUpdates(1)`, and explicit monotonic age validation. The diagnostic acceptance bound of **10 seconds** was methodological only and is **not** a production `FRESH` definition.
- The observed delivery of cached listener fixes much older than the requested 10-second interval means Arihna must continue to validate location age/state explicitly rather than relying on provider interval guarantees alone.

Completed diagnostic matrix:

| Provider | `getCurrentLocation()` | bounded `requestLocationUpdates()` |
| --- | --- | --- |
| framework `network` | **FAIL** — `null` at 30.018 s | **FAIL** — cached callback at 0.039 s, then timeout at 35.027 s |
| framework `fused` | **SUCCESS** — current fix at 22.838 s | **FAIL** — cached callback at 0.038 s, then timeout at 35.006 s |

Production decision:

- Explicit Device-location one-shot uses framework **`fused` first**, with `NETWORK_PROVIDER` only as an availability fallback when framework `fused` is not enabled/present. The previous network-first order is revoked.
- Keep `LocationManagerCompat.getCurrentLocation(...)`; do not replace production current-location acquisition with `requestLocationUpdates()`.
- Increase Arihna's `currentFixTimeout` from **20 seconds to 30 seconds**. This is an initial bounded production value chosen to provide **7.162 seconds** of margin beyond the single observed 22.838-second fused success while avoiding an even longer 35-second user-visible wait. It is evidence-backed but not treated as a latency guarantee; validate it again on the Galaxy S25 after release.
- While `LocationResolutionState.Resolving` is visible, communicate the bounded wait explicitly: **“Ricerca della posizione in corso. Può richiedere fino a 30 secondi.”** Keep the existing progress indicator. The timeout error copy must also state **30 seconds**.
- Foreground significant-update behavior remains unchanged: existing provider selector, 15-minute minimum interval, zero provider-level distance filter, and domain 5 km / `ZoneId` acceptance policy remain as before.
- Cache/FRESH semantics, persistence, foreground lifecycle, manual-city behavior and Home latency remain unchanged and out of scope for this correction.
- Keep `ACCESS_COARSE_LOCATION` only. Do not add FINE/background location or Google Play Services Location.
- No production `FRESH` age threshold is introduced by this correction.

Implementation gate before promotion to `main` must verify the exact implementation SHA with unit regression, `assembleDebug`, Android 9/API28 connected instrumentation, COARSE-only permission policy, fused-first one-shot selection with network fallback, the 30-second policy default, and the updated resolving/timeout UX copy.

'''
text = text.replace(marker, section + marker, 1)

replacements = {
    "- fresh-fix timeout: **20 seconds**;": "- fresh-fix timeout: **30 seconds**;",
    "- The 20-second fresh-fix timeout remains owned by `LocationCoordinator`/`LocationUpdatePolicy`; `DeviceLocationDataSource` supplies a cancellable current-location operation and does not maintain a second independent timeout.": "- The 30-second fresh-fix timeout remains owned by `LocationCoordinator`/`LocationUpdatePolicy`; `DeviceLocationDataSource` supplies a cancellable current-location operation and does not maintain a second independent timeout.",
    "- 20s timeout / 5 km / 15 min; timezone change significant.": "- 30s timeout / 5 km / 15 min; timezone change significant.",
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"normative text not found: {old}")
    text = text.replace(old, new, 1)

change_log_marker = "## 17. Change log\n"
if change_log_marker not in text:
    raise SystemExit("change log marker not found")
change_log = '''## 17. Change log

### 2026-09-01 — S25 native Location diagnosis closed; fused current-location production correction approved

The real Galaxy S25 2×2 provider/API diagnostic matrix is complete. Framework network failed both `getCurrentLocation()` (`null` at 30.018 seconds) and bounded `requestLocationUpdates()` (cached 284.536-second-old callback at 0.039 seconds, then timeout at 35.027 seconds). Framework fused was the only successful current-fix path: `getCurrentLocation()` returned a genuinely current coarse fix at 22.838 seconds with approximately 0.014 seconds monotonic age; bounded fused updates returned only a cached 50.088-second-old callback at 0.038 seconds and then timed out at 35.006 seconds. The bounded-listener Plan B is therefore closed. The previous network-first one-shot decision is superseded. Production is approved to use framework fused first with network only as availability fallback, retain `LocationManagerCompat.getCurrentLocation(...)`, raise the Arihna current-fix timeout to 30 seconds, and make the resolving/timeout UI explicitly communicate that bounded wait. Foreground significant-update behavior, cache/FRESH semantics, persistence, Home latency, COARSE-only permission policy, and the no-Play-Services constraint remain unchanged. No production freshness-age threshold is introduced.
'''
text = text.replace(change_log_marker, change_log, 1)

path.write_text(text, encoding="utf-8")
