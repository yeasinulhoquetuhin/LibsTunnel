# Changelog

All notable changes to Libs Tunnel are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-09-28

First public release.

### Added

- Android VPN client built on Jetpack Compose with a headless `VpnService`.
- Go engine exposed to Android through gomobile as `vpncore.aar`.
- VLESS, VMess and Trojan profiles.
- TCP, WebSocket, gRPC, HTTP Upgrade, SplitHTTP/XHTTP and mKCP transports.
- TLS and REALITY security layers.
- Local payload injector with optional outer TLS and response validation.
- tun2socks plus gVisor bridge from the Android TUN descriptor into a local
  Xray SOCKS5 inbound.
- Per-app routing, custom DNS, and socket protection through
  `VpnService.protect`.
- Import and export of `vless://`, `vmess://` and `trojan://` share links.
- Encrypted `.libs` profile export and import.
- Payload injector, theme and accent settings, connection logs.
- Reproducible build scripts and a Gradle wrapper.

### Known limitations

- OpenVPN and DNSTT are not implemented.
- SSH tunnelling works in the engine through `backend: "ssh"`, but the Android
  profile editor does not expose it yet.
- Release artifacts are debug-signed; no release keystore is published.

[1.0.0]: https://github.com/yeasinulhoquetuhin/LibsTunnel/releases/tag/v1.0.0
