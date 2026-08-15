# TrendRadar Android Implementation Plan

## Milestone 1: Reproducible Project Skeleton

- Create a single-module Kotlin Android application using Gradle Kotlin DSL.
- Target Android SDK 35 with minSdk 26 and Java 17 bytecode.
- Add Compose, Material 3, adaptive navigation, Room, WorkManager, Kotlin serialization, and OkHttp.
- Configure debug APK output and a GitHub Actions Android build workflow.
- Verify Gradle configuration with the Android Studio bundled JDK and SDK.

## Milestone 2: Static Feed Pipeline

- Define a versioned JSON contract shared by generator fixtures and Android models.
- Implement a Python standard-library exporter that normalizes, deduplicates, categorizes, ranks, and validates public headlines without AI.
- Add deterministic fixtures and unit tests for ranking, category assignment, HTTPS filtering, and output stability.
- Add a scheduled GitHub Actions workflow that runs collection, publishes `manifest.json`, current feeds, and briefs to GitHub Pages, and supports optional upstream/AI configuration through Secrets.
- Keep generated content bounded to 30 days.

## Milestone 3: Android Data Layer

- Implement network DTO parsing, endpoint fallback, and content validation.
- Store news, sources, briefs, favorites, read state, and sync metadata in Room.
- Replace snapshots transactionally and retain the previous valid cache on failure.
- Implement repositories for today, categories, briefs, search, favorites, and settings.
- Add WorkManager periodic sync and local notification deduplication.

## Milestone 4: Tablet Product UI

- Implement the approved calm split-view design with an adaptive navigation rail.
- Add Today, Categories, Briefs, Favorites, Search, Story Detail, and Settings destinations.
- Support landscape two-pane and portrait single-pane behavior.
- Add loading, empty, stale, offline, and malformed-feed states.
- Support system light/dark themes, scalable type, keyboard navigation, and Android back behavior.

## Milestone 5: Verification And Delivery

- Run Python pipeline tests and generate a representative static feed.
- Run Android JVM tests and lint where supported.
- Build the debug APK using the local Android Studio JDK and SDK.
- Inspect APK contents for accidental secrets.
- Copy the APK and setup guide to `outputs/`.
- Document GitHub repository variables, Pages setup, HyperOS battery settings, and installation steps.

## Verification Commands

```powershell
python -m unittest discover -s pipeline/tests -v
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Acceptance Gate

Implementation is complete only when the app builds, the pipeline tests pass, the sample feed parses into the app, offline cached content remains readable, AI is disabled by default, and an installable APK is present in `outputs/`.
