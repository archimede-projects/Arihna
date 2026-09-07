from pathlib import Path

path = Path('app/src/main/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreen.kt')
text = path.read_text(encoding='utf-8')

old = '''    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ARIHNA",'''
new = '''    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.82f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "ARIHNA",'''
if text.count(old) != 1:
    raise SystemExit(f'expected one HomeHeader column target, found {text.count(old)}')
text = text.replace(old, new, 1)

old = '                    modifier = Modifier.testTag("home-hijri-date"),'
new = '                    modifier = Modifier.fillMaxWidth().testTag("home-hijri-date"),'
if text.count(old) != 1:
    raise SystemExit(f'expected one Hijri text tag target, found {text.count(old)}')
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('Applied v3 HomeHeader visibility fix for Hijri date')
