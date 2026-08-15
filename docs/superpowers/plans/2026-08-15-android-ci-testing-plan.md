# Android CI Testing Implementation Plan

## 1. E2E Build Isolation

- Add an `e2e` build type derived from debug with application ID `cc.juwen.reader.e2e`.
- Add `BuildConfig.E2E_FIXTURE`, false by default and true only for E2E.
- Package a deterministic HTTPS-only feed under `src/e2e/assets`.
- Load the fixture only when E2E is true and no cache exists.
- Build both debug and E2E variants to prove the source-set boundary.

## 2. Paparazzi Visual Tests

- Add Paparazzi 1.3.5 to the Gradle plugin catalog.
- Expose only the minimum internal Compose surface required for tests.
- Render a fixed `FeedSnapshot` at Redmi Pad Pro landscape and portrait proportions.
- Cover light, dark, long-title, and empty states.
- Record golden images and run verification locally.

## 3. Maestro Runtime Tests

- Add a Maestro 2.8.0 flow that clears state, grants permissions, launches the E2E app, validates fixture content, opens a story, toggles favorite, checks Favorites, opens Settings, and relaunches.
- Use visible Chinese labels rather than coordinates wherever possible.
- Add a second portrait flow for single-pane detail navigation.

## 4. GitHub Actions

- Add a read-only `android-ui-tests.yml` workflow.
- Pin checkout, setup-java, upload-artifact, and emulator-runner actions to commit SHAs.
- Build the E2E APK, boot API 35 x86_64 with KVM, install Maestro 2.8.0, and run flows.
- Upload Maestro output, screenshots, and logcat on failure.
- Run Paparazzi verification before emulator startup.

## 5. Verification

- Run Python tests, Gradle unit tests, Paparazzi record/verify, Lint, debug build, and E2E build.
- Inspect both APKs and verify the E2E fixture is absent from normal debug.
- Validate workflow YAML structure and search for unpinned actions or secrets.
- Commit only source, fixtures, golden images, and documentation; ignore test reports and emulator caches.
