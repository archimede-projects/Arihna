from pathlib import Path

path = Path('PROJECT_SPEC.md')
text = path.read_text()

old = '''6. System last-known candidates are real cached fixes only. Inspect the framework `fused` and `network` providers when available and choose the **most recently captured valid** candidate; do not use passive/GPS as an invented extra policy. `getLastKnownLocation()` may return `null`, so no cache is assumed to exist.
7. Compare a valid system last-known candidate with Arihna's already persisted real Device fix and use the **newer real fix** as the fallback candidate. Do not invent coordinates, city or timezone. Device timezone remains the timezone captured with the real fix according to the existing Device model/persistence policy.
'''
new = '''6. System last-known candidates are real cached framework `Location` objects only. Inspect the framework `fused` and `network` providers when available and choose the **most recently captured valid** raw candidate; do not use passive/GPS as an invented extra policy. `getLastKnownLocation()` may return `null`, so no cache is assumed to exist.
7. A raw framework last-known `Location` does **not** contain the historical `ZoneId` associated with its capture and therefore is not, by itself, a complete Arihna `DeviceLocationFix`. Never attach `ZoneId.systemDefault()` at fallback time to old coordinates. A raw framework candidate may become a calculable CACHED fix only when Arihna can provenance-match it to an already persisted real Device fix with the same capture instant and coordinates; that persisted record supplies the captured `ZoneId`. Otherwise the framework candidate remains evidence that cached coordinates exist, but the calculable fallback is the newest complete Arihna-persisted Device fix. If no complete cached Device fix exists, return the controlled `Unavailable` state rather than inventing a timezone.
'''
if old not in text:
    raise SystemExit('approved cache bullets not found')
text = text.replace(old, new, 1)

old = '''9. Extend `DeviceLocationResult.Success` (or an equivalent datasource-domain result) with explicit `LocationFreshness.FRESH/CACHED` metadata so a last-known fallback can never be mislabeled as current. `LocationResolutionState.Ready` already carries freshness and remains the authoritative resolved state.
'''
new = '''9. Extend `DeviceLocationResult.Success` (or an equivalent datasource-domain result) with explicit `LocationFreshness.FRESH/CACHED` metadata so a last-known fallback can never be mislabeled as current. `SelectedLocation` also carries the resolved freshness metadata for Device locations (`FRESH` or `CACHED`; Manual has no Device freshness), so downstream Prayer/Home code does not need to infer freshness from timestamps. `LocationResolutionState.Ready` must expose the same authoritative value without allowing the two representations to diverge.
'''
if old not in text:
    raise SystemExit('freshness decision bullet not found')
text = text.replace(old, new, 1)

old = '''SelectedLocation
- source
- coordinates
- zoneId
- displayName
'''
new = '''SelectedLocation
- source
- coordinates
- zoneId
- displayName
- freshness? (`FRESH`/`CACHED` for Device; null for Manual)
'''
if old not in text:
    raise SystemExit('SelectedLocation model block not found')
text = text.replace(old, new, 1)

old = '''- newest valid real cache is selected deterministically and absent cache remains controlled unavailable;
'''
new = '''- raw framework last-known selection is deterministic, but no raw cached coordinates are paired with a newly sampled/system-default timezone; only a provenance-matched or already persisted complete Device fix can become calculable CACHED state, and absent complete cache remains controlled unavailable;
'''
if old not in text:
    raise SystemExit('gate cache evidence bullet not found')
text = text.replace(old, new, 1)

path.write_text(text)
