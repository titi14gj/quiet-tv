#!/usr/bin/env bash
set -euo pipefail
TV_SERIAL="${1:?Usage: bash scripts/restore-screensaver.sh TV_IP:5555}"
# Restore the values observed on the documented Whaley test firmware.
# Other devices must restore their own original values.
adb -s "$TV_SERIAL" shell settings put system screen_off_timeout 300000
adb -s "$TV_SERIAL" shell settings put secure screensaver_enabled 1
adb -s "$TV_SERIAL" shell settings put secure screensaver_activate_on_sleep 1
adb -s "$TV_SERIAL" shell settings put secure screensaver_activate_on_dock 1
