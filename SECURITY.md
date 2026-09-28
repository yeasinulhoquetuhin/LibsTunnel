# Security Policy

## Supported versions

| Version | Supported |
| --- | --- |
| 1.0.x | Yes |
| Anything older | No |

## Reporting a vulnerability

Please report security issues privately. Do not open a public issue, and do not
mention the issue in a pull request or a public chat.

Send a description of the problem, the affected version, and the steps needed to
reproduce it to the maintainer through one of these channels:

- Telegram: [@TuhinBroh](https://t.me/TuhinBroh)
- Email via the website: [tuhinbro.com](https://tuhinbro.com)

Include the following where it helps:

- The app version and Android version.
- The relevant log output with identifiers removed.
- Whether the issue requires a malicious server or a malicious profile.

You can expect an acknowledgement, a decision on whether the report is
accepted, and a timeline for a fix. Please allow a reasonable period for a
patch to be prepared before any public disclosure.

## What counts as a vulnerability in this project

- Traffic that escapes the VPN tunnel, or a socket loop that causes a leak.
- A failure to protect the injector socket that bypasses the tunnel.
- Credential or profile data written to storage, logs or an exported file in
  plaintext when the format promises encryption.
- Any code path that would execute attacker-controlled input.

## Hardening notes for users

- Server credentials belong in the app, never in a public repository. This
  repository is scrubbed of profiles, but a fork is your own responsibility.
- `.libs` exports are encrypted with a passphrase. Choose a strong one.
- Verify the checksum published with a release before installing an APK.
