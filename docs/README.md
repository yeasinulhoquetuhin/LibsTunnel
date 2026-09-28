# Documentation

Every document in this folder, and what it covers.

| Document | Purpose |
| --- | --- |
| [architecture.md](architecture.md) | How the Android service, the Go manager, the payload injector and tun2socks fit together, and how the socket loop is prevented. |
| [build.md](build.md) | Toolchain requirements and the exact commands that produce the engine AAR and the APK. |
| [configuration.md](configuration.md) | The profile schema, every field it accepts, and the encrypted share format. |
| [reference/protocols.md](reference/protocols.md) | The full matrix of protocols, transports, security layers and stream settings. |

## Reading order

If you are new to the project, read them in this order:

1. [architecture.md](architecture.md) to understand the data path from the Android TUN
   descriptor to the remote endpoint.
2. [configuration.md](configuration.md) to understand what a stored profile contains.
3. [reference/protocols.md](reference/protocols.md) for the exact protocol and transport
   combinations the engine accepts.
4. [build.md](build.md) only when you intend to compile the project yourself.

## Related files outside this folder

- [`../README.md`](../README.md) - project overview, feature list, download links.
- [`../third_party/THIRD-PARTY-NOTICES.md`](../third_party/THIRD-PARTY-NOTICES.md) - pinned
  dependency versions and their licenses.
- [`../CONTRIBUTING.md`](../CONTRIBUTING.md) - how to propose a change.
- [`../SECURITY.md`](../SECURITY.md) - how to report a vulnerability privately.
