# Libs Tunnel

Android VPN client built as a clean-room project. It does not contain
Dark Tunnel code, assets, keys, branding, encrypted configuration logic, or its
private `libdarktunnel` wrapper.

Official signed build: <https://sgx.tdz-server.store/drive/>

The project combines:

- Xray-core 26.3.27 for VLESS, VMess, Trojan, Shadowsocks, TCP, WebSocket, gRPC,
  HTTP Upgrade, XHTTP, mKCP, TLS, REALITY, and SOCKS inbounds.
- Direct SSH tunneling through tun2socks.
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

1. Install JDK 17 or newer (verified on JDK 21), Go 1.26.3, Android SDK 36,
   Android NDK, and Gradle 8.11+.
2. Run `scripts/build-engine.sh` to generate `android/app/libs/vpncore.aar`.
   The AAR is a build artifact and is gitignored, so it is regenerated from the
   Go engine on every clone.
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

SSH + DNSTT is represented in the profile format but is not yet available as a
runtime backend. OpenVPN is not included because the inspected Dark Tunnel build
did not provide a working OpenVPN backend. Dark Tunnel's proprietary behavior,
ads, analytics, assets, and private configuration format are not copied.

## License

Libs Tunnel is released under **GPL-3.0-only**. That is the project's own
choice, not a constraint: the pinned dependencies are MPL-2.0 (Xray-core), MIT
(tun2socks) and Apache-2.0 (gVisor), each of which may be combined into a GPLv3
work. Xray-core itself stays under MPL-2.0.

If you distribute an APK built from this repository, ship this repository at a
matching tag as the complete corresponding source, as GPL-3.0-only requires.
See [`LICENSE`](LICENSE) and [`third_party/NOTICE.md`](third_party/NOTICE.md)
for the dependency inventory and license texts.
