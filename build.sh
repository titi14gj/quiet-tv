#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK" && -d "$HOME/Library/Android/sdk" ]]; then SDK="$HOME/Library/Android/sdk"; fi
if [[ -z "$SDK" ]]; then echo 'Set ANDROID_HOME to your Android SDK directory.' >&2; exit 1; fi
BT="$SDK/build-tools/33.0.0"
for tool in aapt d8 zipalign apksigner; do
  [[ -x "$BT/$tool" ]] || { echo "Missing tool: $BT/$tool" >&2; exit 1; }
done
[[ -f "$SDK/platforms/android-33/android.jar" ]] || { echo 'Install Android SDK Platform 33.' >&2; exit 1; }
mkdir -p build/classes build/dex build/generated
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex build/generated
"$BT/aapt" package -f -m -M AndroidManifest.xml -S res -A assets -I "$SDK/platforms/android-33/android.jar" -J build/generated
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$SDK/platforms/android-33/android.jar:$BT/core-lambda-stubs.jar" -d build/classes src/cn/quiet/tv/*.java build/generated/cn/quiet/tv/R.java
"$BT/d8" --lib "$SDK/platforms/android-33/android.jar" --min-api 21 --output build/dex build/classes/cn/quiet/tv/*.class
"$BT/aapt" package -f -M AndroidManifest.xml -S res -A assets -I "$SDK/platforms/android-33/android.jar" -F build/unsigned.apk
(cd build/dex && zip -q -j ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [[ -n "${QUIETTV_KEYSTORE:-}" ]]; then
  : "${QUIETTV_KEY_ALIAS:?Set QUIETTV_KEY_ALIAS}"
  : "${QUIETTV_STORE_PASSWORD:?Set QUIETTV_STORE_PASSWORD}"
  export QUIETTV_KEY_PASSWORD="${QUIETTV_KEY_PASSWORD:-$QUIETTV_STORE_PASSWORD}"
  export QUIETTV_STORE_PASSWORD
else
  mkdir -p .local
  QUIETTV_KEYSTORE="$PWD/.local/development.jks"
  QUIETTV_KEY_ALIAS=development
  export QUIETTV_STORE_PASSWORD=android QUIETTV_KEY_PASSWORD=android
  if [[ ! -f "$QUIETTV_KEYSTORE" ]]; then
    keytool -genkeypair -keystore "$QUIETTV_KEYSTORE" -storepass:env QUIETTV_STORE_PASSWORD -keypass:env QUIETTV_KEY_PASSWORD -alias "$QUIETTV_KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Quiet TV Development'
  fi
fi
"$BT/apksigner" sign --ks "$QUIETTV_KEYSTORE" --ks-key-alias "$QUIETTV_KEY_ALIAS" --ks-pass env:QUIETTV_STORE_PASSWORD --key-pass env:QUIETTV_KEY_PASSWORD --out build/QuietTV.apk build/aligned.apk
"$BT/apksigner" verify build/QuietTV.apk
echo 'Built build/QuietTV.apk'
