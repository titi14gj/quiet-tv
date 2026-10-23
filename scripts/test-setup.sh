#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
mkdir -p build/setup-tests
javac -encoding UTF-8 -d build/setup-tests src/cn/quiet/tv/{LocalAdb,AdbKeyCodec,SetupPolicy,SetupEngine}.java tests/cn/quiet/tv/SetupTests.java
java -cp build/setup-tests cn.quiet.tv.SetupTests
