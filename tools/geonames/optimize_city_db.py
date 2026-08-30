#!/usr/bin/env python3
"""Build Arihna's runtime-minimal city DB from the frozen validated E6 staging DB."""
from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import time
from pathlib import Path

OPTIMIZER_VERSION = 2
NON_FTS_INTEGRITY_TABLES = ("country", "admin1", "timezone", "city", "city_alias")
GOLDEN_SEARCH_ALIASES = (
    ("Roma", 3169070, "roma"),
    ("Makkah", 104515, "makkah"),
    ("Mecca", 104515, "mecca"),
    ("New York", 5128581, "new york"),
    ("Sydney", 2147714, "sydney"),
)

# Capability fallbacks are intentionally finite and reviewed. Runtime always
# tries the modern IANA id first. These names are used only when that id is not
# understood by the platform tzdata (Android 9/API28 baseline = tzdata 2018e).
API28_TIMEZONE_COMPATIBILITY = {
    "Europe/Kyiv": {
        "compat_name": "Europe/Kiev",
        "category": "iana_link",
    },
    "America/Ciudad_Juarez": {
        "compat_name": "America/Ojinaga",
        "category": "tzdata_2018e_legacy_equivalence",
    },
    "America/Coyhaique": {
        "compat_name": "America/Punta_Arenas",
        "category": "peer_equivalence_2026_2100",
    },
    "Asia/Qostanay": {
        "compat_name": "Asia/Aqtobe",
        "category": "peer_equivalence_2026_2100",
    },
}
API28_UNSUPPORTED_TIMEZONES = {"America/Nuuk"}
EXPECTED_API28_MAPPED_CITY_COUNTS = {
    "Europe/Kyiv": 4733,
    "America/Ciudad_Juarez": 21,
    "America/Coyhaique": 6,
    "Asia/Qostanay": 31,
}
EXPECTED_API28_UNSUPPORTED_CITY_COUNT = 17


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_source_schema(db: sqlite3.Connection) -> None:
    expected = [
        "id", "name", "country_code", "admin1_code", "latitude_e6",
        "longitude_e6", "timezone_id", "population",
    ]
    actual = [row[1] for row in db.execute("PRAGMA table_info(city)")]
    if actual != expected:
        raise RuntimeError(f"Unexpected E6 staging city schema: {actual}; expected {expected}")
    timezone_type = next(
        row[2].upper() for row in db.execute("PRAGMA table_info(city)") if row[1] == "timezone_id"
    )
    if "TEXT" not in timezone_type:
        raise RuntimeError(f"Expected text timezone_id in E6 staging DB, found {timezone_type}")


def table_integrity(db: sqlite3.Connection) -> dict[str, str]:
    results = {}
    for table in NON_FTS_INTEGRITY_TABLES:
        messages = [row[0] for row in db.execute(f"PRAGMA integrity_check('{table}')")]
        if messages != ["ok"]:
            raise RuntimeError(f"SQLite integrity check failed for {table}: {messages}")
        results[table] = "ok"
    return results


