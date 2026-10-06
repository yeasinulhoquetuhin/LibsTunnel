# Configuration

The engine receives one JSON object containing `xray`, `injection`, and `tun`.
See `engine/examples/` for complete examples.

## Injection modes

- `direct`: TCP to the Xray target, then payload.
- `proxy`: TCP to `proxyHost:proxyPort`, then payload for the target.

## Placeholders

- `[method]`
- `[host]`
- `[port]`
- `[host_port]`
- `[protocol]`
- `[ua]`
- `[real_raw]`
- `[crlf]`, `[lfcr]`, `[cr]`, `[lf]`
- `[split]`
- `[split=N]`, where `N` is a delay from 0 to 60000 milliseconds

`[ua]` is explicitly supported and is replaced by the configured `userAgent`.

## Response handling

`responseMode` may be:

- `headers`: read through the blank line and require a status from
  `acceptedStatus` unless `acceptAnyResponse` is true.
- `none`: do not consume a response; bridge immediately after writing payload.

Use `headers` for CONNECT/Upgrade gateways that return `101` or `200`. Use
`none` only when the endpoint expects payload bytes but sends no outer response.

## Outbound rewriting

Set `outboundTag` when the Xray config contains multiple outbounds. The engine
supports rewriting the first server of VLESS, VMess, Trojan, or Shadowsocks.
Routing rules and all other Xray JSON remain unchanged.
