#!/usr/bin/env bash
# Przeglad ekranow na emulatorze: instalacja, przejscie po ekranach, zrzuty, logi.
set -x
OUT=shots; mkdir -p $OUT
adb install -r Drama.apk
adb logcat -c
adb shell am start -n com.drama.app/.MainActivity
sleep 15
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  python3 - "$1" <<'PY'
import re, sys, subprocess
t = sys.argv[1]
x = open("ui.xml", encoding="utf-8").read()
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    if f'text="{t}"' in n or f'content-desc="{t}"' in n:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])
            print("tap", t); break
else:
    print("NOT FOUND", t)
PY
}
shot 01_dla_ciebie
adb shell input swipe 540 1700 540 500 300; sleep 4; shot 02_dla_ciebie_nastepna
tap_text "Oglądaj"; sleep 7; shot 03_odtwarzacz
sleep 6; shot 04_odtwarzacz_pozniej
sleep 6; shot 04b_odtwarzacz_jeszcze
adb shell dumpsys meminfo com.drama.app | head -12 > $OUT/pamiec.txt
adb shell input keyevent KEYCODE_BACK; sleep 3
adb shell am start -n com.drama.app/.MainActivity; sleep 5
tap_text "Szczegóły"; sleep 5; shot 05_szczegoly
adb shell input keyevent KEYCODE_BACK; sleep 2
tap_text "Odkrywaj"; sleep 6; shot 06_odkrywaj
adb shell input swipe 540 1800 540 700 300; sleep 3; shot 07_odkrywaj_nizej
tap_text "Moja lista"; sleep 4; shot 08_moja_lista
adb logcat -d > $OUT/logcat.txt
grep -E "com.drama.app|chromium" $OUT/logcat.txt | grep -iE "AndroidRuntime|FATAL|CONSOLE|onError|exception" | head -80 > $OUT/bledy.txt
ls -la $OUT
