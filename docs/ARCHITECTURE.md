# Architecture

## Components

### Android service

`HeadlessVpnService` owns Android VPN consent, the TUN descriptor, foreground
service lifetime, and socket protection. It has no Activity or visual UI.

### Go manager

`Manager` starts and stops Xray, the injector, and tun2socks in a deterministic
order. It is the API exported through gomobile.

### Injector

The injector listens on a random localhost TCP port. Before Xray starts, the
selected outbound's original address and port are saved as the target. Xray's
server address is then rewritten to the injector's localhost address.

For every Xray transport connection, the injector:

1. Opens and protects a remote socket.
2. Optionally establishes an outer TLS connection for SNI injection modes.
3. Expands and writes the configured payload.
4. Optionally validates HTTP-like response headers.
5. Bridges the resulting byte stream to Xray.

Xray remains responsible for the inner VLESS/VMess/Trojan protocol and its
TCP/WebSocket/gRPC/TLS transport. This keeps payload injection protocol-neutral.

### tun2socks

The Android TUN descriptor is passed directly to Go. gVisor terminates TCP/UDP
from that descriptor and sends it to Xray's local SOCKS5 inbound.

## Socket loop prevention

The injector calls Android `VpnService.protect(fd)` through the exported
`Platform` interface before its remote socket connects. DNS is resolved and
cached before Android installs the VPN route. Xray only dials the localhost
injector, so it does not create a second unprotected remote socket.

## Payload layers

For VLESS over WebSocket with an outer payload, two HTTP-looking exchanges are
expected:

```text
outer payload -> tunnel established -> real Xray WebSocket handshake -> VLESS
```

An `Upgrade: websocket` line in the outer payload does not change the Xray
transport. The Xray `streamSettings.network` field controls the real transport.
