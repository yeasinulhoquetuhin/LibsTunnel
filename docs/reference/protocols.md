# Protocols and Transports

The engine accepts the combinations listed below. The Android client validates
them before a profile is saved, and `EngineConfigBuilder` converts them into the
Xray configuration that the Go engine runs.

## Protocols

| Protocol | Notes |
| --- | --- |
| VLESS | UUID based, the recommended default. |
| VMess | Legacy protocol, still supported for older servers. |
| Trojan | Password based, usually used with TLS. |

Shadowsocks is available inside Xray, and a SOCKS inbound is used for the local
tun2socks hop, but neither is exposed as a client-facing profile type.

## Transports

| Transport | Field value | Notes |
| --- | --- | --- |
| TCP | `tcp` | Default, optionally with header obfuscation. |
| WebSocket | `ws` | Supports a custom path and host header. |
| gRPC | `grpc` | Service name is configurable. |
| HTTP Upgrade | `httpupgrade` | Uses a custom host and path. |
| SplitHTTP / XHTTP | `splithttp` | HTTP-based transport with configurable mode. |
| mKCP | `mkcp` | Packet-based, configurable seed and obfuscation. |

## Security layers

| Security | Notes |
| --- | --- |
| TLS | Standard TLS, with SNI and optional ALPN. |
| REALITY | XTLS REALITY, requires a valid destination and short ID. |
| None | Plaintext transport, only useful behind a local injector. |

## Flow settings

Stream settings such as packet encoding, TCP header type, mKCP seed, WebSocket
headers and gRPC authority are stored per profile, so each saved profile fully
describes its own connection.

## Payload injection in Xray

Payload injection is built into the Xray connection itself, so a payload can be
written directly into the Xray transport without an external proxy. The injector
sits between the Xray outbound and the remote endpoint. The original destination
address and port are captured before Xray starts, and Xray is pointed at a
loopback port owned by the injector instead. For each transport connection the
injector opens and protects a socket, optionally negotiates an outer TLS session
for SNI injection, writes the configured payload, optionally validates HTTP-like
response headers, and then bridges the byte stream to Xray.

### Modes

| Mode | Engine value | Behaviour |
| --- | --- | --- |
| Direct | `direct` | Writes the payload straight to the remote endpoint. |
| Direct with SNI | `direct-sni` | Adds an outer TLS session using the configured SNI before the payload. |
| Proxy | `proxy` | Writes the payload through a local proxy at the configured host and port. |
| Proxy with SNI | `proxy-sni` | Adds an outer TLS session and writes the payload through the local proxy. |

Because injection happens below the protocol layer, it is independent of the
protocol in use. The expected byte sequence for a WebSocket transport with an
outer payload is:

```text
outer payload -> tunnel established -> real Xray WebSocket handshake -> protocol
```

## Not exposed and not implemented

- OpenVPN is not implemented. The reference build inspected during development
  did not provide a working OpenVPN backend.
- DNSTT is not implemented.
- SSH tunnelling is implemented in the engine and is selected with
  `backend: "ssh"` in a configuration file, but the Android profile editor does
  not expose it. It is currently reachable through the `vpncore` command.

## Also registered but not offered as profile types

The engine registers a wider set of Xray modules than the Android client
offers: Shadowsocks, WireGuard, HTTP, SOCKS, Freedom, Blackhole and Dokodemo
outbounds, the fake DNS and reverse proxy applications, and the observatory.
They are present because the engine imports the corresponding Xray packages, not
because the client can configure them.
