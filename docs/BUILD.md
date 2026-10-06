# Build

## Requirements

- Linux or macOS
- JDK 17
- Go 1.26.3
- Android SDK platform 36 and build-tools
- Android NDK installed through SDK Manager
- Gradle 8.x, or add a normal Gradle wrapper to `android/`

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

## Optimized APK

The release variant enables R8 code minification and resource shrinking.
Compose preview/tooling dependencies are debug-only. The engine build uses
`-ldflags="-s -w"` to omit native symbol tables and DWARF debug information.
The gomobile/JNI classes are kept by `app/proguard-rules.pro`.

For a release build signed with this machine's existing Android debug key:

```bash
cd android
./gradlew -PdebugReleaseSigning=true assembleRelease
cd ..
```

This opt-in signing property is for compatibility with an existing debug-signed
installation; it is not a production release key. Without the property, supply
your own Gradle signing configuration or sign the unsigned release output.

If your host cannot strip the native libraries during the Gradle build, the
optional packing tool can strip them with a compatible LLVM/NDK tool and remove
unused ZIP padding. It verifies that allocated ELF section contents and virtual
addresses are unchanged:

```bash
python3 scripts/compact-apk.py \
  android/app/build/outputs/apk/release/app-release.apk \
  unsigned.apk --strip-tool /path/to/llvm-strip
zipalign -f 4 unsigned.apk aligned.apk
apksigner sign --ks /path/to/keystore --ks-key-alias your-alias \
  --out Libs-Tunnel-v1.0.1-arm64.apk aligned.apk
apksigner verify --verbose Libs-Tunnel-v1.0.1-arm64.apk
```

Repacking removes the previous signatures, so alignment and re-signing are
required. Use the same signing key as the installed app for an in-place update.

## Distribution

If you distribute an APK containing the generated AAR, provide recipients with
this corresponding source, dependency versions, scripts, and required license
notices. Do not remove Xray or tun2socks notices.
