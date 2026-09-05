#!/usr/bin/env bash
set -euo pipefail

PKG=com.archimedeprojects.arihna
CLASS=com.archimedeprojects.arihna.feature.alarms.platform.Step7ModernAndroidTest
TEST_SRC=app/src/androidTest/java/com/archimedeprojects/arihna/feature/alarms/platform/Step7ModernAndroidTest.kt

cp /tmp/Step7ModernAndroidTest.kt "$TEST_SRC"
test -f "$TEST_SRC"
echo "STEP7_TEMP_TEST_SHA256=$(sha256sum "$TEST_SRC" | awk '{print $1}')"

./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --stacktrace

APK=app/build/outputs/apk/debug/app-debug.apk
TEST_APK=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
test -f "$APK"
test -f "$TEST_APK"
adb install -r "$APK"
adb install -r "$TEST_APK"

RUNNER="$(adb shell pm list instrumentation | tr -d '\r' | sed -n "s/^instrumentation:\([^ ]*\) (target=$PKG)$/\1/p" | head -1)"
test -n "$RUNNER"
echo "STEP7_MODERN_RUNNER=$RUNNER"

run_matrix() {
  local expected_exact="$1"
  local expected_notification="$2"
  local label="$3"
  local log="/tmp/step7-modern-${label}.txt"
  adb shell am force-stop "$PKG" || true
  adb shell am instrument -w -r \
    -e class "$CLASS" \
    -e expectedExact "$expected_exact" \
    -e expectedNotification "$expected_notification" \
    "$RUNNER" | tee "$log"
  grep -Eq '^OK \(1 test\)|^OK \(1 tests\)' "$log"
  ! grep -q 'FAILURES!!!' "$log"
}

adb shell appops set "$PKG" SCHEDULE_EXACT_ALARM deny || adb shell appops set "$PKG" SCHEDULE_EXACT_ALARM ignore
adb shell pm revoke "$PKG" android.permission.POST_NOTIFICATIONS || true
run_matrix false false denied

adb shell appops set "$PKG" SCHEDULE_EXACT_ALARM allow
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS
run_matrix true true granted

rm -f "$TEST_SRC"
echo 'STEP7_MODERN_MATRIX PASS api=36 exact=denied+granted notifications=denied+granted fullscreen=query+settings mediaPlayback=manifest'
