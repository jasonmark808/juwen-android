# JuWen Editorial Redesign and Live Feed Design

## Status

Approved in conversation on 2026-08-15. This specification replaces the visual and live-feed portions of the earlier tablet-reader design while preserving its AI-free core.

## Goals

- Deliver a top-tier editorial news reader optimized for Redmi Pad Pro landscape use.
- Make scanning fast without sacrificing a calm, Apple-inspired reading experience.
- Publish fresh news without a paid server and without DeepSeek or any other required AI service.
- Make update state, stale data, and failures understandable inside the app.
- Never replace usable cached news with an empty or failed collection.

## Scope

The release includes Today, Categories, Briefs, Favorites, Search, article detail, source links, manual refresh, automatic cache refresh, data-source diagnostics, light/dark themes, and responsive landscape/portrait layouts.

Accounts, comments, social features, AI summaries, and server-triggered refresh from the app are out of scope.

## Visual Direction

The selected direction is **Editorial Calm with Compact Scan density**.

### Landscape Layout

The primary Redmi Pad Pro layout has three regions:

1. A narrow, stable navigation rail for primary destinations.
2. A compact ranked-news list showing approximately 8-11 items per viewport.
3. A quieter article-detail pane with more generous measure and spacing.

The list uses rank, headline, category, source count, and importance as its scanning hierarchy. The detail pane uses editorial typography and progressive disclosure for metadata and source links. List density must not leak into the reading pane.

### Portrait Layout

Portrait uses a single-column list. Selecting an item navigates to a full-screen detail view with a conventional back action. State and selection survive rotation.

### Visual System

- Neutral black, white, and system-gray surfaces with restrained blue for actionable emphasis.
- No decorative gradients, floating page-section cards, or ornamental blobs.
- Editorial headlines use a bundled, redistribution-safe Chinese serif face with a system-serif fallback; controls, metadata, and body copy use the Android system sans serif for legibility and predictable rendering.
- Information priority is expressed through size, weight, spacing, iconography, and text, never color alone.
- Body text meets 4.5:1 contrast; large text and controls meet at least 3:1.
- Touch targets are at least 48 dp.
- Light, dark, and increased text-size layouts must reflow without clipping.

## Information Architecture

### Permanent Information

The news screen permanently exposes the current destination, last feed generation time, current item count, and refresh entry point. These answer the user's immediate questions: what am I viewing, how current is it, and can I update it?

### Contextual Information

Collection health, partial-source failures, cache age, and detailed errors appear only when relevant. Full connection diagnostics live in Settings.

### Primary Actions

Opening a story, favoriting, changing destination, searching, and refreshing remain reachable within two taps. Settings is available from every primary screen.

## Refresh Experience

- App launch renders the last valid local snapshot immediately, then checks for a newer published snapshot.
- Pull-to-refresh and a toolbar refresh button both fetch the latest GitHub Pages snapshot immediately.
- A refresh action is debounced while one is already running.
- Success updates generation time, fetch time, item count, and content atomically.
- Failure keeps the previous snapshot and reports that cached content is being shown.
- The UI distinguishes: updating, current, partially collected, offline cache, stale data, and no usable data.
- “Data generated at” and “checked at” are shown separately so a successful network request with unchanged content is not misleading.

The app does not trigger GitHub Actions directly. Embedding a GitHub credential in the APK is prohibited.

## Feed Publication Architecture

GitHub Actions runs every 30 minutes and also supports manual `workflow_dispatch`. It produces static JSON and deploys it through GitHub Pages at:

`https://jasonmark808.github.io/juwen-android/feeds/latest.json`

The Android release uses that URL as its default endpoint. A user-configured HTTPS endpoint may override it from Settings.

### Collection Contract

Collection is deterministic and AI-free. Each configured source is fetched independently through a source adapter with a timeout, schema validation, and normalized output. The first release targets the approved set of Chinese hot-list/news sources: Toutiao, Baidu Hot Search, WallstreetCN, The Paper, Bilibili Hot Search, CLS, iFeng, Tieba, Weibo, Douyin, and Zhihu.

A single public aggregation endpoint must not be the only source of truth. The implementation may use an aggregation endpoint as one adapter, but must also support independently configurable adapters or endpoint groups. A failed source records diagnostics and does not abort other collectors.

Publication succeeds only when all of the following hold:

- At least one source succeeds.
- At least 10 valid HTTPS stories exist after normalization.
- The generated JSON passes schema validation.

If those conditions are not met, the workflow fails and leaves the previously deployed Pages artifact untouched. It must never publish an empty feed over a valid one.

### Feed Metadata

The snapshot includes schema version, generation time, collection status, successful source count, failed source count, source diagnostics, and stories. Each story includes stable ID, title, category, publication/collection times, importance, deterministic score, and one or more source links.

## Local Cache and First Run

The app maintains a last-known-good snapshot using an atomic temporary-file replacement. Parser or network failures cannot corrupt the existing cache.

The release APK includes a valid baseline snapshot so first launch is visually complete while the live feed is fetched. It is labeled with its real generation time and may not masquerade as current data. Test fixtures remain isolated to the E2E build and must not ship in the release APK.

## Settings and Diagnostics

Settings provides:

- Current feed URL.
- A connection-test action.
- Last successful generation and fetch times.
- Current cache age and story count.
- Source collection health supplied by feed metadata.
- A concise error explanation and retry action.
- Restore-default-endpoint action.

Saving a new URL validates HTTPS syntax first and then performs a connection test. A failed test does not discard the previous working endpoint.

## Error Vocabulary

- **Updating:** progress indicator and disabled duplicate refresh action.
- **Current:** subtle confirmation with generation time.
- **Partial:** warning icon plus successful/failed source counts; stories remain usable.
- **Offline cache:** explicit cached-data label and last successful time.
- **Stale:** warning when feed generation is older than the agreed freshness threshold of 90 minutes.
- **No usable data:** focused empty state with retry and Settings actions, not a blank screen.

Errors use actionable Chinese copy and never expose stack traces to users. Technical details remain available in diagnostics and CI logs.

## Testing and Acceptance

### Android

- Paparazzi golden tests cover Redmi Pad Pro landscape light/dark, compact long-title lists, selected detail, partial/offline state, no-data state, and portrait detail.
- Unit tests cover atomic cache replacement, URL validation, stale-state calculation, parser failures, and preservation of last-known-good data.
- Maestro covers launch, manual refresh, story detail, favorite persistence, Settings connection test, rotation, and relaunch.
- Release verification confirms the APK contains the baseline snapshot but no E2E fixture or test-only build flag.

### Pipeline

- Unit tests cover normalization, categorization, stable IDs, source failure isolation, minimum-publication threshold, and diagnostic metadata.
- A workflow test proves that zero or insufficient stories fails before deployment.
- GitHub Actions runs pipeline tests, Android build, lint, Paparazzi verification, and emulator E2E tests.
- Third-party GitHub Actions are pinned to immutable commit SHAs.

### Release Acceptance

The release is accepted only when:

1. The Pages feed URL returns valid non-empty JSON.
2. A clean app install shows baseline content, then successfully replaces it with live content.
3. Manual refresh reports accurate generated/checked times.
4. Offline and source-failure tests preserve readable cached news.
5. Redmi Pad Pro landscape and portrait visual tests show no overlap, clipping, or unreadable text.
6. The signed release APK passes signature verification and contains no required AI dependency.
