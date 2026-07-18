# AdShield

A local-only Android app that detects apps repeatedly showing pop-up / overlay
ads — including on your home screen when no app is open — ranks the likely
culprit, shows on-device statistics, and automatically dismisses high-confidence
ad overlays. No login, no signup, no internet access.

## How it works

1. **`AdDetectorAccessibilityService`** — Android's only supported way for a
   non-root app to observe windows it doesn't own. It fires on every window
   change system-wide and scores each window with a handful of explainable
   heuristics (overlay window type, missing close/skip affordance, known ad-SDK
   view IDs, full-screen coverage, and burst frequency from the same package).
2. High-scoring windows are logged locally and, above a second threshold,
   automatically dismissed (`GLOBAL_ACTION_BACK`, falling back to
   `GLOBAL_ACTION_HOME` if the same overlay keeps reappearing — some ad SDKs
   intentionally swallow the back button).
3. **`OverlayPermissionScanner`** cross-references `AppOpsManager` to list every
   installed app currently holding `SYSTEM_ALERT_WINDOW`, so you can see apps
   capable of showing overlays even before they've triggered a detection.
4. Everything is written to a **SQLCipher-encrypted Room database**, whose key
   lives only in `EncryptedSharedPreferences` backed by the Android Keystore.
   The app has **no `INTERNET` permission at all**, so nothing can ever be
   transmitted off the device — this is enforced by the manifest, not just a
   policy promise.

## Honest limitations (please read before relying on this)

- It **cannot** see or block ads rendered *inside* an app's own activity while
  you're actively using that app (in-app banners/interstitials) — those are
  normal UI the app owns, with no anomaly signal to detect.
- Some ad SDKs disable the back button on purpose; the HOME fallback exits the
  situation but doesn't "block" the ad from ever having rendered.
- Heuristic detection means occasional false positives/negatives are possible.
  Use the "mark as trusted" action in an app's detail screen to stop flagging
  a specific app you know is legitimate.
- The most reliable long-term fix for a genuinely malicious app is still to
  **uninstall it** — this app's job is to help you find which one it is
  quickly, and provide a shortcut to app info / overlay settings / uninstall.

## Project structure

```
app/src/main/java/com/adshield/detector/
  MainActivity.kt                 Compose navigation host
  AdShieldApplication.kt          App-wide Room DB instance
  service/
    AdDetectorAccessibilityService.kt   Detection + auto-dismiss engine
    BootRestartReceiver.kt
  data/                            Room entities, DAO, encrypted DB, repository
  util/                            Overlay-permission scanner, app-info helpers,
                                    Keystore-backed passphrase manager
  ui/
    screens/                       Dashboard, Suspects, Statistics, App detail, Settings
    theme/
```

## Building locally

Requires JDK 17 and Android SDK (compileSdk 34, minSdk 26).

```bash
# If gradlew isn't present yet, generate the wrapper once (requires a local
# Gradle install, or just open the project in Android Studio, which does
# this automatically on first sync):
gradle wrapper --gradle-version 8.7

./gradlew assembleDebug
```

## Deploying via Codemagic

`codemagic.yaml` is included at the repo root and configured for an
`android-release` workflow that:

1. Generates the Gradle wrapper on the build machine.
2. Builds a signed release AAB and APK using a keystore you upload in the
   Codemagic UI (reference it under **Team settings → Code signing identities**
   and name it `adshield_keystore`, matching the `android_signing` entry in
   the YAML — update the group/keystore names to match your own setup).
3. Uploads `*.aab`, `*.apk`, and the ProGuard `mapping.txt` as build artifacts.

You'll need to replace `your-email@example.com` in `codemagic.yaml` with your
own notification address, and set up the `keystore_credentials` environment
group with your signing secrets in the Codemagic dashboard.

## Permissions used, and why

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Query other apps' overlay grants (not to draw our own overlay) |
| `PACKAGE_USAGE_STATS` | Reserved for future foreground-app correlation (not required for core detection) |
| `QUERY_ALL_PACKAGES` | Enumerate installed apps to check overlay permission holders |
| `BIND_ACCESSIBILITY_SERVICE` | Core detection engine — see above |
| `RECEIVE_BOOT_COMPLETED` | Reserved for local maintenance on boot (currently a no-op) |
| `POST_NOTIFICATIONS` | Reserved for future local "blocked an ad" notifications |

Notably **absent**: `INTERNET`. This is intentional and permanent.

## Play Store note

Apps using `AccessibilityService` for anything beyond assisting users with
disabilities get extra scrutiny during Play review. Be ready to clearly
justify the use case in your store listing and the in-app disclosure (already
included in `strings.xml` as `accessibility_service_description`) — this is
standard for the "overlay/ad-blocker utility" category and is not unique to
this app.