def validate_fts(db: sqlite3.Connection, alias_count: int) -> dict[str, object]:
    fts_documents = db.execute("SELECT COUNT(*) FROM city_search_docsize").fetchone()[0]
    orphan_docids = db.execute(
        """SELECT COUNT(*) FROM city_search_docsize f
           LEFT JOIN city_alias a ON a.id=f.docid WHERE a.id IS NULL"""
    ).fetchone()[0]
    missing_docids = db.execute(
        """SELECT COUNT(*) FROM city_alias a
           LEFT JOIN city_search_docsize f ON f.docid=a.id WHERE f.docid IS NULL"""
    ).fetchone()[0]
    if fts_documents != alias_count or orphan_docids or missing_docids:
        raise RuntimeError(
            f"FTS validation failed: docs={fts_documents}, aliases={alias_count}, "
            f"orphans={orphan_docids}, missing={missing_docids}"
        )

    golden_matches = []
    for label, city_id, normalized_alias in GOLDEN_SEARCH_ALIASES:
        rows = db.execute(
            "SELECT id FROM city_alias WHERE city_id=? AND normalized_alias=?",
            (city_id, normalized_alias),
        ).fetchall()
        if len(rows) != 1:
            raise RuntimeError(f"Golden alias {label!r} expected one row, found {len(rows)}")
        expected_docid = rows[0][0]
        matched = {
            row[0]
            for row in db.execute(
                "SELECT docid FROM city_search WHERE city_search MATCH ?",
                (normalized_alias,),
            )
        }
        if expected_docid not in matched:
            raise RuntimeError(f"Golden MATCH failed for {label!r}: expected {expected_docid}")
        golden_matches.append({
            "label": label,
            "geonames_id": city_id,
            "normalized_alias": normalized_alias,
            "expected_docid": expected_docid,
        })
    return {
        "alias_rows": alias_count,
        "fts_documents": fts_documents,
        "orphan_docids": orphan_docids,
        "missing_docids": missing_docids,
        "golden_matches": golden_matches,
    }


def logical_equality(source: sqlite3.Connection, target: sqlite3.Connection) -> dict[str, int]:
    source_cities = source.execute(
        """SELECT id,name,country_code,admin1_code,latitude_e6,longitude_e6,timezone_id,population
           FROM city ORDER BY id"""
    )
    target_cities = target.execute(
        """SELECT c.id,c.name,c.country_code,c.admin1_code,c.latitude_e6,c.longitude_e6,t.name,c.population
           FROM city c JOIN timezone t ON t.id=c.timezone_id ORDER BY c.id"""
    )
    cities = 0
    while True:
        source_row, target_row = source_cities.fetchone(), target_cities.fetchone()
        if source_row is None or target_row is None:
            if source_row != target_row:
                raise RuntimeError("Logical city row-count mismatch")
            break
        cities += 1
        if tuple(source_row) != tuple(target_row):
            raise RuntimeError(f"Logical city mismatch: {tuple(source_row)!r} != {tuple(target_row)!r}")

    source_aliases = source.execute(
        """SELECT id,city_id,normalized_alias,source_kind,language,preferred,short_name
           FROM city_alias ORDER BY id"""
    )
    target_aliases = target.execute(
        """SELECT id,city_id,normalized_alias,source_kind,language,preferred,short_name
           FROM city_alias ORDER BY id"""
    )
    aliases = 0
    while True:
        source_row, target_row = source_aliases.fetchone(), target_aliases.fetchone()
        if source_row is None or target_row is None:
            if source_row != target_row:
                raise RuntimeError("Logical alias row-count mismatch")
            break
        aliases += 1
        if tuple(source_row) != tuple(target_row):
            raise RuntimeError(f"Logical alias mismatch: {tuple(source_row)!r} != {tuple(target_row)!r}")

    duplicates = target.execute(
        """SELECT COUNT(*) FROM (
               SELECT city_id,normalized_alias FROM city_alias
               GROUP BY city_id,normalized_alias HAVING COUNT(*)>1
           )"""
    ).fetchone()[0]
    if duplicates:
        raise RuntimeError(f"Runtime city_alias contains {duplicates} duplicate pairs")
    return {
        "cities_compared": cities,
        "city_mismatches": 0,
        "aliases_compared": aliases,
        "alias_mismatches": 0,
        "duplicate_alias_pairs": duplicates,
    }


