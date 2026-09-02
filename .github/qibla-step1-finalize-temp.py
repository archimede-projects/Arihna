from pathlib import Path

p = Path('PROJECT_SPEC.md')
text = p.read_text(encoding='utf-8')

replacements = [
    (
        """- Android orientation sensors may report orientation against **magnetic north**. Arihna must never compare a magnetic heading directly with the true-north Qibla bearing without correction.\n- For sensor paths whose heading is magnetic, calculate declination with Android `android.hardware.GeomagneticField` using the exact active `SelectedLocation.coordinates`, the injected/testable current time, and altitude `0 m` because altitude is not part of Arihna's closed Location contract. The `0 m` value is an explicit geomagnetic-model approximation only; it is not a fabricated user location and must not propagate into Location or Prayer state.""",
        """- Android orientation sources may be **direct true-north** or **magnetic-north** sources. Arihna must keep the reference explicit and must never apply magnetic declination twice.\n- On API 33+ (`Build.VERSION_CODES.TIRAMISU` and newer), prefer `Sensor.TYPE_HEADING` when the runtime actually exposes it. Android defines this sensor as the direction the device is pointing relative to **true north**; `values[0]` is heading in degrees and `values[1]` is heading accuracy in degrees. This path uses the reported true heading directly and does **not** apply `GeomagneticField` correction.\n- For fallback sensor paths whose heading is magnetic, calculate declination with Android `android.hardware.GeomagneticField` using the exact active `SelectedLocation.coordinates`, the injected/testable current time, and altitude `0 m` because altitude is not part of Arihna's closed Location contract. The `0 m` value is an explicit geomagnetic-model approximation only; it is not a fabricated user location and must not propagate into Location or Prayer state.""",
    ),
    (
        """Preferred foreground sensor hierarchy:\n\n1. `Sensor.TYPE_ROTATION_VECTOR` — preferred when available. Android documents its earth reference as approximately east / magnetic north / sky and exposes estimated heading accuracy in `SensorEvent.values[4]` when available.\n2. `Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR` — controlled fallback when the normal rotation vector is unavailable. It is lower-power/lower-accuracy and does not use the gyroscope, but retains a geomagnetic north reference.\n3. Calibrated `TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD` with `SensorManager.getRotationMatrix()`, `remapCoordinateSystem()` and `getOrientation()` — final compatibility fallback when neither rotation-vector sensor exists.""",
        """Preferred foreground sensor hierarchy:\n\n1. **API 33+ `Sensor.TYPE_HEADING`**, when actually available — preferred because Android reports heading directly relative to true north and includes a degree accuracy estimate. Runtime capability detection remains mandatory; API level alone does not guarantee that the physical sensor exists.\n2. `Sensor.TYPE_ROTATION_VECTOR` — preferred magnetic-reference fallback. Android documents its earth reference as approximately east / magnetic north / sky and exposes estimated heading accuracy in `SensorEvent.values[4]` when available; convert to true north with the approved declination path.\n3. `Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR` — controlled fallback when the normal rotation vector is unavailable. It is lower-power/lower-accuracy and does not use the gyroscope, but retains a geomagnetic north reference; convert to true north with declination.\n4. Calibrated `TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD` with `SensorManager.getRotationMatrix()`, `remapCoordinateSystem()` and `getOrientation()` — final compatibility fallback when no heading/rotation-vector path exists.""",
    ),
    (
        """- Do not use deprecated `Sensor.TYPE_ORIENTATION`.\n- Do not use `TYPE_GAME_ROTATION_VECTOR` for Qibla heading because Android documents that it deliberately does not use the geomagnetic field and its north reference may drift.""",
        """- Do not use deprecated `Sensor.TYPE_ORIENTATION`.\n- `TYPE_HEADING` was added in API 33; the implementation must be SDK-guarded and capability-checked so API28 remains fully supported through the documented fallback hierarchy.\n- Do not use `TYPE_GAME_ROTATION_VECTOR` for Qibla heading because Android documents that it deliberately does not use the geomagnetic field and its north reference may drift.""",
    ),
    (
        """- Convert the selected sensor to a rotation matrix and derive azimuth with `SensorManager.getOrientation()` after remapping axes for the current display rotation. Portrait/landscape changes must not silently rotate the compass reference.\n- Normalize magnetic and true headings to `[0°, 360°)`.""",
        """- For `TYPE_HEADING`, consume the direct true-north heading/accuracy values without converting them through a magnetic rotation matrix. For rotation-vector and accelerometer+magnetometer fallbacks, derive azimuth through the appropriate rotation matrix and `SensorManager.getOrientation()`.\n- Android sensor coordinates are based on the device's natural orientation. Any sensor value mapped to an on-screen compass must account for the current display rotation; use `getRotation()` / `remapCoordinateSystem()` where applicable so portrait/landscape changes do not silently rotate the compass reference.\n- Normalize magnetic and true headings to `[0°, 360°)`.""",
    ),
    (
        """- For `TYPE_ROTATION_VECTOR`, retain Android's `values[4]` estimated heading accuracy in radians/degrees when available (`-1` means unavailable); it may be displayed as secondary diagnostic guidance but must not be converted into a fabricated platform accuracy category.""",
        """- For `TYPE_HEADING`, retain Android's direct `values[1]` accuracy in degrees. For `TYPE_ROTATION_VECTOR`, retain `values[4]` estimated heading accuracy in radians/degrees when available (`-1` means unavailable). Numeric accuracy may be displayed as secondary guidance but must not be converted into a fabricated platform accuracy category.""",
    ),
    (
        """DeviceHeadingState\n- Unavailable(reason)\n- Reading(\n    magneticHeadingDegrees,\n    trueHeadingDegrees,\n    declinationDegrees,\n    quality,\n    estimatedAccuracyDegrees?\n  )""",
        """DeviceHeadingState\n- Unavailable(reason)\n- Reading(\n    trueHeadingDegrees,\n    quality,\n    estimatedAccuracyDegrees?,\n    source,\n    magneticHeadingDegrees?,\n    declinationDegrees?\n  )\n\nHeadingSource\n- TRUE_HEADING_SENSOR\n- ROTATION_VECTOR\n- GEOMAGNETIC_ROTATION_VECTOR\n- ACCELEROMETER_MAGNETIC_FIELD""",
    ),
    (
        """- magnetic-to-true correction with positive and negative declination;\n- Device Ready and CACHED Ready use exact accepted coordinates without a fresh-location request;""",
        """- direct `TYPE_HEADING` true-north input is not declination-corrected a second time;\n- magnetic-to-true fallback correction with positive and negative declination;\n- sensor-source fallback selection is deterministic and API33 `TYPE_HEADING` absence falls through safely;\n- Device Ready and CACHED Ready use exact accepted coordinates without a fresh-location request;""",
    ),
    (
        """- fallback selection order is deterministic;\n- no deprecated orientation sensor path;""",
        """- fallback selection order is deterministic, with API28 exercising the no-`TYPE_HEADING` compatibility path;\n- no deprecated orientation sensor path;""",
    ),
    (
        """- device-source Qibla screen opens without requesting new permissions;\n- compass responds smoothly to real rotation and crosses north without a 360° jump;""",
        """- device-source Qibla screen opens without requesting new permissions;\n- validation records which heading source the S25 actually exposes/selects, preferring `TYPE_HEADING` when available;\n- compass responds smoothly to real rotation and crosses north without a 360° jump;""",
    ),
    (
        """Evidence basis reviewed for STEP 1 on 2026-09-02: Android Developers documentation for `Sensor`, `SensorEvent`, `SensorManager`, rotation-vector/position sensors and `GeomagneticField`; GeoNames Kaaba shrine record for the frozen target coordinates. The Android documentation states that rotation-vector orientation references magnetic north, `TYPE_GAME_ROTATION_VECTOR` omits geomagnetic north and may drift, sensor accuracy status is explicitly reported, and `GeomagneticField.getDeclination()` estimates magnetic declination from true north.""",
        """Evidence basis reviewed for STEP 1 on 2026-09-02: Android Developers documentation for `Sensor`, `SensorEvent`, `SensorManager`, sensor coordinate/display remapping, `TYPE_HEADING`, rotation-vector/position sensors and `GeomagneticField`; GeoNames Kaaba shrine record for the frozen target coordinates. Android documents `TYPE_HEADING` (API 33+) as direct true-north heading with degree accuracy, rotation-vector orientation as magnetic-north referenced, `TYPE_GAME_ROTATION_VECTOR` as omitting geomagnetic north and potentially drifting, and `GeomagneticField.getDeclination()` as the magnetic-to-true-north declination estimate.""",
    ),
]

for old, new in replacements:
    if old not in text:
        raise SystemExit(f'missing replacement source: {old[:90]!r}')
    text = text.replace(old, new, 1)

p.write_text(text, encoding='utf-8')
