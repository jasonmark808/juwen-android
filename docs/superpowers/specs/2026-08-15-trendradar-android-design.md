# TrendRadar Android Personal News Reader Design

## 1. Objective

Build a personal Android news reader for Redmi Pad Pro. GitHub Actions runs TrendRadar on a schedule, publishes a compact public news feed, and the native Android app downloads that feed for browsing, search, offline reading, and local notifications.

The system must have no server hosting cost and must work without any AI provider. AI remains an optional enhancement behind a provider-neutral OpenAI-compatible interface.

## 2. Target And Constraints

- Primary device: Redmi Pad Pro running HyperOS/Android.
- Primary orientation: landscape tablet; portrait remains fully usable.
- Audience: one personal user, not a public content service.
- Runtime hosting: GitHub Actions and GitHub Pages only.
- Core operation: no DeepSeek or other AI dependency.
- Network: mainland China connectivity may be intermittent.
- License: any reused TrendRadar GPL-3.0 code must remain GPL-compliant when distributed.

## 3. System Architecture

### 3.1 Feed Pipeline

1. A scheduled GitHub Actions workflow runs every two hours and at dedicated morning and evening brief times.
2. TrendRadar collects configured public hot lists and RSS feeds.
3. A deterministic processing step normalizes, deduplicates, categorizes, and ranks stories.
4. Optional AI enrichment runs only when explicitly enabled and valid GitHub Secrets are present.
5. The workflow validates the output schema and publishes static JSON to a dedicated GitHub Pages branch.
6. The workflow retains current feeds and 30 days of briefs, replacing older generated data so repository history does not grow without bound.

### 3.2 Android Client

- Kotlin and Jetpack Compose with adaptive layouts.
- Room stores feeds, briefs, favorites, read state, and sync metadata locally.
- WorkManager performs best-effort periodic synchronization.
- The client tries a configurable mainland-accessible CDN URL first, GitHub Pages second, and Room cache last.
- Notifications are generated locally after a successful sync. No push provider is required.
- Secrets are never stored in the APK. AI processing, when enabled, happens only in GitHub Actions.

## 4. Deterministic News Processing

The default pipeline does not produce model-written summaries. It provides useful structure through reproducible rules:

- Normalize URLs and titles before deduplication.
- Group exact and near-duplicate headlines into one story cluster.
- Categorize with maintained keyword dictionaries for general, politics, finance, technology, society, automotive, and digital products.
- Rank using source position, recurrence, cross-platform source count, freshness, and configured source weight.
- Mark a story important only when it exceeds explicit cross-platform, rank, or recurrence thresholds.
- Build morning and evening briefs as ranked story lists with source count, rank movement, and timestamps.

All thresholds are versioned configuration. The generated feed records the ruleset version so behavior can be diagnosed.

## 5. Optional AI Enhancement

- Disabled by default.
- Uses one OpenAI-compatible endpoint contract rather than vendor-specific code.
- Configuration is held in GitHub Secrets and workflow variables.
- May add concise summaries, refined topic labels, and related-event links.
- Every enriched field records that it was AI generated.
- Timeout, quota exhaustion, invalid output, or provider failure falls back to deterministic output without failing collection or publication.
- The Android app clearly labels a feed as `Rules` or `AI enhanced`.

## 6. Published Data Contract

The static site publishes:

- `manifest.json`: schema version, generated time, ruleset version, mode, available categories, brief index, and content hashes.
- `feeds/latest.json`: current ranked story clusters.
- `feeds/<category>.json`: category subsets generated from the same snapshot.
- `briefs/YYYY-MM-DD-morning.json` and `briefs/YYYY-MM-DD-evening.json`.

Each story contains a stable ID, title, category, source entries, canonical HTTPS URL, publish and collection times, rank signals, importance flag, and optional AI fields. The feed contains public news metadata only. Favorites and reading history never leave the device.

## 7. Product Experience

### 7.1 Visual Direction

Use the approved "Calm Split View" direction: restrained Apple-inspired hierarchy adapted to Android conventions. The app uses generous spacing, a translucent navigation rail, system-aware light and dark colors, subtle motion, and no advertising or decorative dashboard metrics.

### 7.2 Screens

- **Today:** navigation on the left and the current brief or selected story list on the right in landscape.
- **Categories:** general, politics, finance, technology, society, automotive, digital, plus source filtering.
- **Briefs:** morning and evening archives grouped by date.
- **Story detail:** title, sources, timing, deterministic signals, optional summary, related coverage, and an explicit open-original action.
- **Search:** local search across cached titles, sources, and available summaries.
- **Favorites:** device-only saved and unread items.
- **Settings:** sync interval, notification windows, important-news alerts, endpoint override, theme, text size, and cache controls.

Landscape uses a stable two-pane layout. Portrait collapses to a single-pane navigation flow while preserving state. Android back behavior, keyboard navigation, touch targets, screen readers, and large text remain native.

## 8. Notifications

- Morning and evening briefs produce local notifications after corresponding data is downloaded.
- Important-news notifications are evaluated after each periodic sync and deduplicated by story ID.
- WorkManager timing is best effort; HyperOS may delay work. The UI must describe the last successful sync rather than promise real-time delivery.
- Users can manually refresh at any time.

## 9. Reliability And Security

- Validate schema version, required fields, content type, size limits, and hashes before database replacement.
- Write a new snapshot transactionally; malformed or partial downloads never replace the last valid cache.
- Allow only HTTPS original links and reject invalid hosts or schemes.
- Show quiet inline offline or stale-data status with the last successful update time.
- If all network endpoints fail, continue serving cached content.
- Keep API keys exclusively in GitHub Secrets and redact secrets from workflow logs.
- Use least-privilege workflow permissions and pin third-party GitHub Actions to immutable commit SHAs.

## 10. Testing And Acceptance

### 10.1 Automated Tests

- Feed schema and malformed-input tests.
- Normalization, deduplication, categorization, ranking, and importance-rule tests.
- Room migration and transactional snapshot tests.
- Endpoint fallback, stale cache, and interrupted-download tests.
- Search, filtering, favorites, read state, and notification deduplication tests.
- Compose UI tests for adaptive navigation and key user flows.

### 10.2 Device And Layout Verification

- Redmi Pad Pro landscape profile at 2560 x 1600 proportions.
- Portrait, split-screen, dark mode, large font, and offline states.
- HyperOS battery-restricted and unrestricted behavior documented and tested where hardware is available.

### 10.3 Completion Criteria

- A signed installable APK and reproducible source build.
- A documented GitHub Actions setup requiring no server purchase.
- Fresh data loads when either remote endpoint works and cached data remains readable when both fail.
- Core collection, briefs, search, favorites, and notifications work with AI disabled.
- No secret is present in source control, generated static data, or the APK.

## 11. Explicit Non-Goals

- Guaranteed real-time breaking-news delivery.
- Running the full Python TrendRadar process continuously on the tablet.
- User accounts, cross-device favorite sync, comments, social features, or public app-store launch.
- Scraping full article bodies or redistributing copyrighted article content.
