#!/usr/bin/env bash
set -euo pipefail
TV_SERIAL="${1:?Usage: bash scripts/restore-factory-packages.sh TV_IP:5555}"
for package in com.helios.launcher com.helios.ad.service cn.whaley.adsys; do
  adb -s "$TV_SERIAL" shell pm unhide --user 0 "$package"
done
adb -s "$TV_SERIAL" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