def validate_api28_timezone_policy(db: sqlite3.Connection) -> dict[str, object]:
    mapped = {}
    for modern_name, policy in API28_TIMEZONE_COMPATIBILITY.items():
        timezone = db.execute(
            "SELECT id,api28_compat_name FROM timezone WHERE name=?", (modern_name,)
        ).fetchone()
        if timezone is None or timezone[1] != policy["compat_name"]:
            raise RuntimeError(f"API28 compatibility row invalid for {modern_name}: {timezone!r}")
        city_count = db.execute(
            "SELECT COUNT(*) FROM city WHERE timezone_id=?", (timezone[0],)
        ).fetchone()[0]
        expected = EXPECTED_API28_MAPPED_CITY_COUNTS[modern_name]
        if city_count != expected:
            raise RuntimeError(f"Mapped city count changed for {modern_name}: {city_count} != {expected}")
        bad_markers = db.execute(
            "SELECT COUNT(*) FROM city WHERE timezone_id=? AND api28_time_zone_supported<>1",
            (timezone[0],),
        ).fetchone()[0]
        if bad_markers:
            raise RuntimeError(f"Mapped timezone {modern_name} has {bad_markers} unsupported markers")
        mapped[modern_name] = {
            "compat_name": policy["compat_name"],
            "category": policy["category"],
            "cities": city_count,
        }

    compat_rows = db.execute(
        "SELECT name,api28_compat_name FROM timezone WHERE api28_compat_name IS NOT NULL ORDER BY name"
    ).fetchall()
    expected_compat_rows = sorted(
        (name, policy["compat_name"]) for name, policy in API28_TIMEZONE_COMPATIBILITY.items()
    )
    if compat_rows != expected_compat_rows:
        raise RuntimeError(f"Unexpected API28 compatibility rows: {compat_rows!r}")

    unsupported_rows = db.execute(
        """SELECT t.name,COUNT(*) FROM city c JOIN timezone t ON t.id=c.timezone_id
           WHERE c.api28_time_zone_supported=0 GROUP BY t.name ORDER BY t.name"""
    ).fetchall()
    unsupported = {name: count for name, count in unsupported_rows}
    if set(unsupported) != API28_UNSUPPORTED_TIMEZONES:
        raise RuntimeError(f"Unexpected API28 unsupported timezone set: {unsupported!r}")
    unsupported_city_count = sum(unsupported.values())
    if unsupported_city_count != EXPECTED_API28_UNSUPPORTED_CITY_COUNT:
        raise RuntimeError(
            f"API28 unsupported city count changed: {unsupported_city_count} != "
            f"{EXPECTED_API28_UNSUPPORTED_CITY_COUNT}"
        )
    return {
        "mapped": mapped,
        "unsupported_timezones": unsupported,
        "unsupported_city_count": unsupported_city_count,
        "total_city_count": db.execute("SELECT COUNT(*) FROM city").fetchone()[0],
        "peer_verification": {
            "window_start_utc": "2026-01-01T00:00:00Z",
            "window_end_exclusive_utc": "2101-01-01T00:00:00Z",
            "sample_interval_hours": 6,
            "samples_per_pair": 109572,
            "verified_on": "2026-08-30",
            "offset_discrepancies": 0,
        },
    }


