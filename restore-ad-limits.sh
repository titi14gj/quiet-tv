#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
TV="${1:?Usage: bash restore-ad-limits.sh TV_IP:5555}"
adb connect "$TV"
adb -s "$TV" push build/quiet-admin.jar /data/local/tmp/quiet-admin.jar
adb -s "$TV" shell 'CLASSPATH=/data/local/tmp/quiet-admin.jar app_process /system/bin cn.quiet.tools.TvAdmin set com.helios.ad.service 24 0'
adb -s "$TV" shell 'CLASSPATH=/data/local/tmp/quiet-admin.jar app_process /system/bin cn.quiet.tools.TvAdmin set com.helios.ad.service 11 0'
adb -s "$TV" shell 'CLASSPATH=/data/local/tmp/quiet-admin.jar app_process /system/bin cn.quiet.tools.TvAdmin set com.helios.ad.service 45 0'
adb -s "$TV" shell 'CLASSPATH=/data/local/tmp/quiet-admin.jar app_process /system/bin cn.quiet.tools.TvAdmin set com.helios.ad.service 40 0'
adb -s "$TV" shell 'CLASSPATH=/data/local/tmp/quiet-admin.jar app_process /system/bin cn.quiet.tools.TvAdmin set cn.whaley.adsys 24 0'
adb -s "$TV" shell 'CLASSPATH=/data/local/tmp/quiet-admin.jar app_process /system/bin cn.quiet.tools.TvAdmin set cn.whaley.adsys 11 0'
adb -s "$TV" shell 'CLASSPATH=/data/local/tmp/quiet-admin.jar app_process /system/bin cn.quiet.tools.TvAdmin set cn.whaley.adsys 45 0'
adb -s "$TV" shell 'CLASSPATH=/data/local/tmp/quiet-admin.jar app_process /system/bin cn.quiet.tools.TvAdmin set cn.whaley.adsys 40 0'
