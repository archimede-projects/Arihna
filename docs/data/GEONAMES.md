# GeoNames city database pipeline

Arihna's manual-city database is generated offline from GeoNames `cities500` plus selected alternate names. Runtime city search never calls an online geocoder.

## Populated-place feature filter

`cities500.txt` is an upstream candidate source, not Arihna's final city set. Arihna requires feature class `P` and applies a deny-by-default whitelist to feature code (column 7):

- `PPL`
- `PPLA`, `PPLA2`, `PPLA3`, `PPLA4`, `PPLA5`
- `PPLC`, `PPLG`
- `PPLF`, `PPLR`, `STLMT`

Explicitly excluded examples include `PPLX` (section of a larger populated place), `PPLH`/`PPLCH` (historical), `PPLQ` (abandoned), `PPLW` (destroyed), `PPLL` (minor locality) and `PPLS` (aggregate populated places). Unknown/future codes remain excluded until reviewed. Official reference: https://www.geonames.org/export/codes.html

The generator metadata retains raw/included/excluded counts and per-code distributions so upstream classification changes are auditable.

## Lossless runtime-minimal storage profile

No city or alias is removed for footprint optimization.

The final candidate runtime schema uses:

- country/region display names in the existing `country` and `admin1` lookup tables; city keeps their short text codes because replacing those codes with numeric ids saved only about 34 KB after compression in the pre-benchmark;
- `latitude_e6` / `longitude_e6` signed SQLite INTEGER microdegrees instead of REAL; the E6 staging generator rejects coordinates that would require silent rounding and validates exact E6 round-trip for every row;
- a small `timezone(id, name)` lookup, with `city.timezone_id` numeric and runtime `ZoneId.of(timezone.name)` preserving the original IANA identifier;
- `population` as SQLite INTEGER for ranking/disambiguation;
- only `city_lat_lon_idx(latitude_e6, longitude_e6)` as an explicit runtime secondary index. `findById` uses the city INTEGER PRIMARY KEY; `search` uses FTS4 then INTEGER PRIMARY KEY joins; `nearest` uses the retained coordinate index;
- final `city_alias` without preprocessing-only UNIQUE/secondary indexes. Alias uniqueness is enforced in validated build-time staging, then every already-deduplicated alias row (including its id) is copied losslessly into the read-only runtime database;
- `city_search` unchanged as FTS4 `content=''` with `docid = city_alias.id`.

The runtime-minimal optimizer performs a full logical row comparison against the validated E6 staging database after joining numeric timezone ids back to IANA text, verifies city/alias/timezone counts, checks for duplicate alias pairs and orphan timezone ids, and exercises representative SQL for `findById`, FTS `search`, and `nearest`. `EXPLAIN QUERY PLAN` must show FTS usage for search and `city_lat_lon_idx` for nearest.

## Integrity validation

Do **not** use a global `PRAGMA integrity_check` as the FTS4 contentless gate. Since SQLite 3.44, global integrity checking invokes virtual-table `xIntegrity`; FTS4 cannot validate a `content=''` index against original content that is intentionally absent and may emit `unable to validate the inverted index ... SQL logic error` for a valid database.

References:

- https://sqlite.org/releaselog/3_44_0.html
- https://sqlite.org/releaselog/3_45_1.html
- https://sqlite.org/fts3.html

The runtime-minimal build therefore requires:

1. table-scoped `PRAGMA integrity_check('<table>')` = `ok` for `country`, `admin1`, `timezone`, `city`, and `city_alias`;
2. FTS document count equal to `city_alias` count;
3. zero orphan FTS docids and zero aliases missing from FTS;
4. golden `MATCH` coverage for Roma (`3169070`), Makkah/Mecca (`104515`), New York (`5128581`) and Sydney (`2147714`);
5. lossless logical equality with staging for every city and alias row.

## APK-size measurement and exact benchmark provenance

The filtered baseline run `33280106118` measured 224,327 cities / 258,681 aliases and a 27,459,231-byte APK increment. A later E6-only run (`33292976302`) used an updated GeoNames `latest` payload, so cross-run deltas must not be mixed blindly.

For the decisive runtime-minimal AAPT measurement, the workflow intentionally reuses the exact validated E6 staging database from run `33292976302`, rather than downloading `latest` again. It verifies:

- staging DB SHA-256 `e567b7eabb40994d5d9fb95209c050503cf5747a6cdd25ef73d195aeb4003877`;
- 224,330 cities / 258,685 aliases / 391 timezone strings;
- the recorded source-file SHA-256 values from that run.

This makes the runtime-minimal result directly comparable with the E6 measurement from the same semantic snapshot. The APK build additionally hashes the decompressed `assets/geonames/cities.db` entry and requires it to equal the generated runtime DB hash, preventing a stale Gradle/AAPT asset from contaminating the result.

The APK already stores `cities.db` using ZIP method 8 (DEFLATE). Separate `.gz`/`.zst` precompression is not used because Android platform SQLite cannot open such a stream directly; it would require extraction and extra on-device storage/complexity.

If the real runtime-minimal APK increment is greater than 20 MiB, STEP 4 stops for review. If it is 20 MiB or less, the compact schema can be finalized and CityRepository/API28 validation may proceed.
