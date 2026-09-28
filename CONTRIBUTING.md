# Contributing

Thank you for considering a contribution to Libs Tunnel. This document explains
how to build the project, what a good pull request looks like, and the rules
that keep the repository safe to publish.

## Ground rules

- This is a clean-room project. Do not add code, assets, keys, branding or
  configuration copied from any proprietary application.
- Do not commit real server addresses, UUIDs, passwords, private keys, exported
  profiles or any file matching `*.dark` or `*.libs`.
- Do not commit `android/app/libs/*.aar`, `local.properties`, `*.jks` or
  `*.keystore`. All of them are already ignored.
- Keep dependency versions pinned in `engine/go.mod` and update
  `third_party/THIRD-PARTY-NOTICES.md` whenever a pin changes.

## Build the project

Install the toolchain first: JDK 17 or newer, Go 1.26.3, Android SDK platform
36, Android NDK. The repository ships a Gradle wrapper, so no separate Gradle
installation is needed.

```bash
git clone https://github.com/yeasinulhoquetuhin/LibsTunnel.git
cd LibsTunnel
sh scripts/build-engine.sh
sh scripts/build-android.sh
```

The first command runs the Go tests and writes `android/app/libs/vpncore.aar`.
The second assembles the debug APK.

## Before you open a pull request

Run the same checks that CI runs:

```bash
cd engine && go vet ./... && go test ./...
cd ../android && ./gradlew assembleDebug
```

Then confirm:

- New behaviour is described in `docs/` and listed in `CHANGELOG.md`.
- Public API changes are reflected in the README.
- No new dependency is added without a license entry under
  `third_party/licenses/`.

## Commit and branch naming

- Branch names: `feature/short-description`, `fix/short-description`, or
  `docs/short-description`.
- Commit messages follow the imperative mood, for example
  `Add REALITY short ID validation`.

## Reporting bugs

Open an issue using the bug report template. Please include the app version,
the Android version, the profile settings with all identifiers removed, and the
relevant log lines. Never paste a live share link or a private key into a public
issue.

## Security issues

Do not open a public issue for a vulnerability. Follow [SECURITY.md](SECURITY.md).

## License

By contributing you agree that your contribution is licensed under
GPL-3.0-only, the license of this project.
