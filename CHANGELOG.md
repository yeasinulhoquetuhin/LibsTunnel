# Changelog

## 1.0.1

- Replaced the original 50.7 MiB debug APK with a 12.4 MiB optimized release-variant APK, retaining the v1.0.1 signing certificate.
- Enabled R8/resource shrinking, made Compose preview tooling debug-only, and removed native debug symbols and excess APK ZIP padding.
- Added an optional APK packing tool and optimized-build instructions.
- Reworked the top bar: "Libs Tunnel" aligned left and the connection-status label removed.
- Expanded About details in Settings.
- Back from a non-home section now returns to Home; back from the profile editor returns to the section that opened it.
- Updated `.libs` sharing to send a real config file; imports show the chosen file before confirmation.
- Kept single-profile file export, clipboard copy; removed bulk export, encrypted/passphrase export, and QR/Share options.
- Expanded logs with search, severity filters, auto-scroll, copy/share, and clear confirmation.
- Added deletion protection for the active profile, confirmation, and success feedback.
- Added a blank initial server port, one more accent color, and configurable haptic feedback.
- Added a Reconnect action to the VPN notification.
- Expanded the About section and made the privacy card fill the available width.
