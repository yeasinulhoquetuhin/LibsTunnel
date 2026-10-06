# Libs Tunnel

Android VPN client built as a clean-room project. It does not contain
Dark Tunnel code, assets, keys, branding, encrypted configuration logic, or its
private `libdarktunnel` wrapper.

The project combines:

- Xray-core 26.3.27 for VLESS, VMess, Trojan, Shadowsocks, TCP, WebSocket, gRPC,
  HTTP Upgrade, XHTTP, mKCP, TLS, REALITY, and SOCKS inbounds.
- A new local payload injector written in this project.
- `xjasonlyu/tun2socks` and gVisor for routing an Android TUN file descriptor
  into Xray's local SOCKS5 inbound.
- A responsive Jetpack Compose client around an Android `VpnService`.

## Layout

```text
engine/                  Go engine and tests
engine/examples/         Safe placeholder configurations
android/                 Headless Android application/service
scripts/                 Reproducible build commands
docs/                    Architecture, build, and configuration docs
third_party/             Dependency and license notices
```

## Build order

1. Install JDK 17, Go 1.26.3, Android SDK 36, Android NDK, and Gradle.
2. Run `scripts/build-engine.sh` to generate `android/app/libs/vpncore.aar`.
3. Run `scripts/build-android.sh`.

Detailed instructions are in `docs/BUILD.md`.

## Calling from your UI

First request VPN consent:

```kotlin
val permission = VpnCommands.permissionIntent(this)
if (permission != null) {
    vpnPermissionLauncher.launch(permission)
}
```

After consent:

```kotlin
VpnCommands.start(this, engineConfigJson, mtu = 1500)
```

Stop it with:

```kotlin
VpnCommands.stop(this)
```

Keep credentials in app-private storage. Do not ship server UUIDs, passwords,
or private keys in public source or APK resources.

## Scope

The engine supports the normal payload flow:

```text
Android TUN -> tun2socks -> Xray SOCKS inbound -> Xray outbound
            -> local injector -> outer payload/proxy -> remote endpoint
```

The Android app exposes VLESS, VMess, and Trojan profiles through Xray. OpenVPN
is not included because the inspected Dark Tunnel build did not provide a
working OpenVPN backend. Dark Tunnel's proprietary behavior, ads, analytics,
assets, and private configuration format are not copied.

## License

This project is offered under GPL-3.0-only. Xray-core remains under MPL-2.0 and
tun2socks is MIT-licensed. See `LICENSE` and
`third_party/NOTICE.md` before distributing an APK.
