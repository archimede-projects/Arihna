from pathlib import Path

path = Path('app/src/main/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreen.kt')
text = path.read_text(encoding='utf-8')
old = '''            Column(
                modifier = Modifier
                    .clickable { calendarOpen = true }
                    .testTag("home-date-block"),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = formatDate(state.localDate, arabic),
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeMuted,
                    modifier = Modifier.testTag("home-current-date"),
                )
                Text(
                    text = HijriDateFormatter.format(state.localDate, arabic),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeAccent,
                    modifier = Modifier.fillMaxWidth().testTag("home-hijri-date"),
                )
            }
'''
new = '''            Text(
                text = formatDate(state.localDate, arabic),
                style = MaterialTheme.typography.bodySmall,
                color = HomeMuted,
                modifier = Modifier
                    .clickable { calendarOpen = true }
                    .testTag("home-date-block"),
            )
            Text(
                text = HijriDateFormatter.format(state.localDate, arabic),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = HomeAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { calendarOpen = true }
                    .testTag("home-hijri-date"),
            )
'''
if text.count(old) != 1:
    raise SystemExit(f'expected one v3 date block, found {text.count(old)}')
text = text.replace(old, new, 1)
path.write_text(text, encoding='utf-8')
print('Applied v4 Hijri visibility/semantics fix')
