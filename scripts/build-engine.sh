#!/usr/bin/env sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ENGINE="$ROOT/engine"
OUTPUT="$ROOT/android/app/libs/vpncore.aar"

command -v go >/dev/null 2>&1 || {
  printf '%s\n' "Go 1.26.3 or newer is required" >&2
  exit 1
}

cd "$ENGINE"
go mod download
go test ./...

if ! command -v gomobile >/dev/null 2>&1; then
  go install golang.org/x/mobile/cmd/gomobile@v0.0.0-20260803200217-62cee1672c8e
  go install golang.org/x/mobile/cmd/gobind@v0.0.0-20260803200217-62cee1672c8e
  PATH="$(go env GOPATH)/bin:$PATH"
fi

gomobile init
gomobile bind \
  -target=android/arm64 \
  -androidapi=24 \
  -ldflags="-s -w" \
  -javapkg=com.libsvpn.tunnel.bindings \
  -o "$OUTPUT" \
  .

printf '%s\n' "Built $OUTPUT"
