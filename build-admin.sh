#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
BT="$SDK/build-tools/33.0.0"
mkdir -p build/admin-classes build/admin-dex
javac -source 8 -target 8 -bootclasspath "$SDK/platforms/android-33/android.jar:$BT/core-lambda-stubs.jar" -d build/admin-classes admin-src/cn/quiet/tools/*.java
"$BT/d8" --lib "$SDK/platforms/android-33/android.jar" --min-api 21 --output build/admin-dex build/admin-classes/cn/quiet/tools/*.class
(cd build/admin-dex && zip -q -j ../quiet-admin.jar classes.dex)
echo 'Built build/quiet-admin.jar'
