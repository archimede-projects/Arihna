#!/usr/bin/env python3
"""Build Arihna's runtime-minimal city database from a validated E6 staging DB.

This is a lossless packaging transform. It preserves every city and alias from
an already filtered/deduplicated GeoNames staging database while removing
preprocessing-only indexes, normalizing repeated timezone strings, and retaining
only the coordinate index required by CityRepository.nearest().
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import time
from pathlib import Path

OPTIMIZER_VERSION = 1
NON_FTS_INTEGRITY_TABLES = ("country", "admin1", "timezone", "city", "city_alias")
GOLDEN_SEARCH_ALIASES = (
    ("Roma", 3169070, "roma"),
    ("Makkah", 104515, "makkah"),
    ("Mecca", 104515, "mecca"),
    ("New York", 5128581, "new york"),
    ("Sydney", 2147714, "sydney"),
)


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def validate_source_schema(db: sqlite3.Connection) -> None:
    expected_city_columns = [
        "id", "name", "country_code", "admin1_code", "latitude_e6",
        "longitude_e6", "timezone_id", "population",
    ]
    actual_city_columns = [row[1] for row in db.execute("PRAGMA table_info(city)")]
    if actual_city_columns != expected_city_columns:
        raise RuntimeError(
            f"Unexpected E6 staging city schema: {actual_city_columns}; expected {expected_city_columns}"
        )
    source_timezone_type = next(
        row[2].upper() for row in db.execute("PRAGMA table_info(city)") if row[1] == "timezone_id"
    )
    if "TEXT" not in source_timezone_type:
        raise RuntimeError(f"Expected text timezone_id in E6 staging DB, found {source_timezone_type}")


def table_integrity(db: sqlite3.Connection) -> dict[str, str]:
    results: dict[str, str] = {}
    for table in NON_FTS_INTEGRITY_TABLES:
        messages = [row[0] for row in db.execute(f"PRAGMA integrity_check('{table}')")]
        if messages != ["ok"]:
            raise RuntimeError(f"SQLite integrity check failed for {table}: {messages}")
        results[table] = "ok"
    return results


def validate_fts(db: sqlite3.Connection, alias_count: int) -> dict[str, object]:
    fts_documents = db.execute("SELECT COUNT(*) FROM city_search_docsize").fetchone()[0]
    orphan_docids = db.execute(
        """
        SELECT COUNT(*)
        FROM city_search_docsize AS f
        LEFT JOIN city_alias AS a ON a.id = f.docid
        WHERE a.id IS NULL
        """
    ).fetchone()[0]
    missing_docids = db.execute(
        """
        SELECT COUNT(*)
        FROM city_alias AS a
        LEFT JOIN city_search_docsize AS f ON f.docid = a.id
        WHERE f.docid IS NULL
        """
    ).fetchone()[0]
    if fts_documents != alias_count:
        raise RuntimeError(f"FTS document count mismatch: {fts_documents} != {alias_count}")
    if orphan_docids:
        raise RuntimeError(f"FTS has {orphan_docids} orphan docids")
    if missing_docids:
        raise RuntimeError(f"FTS is missing {missing_docids} alias docids")

    golden_matches: list[dict[str, object]] = []
    for label, city_id, normalized_alias in GOLDEN_SEARCH_ALIASES:
        alias_rows = db.execute(
            "SELECT id FROM city_alias WHERE city_id=? AND normalized_alias=?",
            (city_id, normalized_alias),
        ).fetchall()
        if len(alias_rows) != 1:
            raise RuntimeError(
                f"Golden alias {label!r} expected one alias for city {city_id}, found {len(alias_rows)}"
            )
        expected_docid = alias_rows[0][0]
        matched = {
            row[0]
            for row in db.execute(
                "SELECT docid FROM city_search WHERE city_search MATCH ?",
                (normalized_alias,),
            )
        }
        if expected_docid not in matched:
            raise RuntimeError(
                f"Golden MATCH failed for {label!r}: expected docid {expected_docid}"
            )
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
        """
        SELECT id,name,country_code,admin1_code,latitude_e6,longitude_e6,timezone_id,population
        FROM city ORDER BY id
        """
    )
    target_cities = target.execute(
        """
        SELECT c.id,c.name,c.country_code,c.admin1_code,c.latitude_e6,c.longitude_e6,t.name,c.population
        FROM city AS c
        JOIN timezone AS t ON t.id=c.timezone_id
        ORDER BY c.id
        """
    )
    city_rows = 0
    while True:
        source_row = source_cities.fetchone()
        target_row = target_cities.fetchone()
        if source_row is None or target_row is None:
            if source_row != target_row:
                raise RuntimeError("Logical city row-count mismatch")
            break
        city_rows += 1
        if tuple(source_row) != tuple(target_row):
            raise RuntimeError(
                f"Logical city mismatch at source={tuple(source_row)!r}, target={tuple(target_row)!r}"
            )

    source_aliases = source.execute(
        """
        SELECT id,city_id,normalized_alias,source_kind,language,preferred,short_name
        FROM city_alias ORDER BY id
        """
    )
    target_aliases = target.execute(
        """
        SELECT id,city_id,normalized_alias,source_kind,language,preferred,short_name
        FROM city_alias ORDER BY id
        """
    )
    alias_rows = 0
    while True:
        source_row = source_aliases.fetchone()
        target_row = target_aliases.fetchone()
        if source_row is None or target_row is None:
            if source_row != target_row:
                raise RuntimeError("Logical alias row-count mismatch")
            break
        alias_rows += 1
        if tuple(source_row) != tuple(target_row):
            raise RuntimeError(
                f"Logical alias mismatch at source={tuple(source_row)!r}, target={tuple(target_row)!r}"
            )

    duplicate_alias_pairs = target.execute(
        """
        SELECT COUNT(*) FROM (
            SELECT city_id,normalized_alias
            FROM city_alias
            GROUP BY city_id,normalized_alias
            HAVING COUNT(*) > 1
        )
        """
    ).fetchone()[0]
    if duplicate_alias_pairs:
        raise RuntimeError(f"Runtime city_alias contains {duplicate_alias_pairs} duplicate pairs")

    return {
        "cities_compared": city_rows,
        "city_mismatches": 0,
        "aliases_compared": alias_rows,
        "alias_mismatches": 0,
        "duplicate_alias_pairs": duplicate_alias_pairs,
    }


def query_contract_validation(db: sqlite3.Connection) -> dict[str, object]:
    find_sql = """
        SELECT c.id,c.name,a.name AS region_name,co.name AS country_name,co.code,
               c.latitude_e6,c.longitude_e6,t.name,c.population
        FROM city AS c
        JOIN country AS co ON co.code=c.country_code
        LEFT JOIN admin1 AS a ON a.code=c.admin1_code
        JOIN timezone AS t ON t.id=c.timezone_id
        WHERE c.id=?
    """
    rome = db.execute(find_sql, (3169070,)).fetchone()
    if rome is None or rome[7] != "Europe/Rome" or not rome[3]:
        raise RuntimeError(f"findById smoke failed for Rome: {rome!r}")

    search_sql = """
        SELECT c.id,c.name,a.normalized_alias,adm.name,co.name,co.code,
               c.latitude_e6,c.longitude_e6,t.name,c.population
        FROM city_search AS f
        JOIN city_alias AS a ON a.id=f.docid
        JOIN city AS c ON c.id=a.city_id
        JOIN country AS co ON co.code=c.country_code
        LEFT JOIN admin1 AS adm ON adm.code=c.admin1_code
        JOIN timezone AS t ON t.id=c.timezone_id
        WHERE city_search MATCH ?
        ORDER BY CASE WHEN a.normalized_alias=? THEN 0 ELSE 1 END,
                 c.population DESC,c.id
        LIMIT 20
    """
    search_rows = db.execute(search_sql, ("roma*", "roma")).fetchall()
    if not any(row[0] == 3169070 for row in search_rows):
        raise RuntimeError("search smoke failed to return Rome for roma*")

    nearest_sql = """
        SELECT c.id,c.name,adm.name,co.name,co.code,c.latitude_e6,c.longitude_e6,t.name,c.population
        FROM city AS c INDEXED BY city_lat_lon_idx
        JOIN country AS co ON co.code=c.country_code
        LEFT JOIN admin1 AS adm ON adm.code=c.admin1_code
        JOIN timezone AS t ON t.id=c.timezone_id
        WHERE c.latitude_e6 BETWEEN ? AND ?
          AND c.longitude_e6 BETWEEN ? AND ?
        ORDER BY ((c.latitude_e6-?)*(c.latitude_e6-?) +
                  (c.longitude_e6-?)*(c.longitude_e6-?)) ASC,
                 c.population DESC,c.id
        LIMIT 1
    """
    lat = 41_902_800
    lon = 12_496_400
    radius = 250_000
    nearest_args = (lat-radius, lat+radius, lon-radius, lon+radius, lat, lat, lon, lon)
    nearest = db.execute(nearest_sql, nearest_args).fetchone()
    if nearest is None or nearest[0] != 3169070:
        raise RuntimeError(f"nearest smoke expected Rome, got {nearest!r}")

    nearest_plan = [row[3] for row in db.execute("EXPLAIN QUERY PLAN " + nearest_sql, nearest_args)]
    if not any("city_lat_lon_idx" in detail for detail in nearest_plan):
        raise RuntimeError(f"nearest query plan did not use city_lat_lon_idx: {nearest_plan}")
    search_plan = [row[3] for row in db.execute("EXPLAIN QUERY PLAN " + search_sql, ("roma*", "roma"))]
    if not any("VIRTUAL TABLE INDEX" in detail.upper() for detail in search_plan):
        raise RuntimeError(f"search query plan did not use FTS virtual table: {search_plan}")

    def best_ms(sql: str, args: tuple, repetitions: int = 5) -> float:
        values = []
        for _ in range(repetitions):
            start = time.perf_counter_ns()
            db.execute(sql, args).fetchall()
            values.append((time.perf_counter_ns() - start) / 1_000_000)
        return min(values)

    return {
        "find_by_id": {
            "geonames_id": rome[0], "name": rome[1], "region_name": rome[2],
            "country_name": rome[3], "country_code": rome[4], "timezone": rome[7],
            "best_of_5_ms": best_ms(find_sql, (3169070,)),
        },
        "search": {
            "query": "roma*", "returned_rows": len(search_rows), "rome_present": True,
            "plan": search_plan, "best_of_5_ms": best_ms(search_sql, ("roma*", "roma")),
        },
        "nearest": {
            "query_latitude_e6": lat, "query_longitude_e6": lon,
            "result_geonames_id": nearest[0], "result_name": nearest[1],
            "plan": nearest_plan, "best_of_5_ms": best_ms(nearest_sql, nearest_args),
        },
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
        CREATE TABLE country(
            code TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL
        ) WITHOUT ROWID;
        CREATE TABLE admin1(
            code TEXT PRIMARY KEY NOT NULL,
            country_code TEXT NOT NULL,
            name TEXT NOT NULL
        ) WITHOUT ROWID;
        CREATE TABLE timezone(
            id INTEGER PRIMARY KEY NOT NULL,
            name TEXT NOT NULL
        );
        CREATE TABLE city(
            id INTEGER PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            country_code TEXT NOT NULL,
            admin1_code TEXT,
            latitude_e6 INTEGER NOT NULL,
            longitude_e6 INTEGER NOT NULL,
            timezone_id INTEGER NOT NULL,
            population INTEGER NOT NULL
        );
        CREATE INDEX city_lat_lon_idx ON city(latitude_e6, longitude_e6);
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
        target.executemany(
            "INSERT INTO country(code,name) VALUES(?,?)",
            source.execute("SELECT code,name FROM country ORDER BY code"),
        )
        target.executemany(
            "INSERT INTO admin1(code,country_code,name) VALUES(?,?,?)",
            source.execute("SELECT code,country_code,name FROM admin1 ORDER BY code"),
        )
        target.executemany(
            "INSERT INTO timezone(id,name) VALUES(?,?)",
            ((timezone_ids[name], name) for name in timezones),
        )

        city_batch = []
        for row in source.execute(
            """
            SELECT id,name,country_code,admin1_code,latitude_e6,longitude_e6,timezone_id,population
            FROM city ORDER BY id
            """
        ):
            city_batch.append((*row[:6], timezone_ids[row[6]], row[7]))
            if len(city_batch) >= 5000:
                target.executemany(
                    "INSERT INTO city(id,name,country_code,admin1_code,latitude_e6,longitude_e6,timezone_id,population) VALUES(?,?,?,?,?,?,?,?)",
                    city_batch,
                )
                city_batch.clear()
        if city_batch:
            target.executemany(
                "INSERT INTO city(id,name,country_code,admin1_code,latitude_e6,longitude_e6,timezone_id,population) VALUES(?,?,?,?,?,?,?,?)",
                city_batch,
            )

        alias_batch = []
        for row in source.execute(
            """
            SELECT id,city_id,normalized_alias,source_kind,language,preferred,short_name
            FROM city_alias ORDER BY id
            """
        ):
            alias_batch.append(tuple(row))
            if len(alias_batch) >= 5000:
                target.executemany(
                    "INSERT INTO city_alias(id,city_id,normalized_alias,source_kind,language,preferred,short_name) VALUES(?,?,?,?,?,?,?)",
                    alias_batch,
                )
                alias_batch.clear()
        if alias_batch:
            target.executemany(
                "INSERT INTO city_alias(id,city_id,normalized_alias,source_kind,language,preferred,short_name) VALUES(?,?,?,?,?,?,?)",
                alias_batch,
            )

        target.execute("CREATE VIRTUAL TABLE city_search USING fts4(normalized_alias, content='')")
        target.execute(
            "INSERT INTO city_search(docid, normalized_alias) SELECT id, normalized_alias FROM city_alias ORDER BY id"
        )
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
    if target_city_count != source_city_count:
        raise RuntimeError(f"City count changed: {source_city_count} -> {target_city_count}")
    if target_alias_count != source_alias_count:
        raise RuntimeError(f"Alias count changed: {source_alias_count} -> {target_alias_count}")
    if target_timezone_count != source_timezone_count:
        raise RuntimeError(f"Timezone count changed: {source_timezone_count} -> {target_timezone_count}")

    orphan_timezones = target.execute(
        "SELECT COUNT(*) FROM city AS c LEFT JOIN timezone AS t ON t.id=c.timezone_id WHERE t.id IS NULL"
    ).fetchone()[0]
    if orphan_timezones:
        raise RuntimeError(f"city has {orphan_timezones} orphan timezone ids")
    invalid_coordinates = target.execute(
        """
        SELECT COUNT(*) FROM city
        WHERE latitude_e6 < -90000000 OR latitude_e6 > 90000000
           OR longitude_e6 < -180000000 OR longitude_e6 > 180000000
        """
    ).fetchone()[0]
    if invalid_coordinates:
        raise RuntimeError(f"city has {invalid_coordinates} invalid E6 coordinate rows")

    logical = logical_equality(source, target)
    integrity = table_integrity(target)
    fts = validate_fts(target, target_alias_count)
    query_contract = query_contract_validation(target)

    runtime_indexes = [
        row[0] for row in target.execute(
            "SELECT name FROM sqlite_master WHERE type='index' AND sql IS NOT NULL ORDER BY name"
        )
    ]
    if runtime_indexes != ["city_lat_lon_idx"]:
        raise RuntimeError(f"Unexpected explicit runtime indexes: {runtime_indexes}")

    source_metadata = None
    if args.source_metadata:
        source_metadata = json.loads(args.source_metadata.read_text(encoding="utf-8"))

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
            "timezone": "numeric city.timezone_id -> timezone.name",
            "explicit_runtime_indexes": runtime_indexes,
            "alias_deduplication": "inherited from validated build-time staging; no runtime UNIQUE/index",
            "fts": "FTS4 contentless, docid=city_alias.id",
        },
        "validation": {
            "logical_equality": logical,
            "non_fts_integrity": integrity,
            "city_search": fts,
            "query_contract": query_contract,
        },
    }
    args.metadata.parent.mkdir(parents=True, exist_ok=True)
    args.metadata.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2, sort_keys=True))
    target.close()
    source.close()


if __name__ == "__main__":
    main()
