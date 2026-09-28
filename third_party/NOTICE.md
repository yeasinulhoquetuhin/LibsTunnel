# Third-Party Notices

Libs Tunnel is licensed under GPL-3.0-only (see [`LICENSE`](../LICENSE)). It
links against the third-party Go modules pinned in `engine/go.mod`. The license
texts of the direct dependencies are mirrored in
[`third_party/licenses/`](licenses/).

Dependencies are consumed as Go modules and are **not** vendored, so the
pinned sources are fetched from their upstreams during the build.

## Xray-core

- Project: <https://github.com/XTLS/Xray-core>
- Pinned version: `v1.260327.0` (upstream release 26.3.27)
- License: Mozilla Public License 2.0 (MPL-2.0)
- Imported: Go module dependency, unmodified

MPL-2.0 is file-level copyleft. This project does not patch Xray-core sources.
If you modify any MPL-covered file, that modified file must stay available under
MPL-2.0 with its notices intact, and recipients of a distributed binary must be
able to obtain the source of the MPL-covered files you shipped.

## tun2socks

- Project: <https://github.com/xjasonlyu/tun2socks>
- Pinned version: `v2.7.0`
- License: MIT
- Imported: Go module dependency, unmodified

## gVisor

- Project: <https://gvisor.dev/gvisor>
- Pinned version: `v0.0.0-20260701204157-69c2d17aea96`
- License: Apache License 2.0
- Imported: Go module dependency, unmodified

Apache-2.0 requires that the license text and any upstream `NOTICE` file travel
with redistributed binaries.

## Other Go and Android dependencies

Transitive Go dependencies are pinned in `engine/go.sum`; Android and AndroidX
dependencies are declared in `android/app/build.gradle.kts` and
`android/gradle/libs.versions.toml`. Jetpack Compose and AndroidX are
Apache-2.0. Their license texts are shipped inside the Gradle and Go module
caches, and can be regenerated with:

```bash
cd engine && go mod graph
cd android && ./gradlew :app:dependencies
```

## Distribution

If you distribute an APK built from this repository, ship this repository at a
matching tag as the complete corresponding source, as GPL-3.0-only requires.

No Dark Tunnel binary, source, icon, trademark, configuration, or private key
is included in this project.