def query_contract_validation(db: sqlite3.Connection) -> dict[str, object]:
    find_sql = """SELECT c.id,c.name,a.name,co.name,co.code,c.latitude_e6,c.longitude_e6,t.name,c.population
                  FROM city c JOIN country co ON co.code=c.country_code
                  LEFT JOIN admin1 a ON a.code=c.admin1_code
                  JOIN timezone t ON t.id=c.timezone_id WHERE c.id=?"""
    rome = db.execute(find_sql, (3169070,)).fetchone()
    if rome is None or rome[7] != "Europe/Rome" or not rome[3]:
        raise RuntimeError(f"findById smoke failed for Rome: {rome!r}")

    search_sql = """SELECT c.id,c.name,a.normalized_alias,adm.name,co.name,co.code,
                           c.latitude_e6,c.longitude_e6,t.name,c.population
                    FROM city_search f JOIN city_alias a ON a.id=f.docid
                    JOIN city c ON c.id=a.city_id JOIN country co ON co.code=c.country_code
                    LEFT JOIN admin1 adm ON adm.code=c.admin1_code
                    JOIN timezone t ON t.id=c.timezone_id
                    WHERE city_search MATCH ?
                    ORDER BY CASE WHEN a.normalized_alias=? THEN 0 ELSE 1 END,c.population DESC,c.id
                    LIMIT 20"""
    search_rows = db.execute(search_sql, ("roma*", "roma")).fetchall()
    if not any(row[0] == 3169070 for row in search_rows):
        raise RuntimeError("search smoke failed to return Rome")

    nearest_sql = """SELECT c.id,c.name FROM city c INDEXED BY city_lat_lon_idx
                     WHERE c.latitude_e6 BETWEEN ? AND ? AND c.longitude_e6 BETWEEN ? AND ?
                     ORDER BY ((c.latitude_e6-?)*(c.latitude_e6-?) +
                               (c.longitude_e6-?)*(c.longitude_e6-?)),c.population DESC,c.id LIMIT 1"""
    lat, lon, radius = 41_902_800, 12_496_400, 250_000
    args = (lat-radius, lat+radius, lon-radius, lon+radius, lat, lat, lon, lon)
    nearest = db.execute(nearest_sql, args).fetchone()
    if nearest is None or nearest[0] != 3169070:
        raise RuntimeError(f"nearest smoke expected Rome, got {nearest!r}")
    nearest_plan = [row[3] for row in db.execute("EXPLAIN QUERY PLAN " + nearest_sql, args)]
    search_plan = [row[3] for row in db.execute("EXPLAIN QUERY PLAN " + search_sql, ("roma*", "roma"))]
    if not any("city_lat_lon_idx" in item for item in nearest_plan):
        raise RuntimeError(f"nearest plan missed city_lat_lon_idx: {nearest_plan}")
    if not any("VIRTUAL TABLE INDEX" in item.upper() for item in search_plan):
        raise RuntimeError(f"search plan missed FTS: {search_plan}")

    def best_ms(sql: str, args: tuple) -> float:
        values = []
        for _ in range(5):
            start = time.perf_counter_ns()
            db.execute(sql, args).fetchall()
            values.append((time.perf_counter_ns() - start) / 1_000_000)
        return min(values)

    return {
        "find_by_id": {"geonames_id": rome[0], "timezone": rome[7], "best_of_5_ms": best_ms(find_sql, (3169070,))},
        "search": {"query": "roma*", "returned_rows": len(search_rows), "plan": search_plan, "best_of_5_ms": best_ms(search_sql, ("roma*", "roma"))},
        "nearest": {"result_geonames_id": nearest[0], "plan": nearest_plan, "best_of_5_ms": best_ms(nearest_sql, args)},
    }


