# JuWen Editorial Redesign and Live Feed Implementation Plan

## Objective

Implement the approved Editorial Calm / Compact Scan redesign, deployable GitHub Pages feed, resilient cache behavior, and complete verification for the Redmi Pad Pro release APK.

## 1. Feed Contract and Pipeline

- Extend feed metadata with collection status and source diagnostics while retaining schema compatibility.
- Isolate per-source fetch failures and enforce the minimum publication threshold.
- Support an ordered list of independently configurable NewsNow-compatible endpoints rather than one hard-coded host.
- Add tests for partial success, total failure, minimum story threshold, diagnostics, and non-empty publication protection.
- Change the scheduled workflow to 30 minutes and pin every action to an immutable commit SHA.
- Keep manual `workflow_dispatch` support and publish only validated output.

Verification: Python unit tests pass; an insufficient collection exits non-zero before Pages upload.

## 2. Android Data State and Cache

- Extend parsed feed models with collection metadata.
- Introduce explicit refresh/result state with generated time, checked time, cache state, and user-safe failure reason.
- Validate HTTPS endpoints and test before saving.
- Preserve the last-known-good file using atomic replacement.
- Load a release baseline snapshot only when no cache exists; keep the E2E fixture isolated.
- Set the default endpoint to the user's GitHub Pages URL.

Verification: parser/repository tests cover valid metadata, invalid endpoint, stale data, parser failure, and preservation of cached content.

## 3. Editorial UI System

- Split the monolithic activity UI into theme, shell, news list/detail, status, and Settings components.
- Implement the compact ranked list, stable narrow rail, quiet detail typography, distinct selected state, and meaningful no-data/offline states.
- Add accurate generated/checked timestamps, progress feedback, partial-source warnings, and manual refresh.
- Implement single-column portrait navigation and preserve state across rotation.
- Keep touch targets at least 48 dp and ensure status meaning is not color-only.

Verification: compile, lint, manual review, and accessibility-oriented layout inspection.

## 4. Visual and Runtime Regression Tests

- Update Paparazzi tests and goldens for Redmi Pad Pro landscape light/dark, long compact list, partial/offline, no-data, selected detail, and portrait.
- Update E2E fixture metadata and Maestro selectors for the redesigned UI.
- Cover refresh feedback, detail, favorite persistence, Settings endpoint validation/connection testing, rotation, and relaunch.

Verification: Paparazzi verification, debug/E2E assembly, lint, and pipeline tests pass locally; emulator E2E runs in GitHub Actions.

## 5. Publish, Verify, and Package

- Configure the local remote for `https://github.com/jasonmark808/juwen-android.git`.
- Push commits when GitHub credentials and network access are available.
- Enable/verify Pages through the Actions deployment and confirm the live feed returns valid non-empty JSON.
- Build and sign the release APK, verify its signature and asset isolation, and place it in `outputs/juwen-redmi-pad-pro.apk`.

Verification: live Pages URL succeeds, clean install can replace baseline content with live content, Actions are green, and SHA-256 is reported for the signed APK.

