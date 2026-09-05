from pathlib import Path

path = Path("app/src/main/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreen.kt")
text = path.read_text(encoding="utf-8")

alarm_cards = """        item {
            AlarmSystemSettingsCard(
                state = alarmSettings,
                onManageNotifications = onManageNotifications,
                onManageExactAlarms = onManageExactAlarms,
                onManageFullScreen = onManageFullScreen,
            )
        }
        item {
            AlarmDiagnosticCard(
                state = alarmSettings,
                onTestAlarm = onTestAlarm,
                onTestAdhan = onTestAdhan,
                onCancelDiagnostic = onCancelDiagnostic,
            )
        }
"""
if text.count(alarm_cards) != 1:
    raise SystemExit("expected top alarm settings block missing or duplicated")
text = text.replace(alarm_cards, "", 1)

insertion = """        items(
            items = uiState.searchResults,
            key = { city -> city.id },
        ) { city ->
            CityResultCard(city = city, onSelectCity = onSelectCity)
        }
"""
moved = insertion + """
        item {
            AlarmSystemSettingsCard(
                state = alarmSettings,
                onManageNotifications = onManageNotifications,
                onManageExactAlarms = onManageExactAlarms,
                onManageFullScreen = onManageFullScreen,
            )
        }
        item {
            AlarmDiagnosticCard(
                state = alarmSettings,
                onTestAlarm = onTestAlarm,
                onTestAdhan = onTestAdhan,
                onCancelDiagnostic = onCancelDiagnostic,
            )
        }
"""
if text.count(insertion) != 1:
    raise SystemExit("expected city results insertion marker missing or duplicated")
path.write_text(text.replace(insertion, moved, 1), encoding="utf-8")