def build_runtime_minimal(source: sqlite3.Connection, output: Path) -> sqlite3.Connection:
    if output.exists():
        output.unlink()
    output.parent.mkdir(parents=True, exist_ok=True)
    target = sqlite3.connect(output)
    target.execute("PRAGMA page_size=4096")
    target.execute("PRAGMA journal_mode=OFF")
    target.execute("PRAGMA synchronous=OFF")
    target.execute("PRAGMA temp_store=MEMORY")
    target.executescript(
        """
        CREATE TABLE country(code TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL) WITHOUT ROWID;
        CREATE TABLE admin1(code TEXT PRIMARY KEY NOT NULL,country_code TEXT NOT NULL,name TEXT NOT NULL) WITHOUT ROWID;
        CREATE TABLE timezone(
            id INTEGER PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            api28_compat_name TEXT
        );
        CREATE TABLE city(
            id INTEGER PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            country_code TEXT NOT NULL,
            admin1_code TEXT,
            latitude_e6 INTEGER NOT NULL,
            longitude_e6 INTEGER NOT NULL,
            timezone_id INTEGER NOT NULL,
            population INTEGER NOT NULL,
            api28_time_zone_supported INTEGER NOT NULL CHECK(api28_time_zone_supported IN (0,1))
        );
        CREATE INDEX city_lat_lon_idx ON city(latitude_e6,longitude_e6);
        CREATE TABLE city_alias(
            id INTEGER PRIMARY KEY NOT NULL,
            city_id INTEGER NOT NULL,
            normalized_alias TEXT NOT NULL,
            source_kind INTEGER NOT NULL,
            language TEXT,
            preferred INTEGER NOT NULL DEFAULT 0,
            short_name INTEGER NOT NULL DEFAULT 0
        );
        """
    )

    timezones = [row[0] for row in source.execute("SELECT DISTINCT timezone_id FROM city ORDER BY timezone_id")]
    timezone_ids = {name: index + 1 for index, name in enumerate(timezones)}
    with target:
        target.executemany("INSERT INTO country(code,name) VALUES(?,?)", source.execute("SELECT code,name FROM country ORDER BY code"))
        target.executemany(
            "INSERT INTO admin1(code,country_code,name) VALUES(?,?,?)",
            source.execute("SELECT code,country_code,name FROM admin1 ORDER BY code"),
        )
        target.executemany(
            "INSERT INTO timezone(id,name,api28_compat_name) VALUES(?,?,?)",
            (
                (timezone_ids[name], name, API28_TIMEZONE_COMPATIBILITY.get(name, {}).get("compat_name"))
                for name in timezones
            ),
        )
        batch = []
        for row in source.execute(
            """SELECT id,name,country_code,admin1_code,latitude_e6,longitude_e6,timezone_id,population
               FROM city ORDER BY id"""
        ):
            batch.append((*row[:6], timezone_ids[row[6]], row[7], 0 if row[6] in API28_UNSUPPORTED_TIMEZONES else 1))
            if len(batch) >= 5000:
                target.executemany(
                    """INSERT INTO city(id,name,country_code,admin1_code,latitude_e6,longitude_e6,
                                         timezone_id,population,api28_time_zone_supported)
                       VALUES(?,?,?,?,?,?,?,?,?)""",
                    batch,
                )
                batch.clear()
        if batch:
            target.executemany(
                """INSERT INTO city(id,name,country_code,admin1_code,latitude_e6,longitude_e6,
                                     timezone_id,population,api28_time_zone_supported)
                   VALUES(?,?,?,?,?,?,?,?,?)""",
                batch,
            )

        batch = []
        for row in source.execute(
            """SELECT id,city_id,normalized_alias,source_kind,language,preferred,short_name
               FROM city_alias ORDER BY id"""
        ):
            batch.append(tuple(row))
            if len(batch) >= 5000:
                target.executemany(
                    "INSERT INTO city_alias(id,city_id,normalized_alias,source_kind,language,preferred,short_name) VALUES(?,?,?,?,?,?,?)",
                    batch,
                )
                batch.clear()
        if batch:
            target.executemany(
                "INSERT INTO city_alias(id,city_id,normalized_alias,source_kind,language,preferred,short_name) VALUES(?,?,?,?,?,?,?)",
                batch,
            )
        target.execute("CREATE VIRTUAL TABLE city_search USING fts4(normalized_alias, content='')")
        target.execute("INSERT INTO city_search(docid,normalized_alias) SELECT id,normalized_alias FROM city_alias ORDER BY id")
        target.execute("ANALYZE")
    target.execute("VACUUM")
    return target


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--metadata", type=Path, required=True)
    parser.add_argument("--expected-source-sha256")
    parser.add_argument("--source-metadata", type=Path)
    args = parser.parse_args()

    if not args.source.is_file():
        raise FileNotFoundError(args.source)
    source_sha = sha256(args.source)
    if args.expected_source_sha256 and source_sha != args.expected_source_sha256:
        raise RuntimeError(f"Staging DB SHA mismatch: {source_sha} != {args.expected_source_sha256}")

    source = sqlite3.connect(f"file:{args.source}?mode=ro", uri=True)
    validate_source_schema(source)
    source_city_count = source.execute("SELECT COUNT(*) FROM city").fetchone()[0]
    source_alias_count = source.execute("SELECT COUNT(*) FROM city_alias").fetchone()[0]
    source_timezone_count = source.execute("SELECT COUNT(DISTINCT timezone_id) FROM city").fetchone()[0]

    target = build_runtime_minimal(source, args.output)
    target_city_count = target.execute("SELECT COUNT(*) FROM city").fetchone()[0]
    target_alias_count = target.execute("SELECT COUNT(*) FROM city_alias").fetchone()[0]
    target_timezone_count = target.execute("SELECT COUNT(*) FROM timezone").fetchone()[0]
    if (target_city_count, target_alias_count, target_timezone_count) != (
        source_city_count, source_alias_count, source_timezone_count
    ):
        raise RuntimeError("Runtime-minimal city/alias/timezone count changed")

    orphan_timezones = target.execute(
        "SELECT COUNT(*) FROM city c LEFT JOIN timezone t ON t.id=c.timezone_id WHERE t.id IS NULL"
    ).fetchone()[0]
    invalid_coordinates = target.execute(
        """SELECT COUNT(*) FROM city
           WHERE latitude_e6 < -90000000 OR latitude_e6 > 90000000
              OR longitude_e6 < -180000000 OR longitude_e6 > 180000000"""
    ).fetchone()[0]
    if orphan_timezones or invalid_coordinates:
        raise RuntimeError(f"Invalid city rows: orphan_timezones={orphan_timezones}, invalid_coordinates={invalid_coordinates}")

    logical = logical_equality(source, target)
    integrity = table_integrity(target)
    fts = validate_fts(target, target_alias_count)
    query_contract = query_contract_validation(target)
    api28_timezone = validate_api28_timezone_policy(target)
    runtime_indexes = [
        row[0] for row in target.execute(
            "SELECT name FROM sqlite_master WHERE type='index' AND sql IS NOT NULL ORDER BY name"
        )
    ]
    if runtime_indexes != ["city_lat_lon_idx"]:
        raise RuntimeError(f"Unexpected explicit runtime indexes: {runtime_indexes}")

    source_metadata = json.loads(args.source_metadata.read_text()) if args.source_metadata else None
    metadata = {
        "optimizer_version": OPTIMIZER_VERSION,
        "sqlite_version": sqlite3.sqlite_version,
        "profile": "runtime-minimal",
        "source": {
            "database_sha256": source_sha,
            "database_bytes": args.source.stat().st_size,
            "metadata": source_metadata,
        },
        "output": {"sha256": sha256(args.output), "bytes": args.output.stat().st_size},
        "counts": {
            "cities": target_city_count,
            "aliases": target_alias_count,
            "timezones": target_timezone_count,
            "orphan_timezone_ids": orphan_timezones,
            "invalid_coordinates": invalid_coordinates,
        },
        "storage": {
            "coordinates": "signed integer microdegrees (E6)",
            "timezone": "modern IANA timezone.name plus explicit nullable api28_compat_name",
            "api28_city_support_marker": "city.api28_time_zone_supported (0/1 baseline capability marker)",
            "explicit_runtime_indexes": runtime_indexes,
            "alias_deduplication": "inherited from validated build-time staging; no runtime UNIQUE/index",
            "fts": "FTS4 contentless, docid=city_alias.id",
        },
        "validation": {
            "logical_equality": logical,
            "non_fts_integrity": integrity,
            "city_search": fts,
            "query_contract": query_contract,
            "api28_timezone": api28_timezone,
        },
    }
    args.metadata.parent.mkdir(parents=True, exist_ok=True)
    args.metadata.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n")
    print(json.dumps(metadata, indent=2, sort_keys=True))
    target.close()
    source.close()


if __name__ == "__main__":
    main()
