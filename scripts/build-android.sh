#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ANDROID="$ROOT/android"

test -f "$ANDROID/app/libs/vpncore.aar" || {
  printf '%s\n' "Build the engine AAR first: ./scripts/build-engine.sh" >&2
  exit 1
}

cd "$ANDROID"
if test -x ./gradlew; then
  ./gradlew assembleDebug
else
  command -v gradle >/dev/null 2>&1 || {
    printf '%s\n' "Gradle or a Gradle wrapper is required" >&2
    exit 1
  }
  gradle assembleDebug
fi
