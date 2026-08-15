# Android CI Testing Design

## Objective

Add reproducible Android UI and runtime testing to JuWen without relying on the developer machine, the live NewsNow endpoint, GitHub Pages, or an AI provider.

## Selected Approach

Use three complementary test layers:

1. Existing Python and JVM tests verify deterministic data transformation and parsers.
2. Paparazzi renders Compose screens on the JVM for fast visual regression at Redmi Pad Pro landscape and portrait proportions, light mode, and dark mode.
3. ReactiveCircus Android Emulator Runner boots an Android 35 x86_64 emulator in GitHub Actions. Maestro installs and drives a dedicated E2E APK through the primary user flows.

This combination was selected over emulator-only testing because screenshot failures should be fast to diagnose, and over screenshot-only testing because installation, navigation, Android permissions, and process startup require a real Android runtime.

## Test Isolation

Add an `e2e` build type derived from `debug` with a distinct application ID suffix. It exposes a build-time test flag and packages a deterministic public-news fixture under the E2E source set only.

Production and normal debug builds must not contain or load the fixture. The E2E repository loads the fixture on first launch, while subsequent reads use the same cache and UI paths as production. No API keys, tokens, or personal data are used.

## Visual Regression

Paparazzi tests render the approved calm split-view design with fixed sample stories in these states:

- 2560 x 1600 landscape proportions.
- 1600 x 2560 portrait proportions.
- Light and dark system themes.
- Empty/offline state.
- Long Chinese title and enlarged font stress state.

Golden images are version-controlled. CI verifies screenshots and uploads the comparison report when verification fails.

## Runtime E2E Flows

Maestro runs against Android 35 and verifies:

- App installs and launches without a crash.
- Notification permission can be handled on Android 13+.
- Today shows deterministic fixture headlines.
- Category selection filters the list.
- Opening a story shows sources and the original-link action.
- Favorite toggling makes the story appear in Favorites.
- Settings accepts and saves a feed URL without transmitting credentials.
- Relaunch preserves cached content and favorites.
- Portrait navigation opens and closes story detail correctly.

The workflow captures `logcat`, screenshots, and Maestro output on failure. Tests fail on an uncaught application exception or missing expected UI text.

## GitHub Actions

Add a dedicated `android-ui-tests.yml` workflow triggered manually and on relevant source changes. It uses:

- JDK 17 and Gradle 8.10.2.
- `reactivecircus/android-emulator-runner@v2` with API 35 and x86_64.
- A tablet-sized AVD profile or explicit 2560 x 1600 resolution and density.
- Maestro CLI installed from its official GitHub release mechanism.
- Gradle and Android emulator caching where safe.

Third-party actions must be pinned to immutable commit SHAs before finalization. Workflow permissions remain `contents: read`.

## Failure Handling

- Network access is not required after dependencies and tools are installed.
- Fixture parsing failure blocks the E2E build.
- Emulator boot timeout, APK installation failure, application crash, screenshot mismatch, or Maestro assertion failure fails CI.
- Logs and screenshots are uploaded with a short retention period for diagnosis.
- Flaky retries are not added initially; failures must remain visible until a concrete flaky cause is identified.

## Acceptance Criteria

- Local JVM visual tests can run without an emulator.
- GitHub Actions can build and install the E2E APK on Android 35.
- All listed Maestro flows pass using only the packaged fixture.
- Redmi Pad Pro landscape and portrait screenshots are produced and verified.
- Normal debug and release artifacts contain no E2E fixture.
- The existing build, Lint, and Python pipeline tests continue to pass.
