# GeoNames city database pipeline

Arihna's manual-city database is generated offline from the official GeoNames `cities500` dump plus selected alternate names. Runtime city search does not call an online geocoder.

## Populated-place feature filter

The raw `cities500.txt` file is an upstream candidate source, not the final Arihna city set. GeoNames includes several feature-code semantics under class `P`; some are sections of larger cities, historical/abandoned/destroyed places, minor localities, or aggregate records that should not appear as independent manual-city choices.

Arihna therefore requires `feature class = P` and applies an explicit deny-by-default whitelist to column 7 (`feature code`). Any current or future code not listed below is excluded until separately reviewed.

Included codes:

- `PPL` — populated place.
- `PPLA`, `PPLA2`, `PPLA3`, `PPLA4`, `PPLA5` — seats of administrative divisions.
- `PPLC` — capital of a political entity.
- `PPLG` — seat of government of a political entity.
- `PPLF` — farm village. GeoNames defines this as a current populated place; the occupational character does not make it historical or non-inhabited.
- `PPLR` — religious populated place. GeoNames likewise defines it as a current populated place.
- `STLMT` — current inhabited settlement with a GeoNames-specific classification. Arihna preserves GeoNames-provided country and timezone fields and does not reinterpret geopolitical status.

Explicitly excluded examples:

- `PPLX` — section of a larger populated place; a district/neighborhood is not an independent city choice.
- `PPLH`, `PPLCH` — historical populated place / historical capital.
- `PPLQ` — abandoned populated place.
- `PPLW` — destroyed populated place.
- `PPLL` — minor populated locality rather than a city/town/village center for Arihna's manual-city UX.
- `PPLS` — aggregate/plural populated places rather than one independent center.

Official GeoNames feature-code reference: https://www.geonames.org/export/codes.html

The generator writes auditable filter evidence into metadata: raw row count, included/excluded row counts, raw feature-class and feature-code distributions, and per-code included/excluded counts. This makes future upstream classification changes visible instead of silently changing the bundled dataset.

## Lossless storage optimization

The semantic dataset is fixed during this optimization step: no city or alias may be removed to reduce size.

### Coordinate storage

Generator v4 changes only the coordinate representation. `city.latitude`/`longitude` SQLite `REAL` columns are replaced by signed `latitude_e6` / `longitude_e6` integers (degrees × 1,000,000). GeoNames coordinates in the approved snapshot are exactly representable at microdegree precision; the generator rejects a future source value requiring silent rounding. After generation it validates every bundled row by decoding `e6 / 1_000_000.0` and checking that converting back to E6 reproduces the exact stored integer.

All other schema/index choices remain unchanged in this measurement so the APK delta can be attributed specifically to coordinate compaction.

Country and region display names were already normalized before this optimization: `city` stores country/admin1 codes while `country` and `admin1` store the readable names. A local pre-benchmark found that converting those short codes to extra numeric lookup ids provides negligible compressed-APK benefit relative to the added schema complexity, so that change is intentionally not applied.

## FTS4 contentless integrity validation

`city_search` intentionally uses FTS4 with `content=''` and `docid = city_alias.id`. This keeps only the inverted index needed for lookup and does not duplicate the alias text inside the FTS virtual table.

Do **not** replace the validation below with a global `PRAGMA integrity_check` while this schema remains contentless. Starting with SQLite 3.44.0, global `PRAGMA integrity_check` invokes virtual-table `xIntegrity` for FTS3/FTS4/FTS5 and other virtual tables. FTS4's integrity path attempts to validate the inverted index against source content; with `content=''`, that source content is intentionally absent, so a valid index can fail with `unable to validate the inverted index ... SQL logic error`.

Official references:

- SQLite 3.44.0 release notes: https://sqlite.org/releaselog/3_44_0.html
- SQLite 3.45.1 release notes: https://sqlite.org/releaselog/3_45_1.html
- SQLite FTS3/FTS4 documentation: https://sqlite.org/fts3.html

The generator therefore uses two complementary gates:

1. Table-scoped `PRAGMA integrity_check('<table>')` for Arihna-owned non-FTS tables. This preserves SQLite's structural B-tree/index integrity checking for the ordinary database.
2. Functional validation for `city_search`:
   - `city_search_docsize` must contain exactly one document row for every `city_alias` row;
   - no indexed docid may be orphaned from `city_alias`;
   - no `city_alias.id` may be missing from the FTS document roster;
   - representative `MATCH` queries must return the exact expected `city_alias.id` for Roma/Rome `3169070`, Makkah and Mecca `104515`, New York `5128581`, and Sydney `2147714`.

The generator writes validation evidence and the Python SQLite version into metadata JSON.

## APK-size gate

Filtered-schema baseline (run `33280106118`):

- cities: 224,327
- aliases: 258,681
- SQLite: 56,692,736 bytes
- APK-compressed SQLite asset: 27,459,105 bytes
- APK increment: 27,459,231 bytes

Each significant lossless storage change is measured independently against this semantic dataset. The workflow builds a baseline debug APK without the generated database, then another APK with `assets/geonames/cities.db` and records SQLite bytes, compressed asset bytes, ZIP method, and APK increment.

The current APK already stores the database using ZIP method 8 (DEFLATE). A separately compressed `.gz`/`.zst` database is not used: Android platform SQLite cannot open that stream directly, so it would require runtime extraction and duplicate on-device storage/complexity.

If the final increment remains greater than 20 MiB after approved lossless optimizations, the Location STEP 4 pipeline stops for explicit review. It must never silently switch datasets or change the approved feature-code policy merely to pass the size gate.
