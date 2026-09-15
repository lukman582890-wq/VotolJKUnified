from pathlib import Path

main = Path('app/src/main/java/com/votoljk/unified/MainActivity.kt')
s = main.read_text()
marker = '        findViewById<Button>(R.id.exportLog).setOnClickListener { exportSystemLog() }'
insert = marker + '\n        findViewById<Button>(R.id.settingsButton).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }'
if 'R.id.settingsButton' not in s:
    if marker not in s:
        raise SystemExit('MainActivity exportLog marker not found')
    s = s.replace(marker, insert, 1)
main.write_text(s)

xml = Path('app/src/main/res/layout/activity_main.xml')
x = xml.read_text()
if '@+id/settingsButton' not in x:
    marker = 'android:text="EV SERVICE TOOL  •  VOTOL CONTROLLER  •  JK BMS" android:textColor="#718096" android:textSize="11sp"/>'
    replacement = marker + '<Button android:id="@+id/settingsButton" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="10dp" android:text="⚙  PENGATURAN" android:textSize="14sp"/>'
    if marker not in x:
        raise SystemExit('Dashboard subtitle marker not found')
    x = x.replace(marker, replacement, 1)
xml.write_text(x)

manifest = Path('app/src/main/AndroidManifest.xml')
m = manifest.read_text()
if 'android:name=".SettingsActivity"' not in m:
    marker = '        <activity android:name=".BmsDetailActivity" />'
    if marker not in m:
        raise SystemExit('Manifest activity marker not found')
    m = m.replace(marker, marker + '\n        <activity android:name=".SettingsActivity" android:screenOrientation="portrait" />', 1)
manifest.write_text(m)
print('SETTINGS PATCHED')
