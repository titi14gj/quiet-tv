#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
mkdir -p build/launch-tests
javac -encoding UTF-8 -d build/launch-tests $(find tests/launch-stubs -name '*.java') src/cn/quiet/tv/AppLaunch.java
java -cp build/launch-tests cn.quiet.tv.AppLaunchTest
