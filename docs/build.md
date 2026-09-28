# Build

## Requirements

- Linux or macOS
- JDK 17 or newer, verified on 21
- Go 1.26.3
- Android SDK platform 36 and build-tools
- Android NDK installed through SDK Manager
- No separate Gradle install needed: the wrapper in `android/gradlew`
  pins Gradle 8.11.1

Use the pinned Go version for reproducible gomobile output.

## Engine AAR

From the project root:

```bash
./scripts/build-engine.sh
```

This runs Go tests and builds:

```text
android/app/libs/vpncore.aar
```

The generated Java package is `com.libsvpn.tunnel.bindings.vpncore`, matching
the imports in `CoreBridge.kt`.

## Android APK

```bash
./scripts/build-android.sh
```

The installable debug APK is written to
`android/app/build/outputs/apk/debug/app-debug.apk`. The module includes the
Libs Tunnel launcher Activity and Compose interface.

## Distribution

If you distribute an APK containing the generated AAR, provide recipients with
this corresponding source, dependency versions, scripts, and required license
notices. Do not remove Xray or tun2socks notices.
