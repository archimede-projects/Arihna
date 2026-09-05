#!/usr/bin/env bash
set -euo pipefail

PKG=com.archimedeprojects.arihna
CLASS=com.archimedeprojects.arihna.feature.alarms.platform.OverlayVolumeModernAndroidTest
TEST_SRC=app/src/androidTest/java/com/archimedeprojects/arihna/feature/alarms/platform/OverlayVolumeModernAndroidTest.kt

cp /tmp/OverlayVolumeModernAndroidTest.kt "$TEST_SRC"
test -f "$TEST_SRC"
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --stacktrace

APK=app/build/outputs/apk/debug/app-debug.apk
TEST_APK=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
test -f "$APK"; test -f "$TEST_APK"
adb install -r "$APK"
adb install -r "$TEST_APK"

RUNNER="$(adb shell pm list instrumentation | tr -d '\r' | sed -n "s/^instrumentation:\([^ ]*\) (target=$PKG)$/\1/p" | head -1)"
test -n "$RUNNER"

run_matrix() {
  local expected_exact="$1"
  local expected_notification="$2"
  local expected_fullscreen="$3"
  local expected_overlay="$4"
  local label="$5"
  local log="/tmp/overlay-volume-modern-${label}.txt"
  adb shell am force-stop "$PKG" || true
  adb shell am instrument -w -r \
    -e class "$CLASS" \
    -e expectedExact "$expected_exact" \
    -e expectedNotification "$expected_notification" \
    -e expectedFullScreen "$expected_fullscreen" \
    -e expectedOverlay "$expected_overlay" \
    "$RUNNER" | tee "$log"
  grep -Eq '^OK \(1 test\)|^OK \(1 tests\)' "$log"
  ! grep -q 'FAILURES!!!' "$log"
}

adb shell appops set "$PKG" SCHEDULE_EXACT_ALARM deny || adb shell appops set "$PKG" SCHEDULE_EXACT_ALARM ignore
adb shell pm revoke "$PKG" android.permission.POST_NOTIFICATIONS || true
adb shell appops set "$PKG" USE_FULL_SCREEN_INTENT deny
adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW deny || adb shell appops set "$PKG" android:system_alert_window deny
run_matrix false false false false denied

adb shell appops set "$PKG" SCHEDULE_EXACT_ALARM allow
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS
adb shell appops set "$PKG" USE_FULL_SCREEN_INTENT allow
adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow || adb shell appops set "$PKG" android:system_alert_window allow
run_matrix true true true true granted

rm -f "$TEST_SRC"
echo 'OVERLAY_VOLUME_MODERN_MATRIX PASS api=36 exact=denied+granted notifications=denied+granted fullscreen=denied+granted overlay=denied+granted platform_notification=true'
