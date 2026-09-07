# Snatch

A fast, no-frills media downloader for Android. Share any video, photo, or audio link straight from Instagram, TikTok, YouTube, and more — Snatch grabs it in the background, tracks it in a proper download manager, and lets you play it back right inside the app.

No ads, no watermarks, no middleman website.

---

## Features

- **Share-target downloading** — share a link from any app; no need to open Snatch first
- **Four dedicated share-sheet entries** — default (uses your Settings preference), plus one-tap 480p / 720p / 1080p buttons with automatic quality fallback (e.g. tapping 480p tries ≤480p → ≤720p → ≤1080p → best, in that order, if a lower quality isn't available)
- **Audio-only and Photo-only share targets** — extract just the audio (MP3), or grab a single image
- **Instagram carousel support** — a multi-item post is automatically split into one download per item, with each item's real type (photo vs. video) detected individually
- **Persistent download manager** — every download's status (queued, downloading, paused, completed, failed) survives app restarts, backed by a local Room database
- **Pause / Resume / Delete** — from the in-app list *or* directly from the notification, with working cancellation that actually stops in-progress work (not just a UI change)
- **Up to 2 parallel downloads** on Wi-Fi (1 on mobile data), processed as a real queue
- **Live progress** — percentage, real-time speed, downloaded/total size, and ETA, updating in the notification and in-app simultaneously
- **In-app video player** — built on ExoPlayer (Media3), no need to hand off to another app
- **Automatic local thumbnails** — generated directly from the downloaded file (same technique file explorers use), no dependency on a remote CDN
- **Custom storage location** — pick any folder via the system folder picker; Snatch auto-creates and reuses a `Snatch` subfolder inside it. First-run prompt on install, changeable anytime from Settings
- **Network resilience** — automatic retry with exponential backoff on connection drops (including handling YouTube's own rate-limiting), with real resume-from-where-it-left-off support
- **Device-compatible encoding** — prefers H.264 video + AAC audio for maximum playback compatibility across stock Android players, and ensures files are properly seekable (faststart) right after download

---

## Tech stack

| Purpose | Library |
|---|---|
| Extraction & download engine | [youtubedl-android](https://github.com/junkfood02/youtubedl-android) (bundles yt-dlp + Python + ffmpeg) |
| Local database | Room (via KSP) |
| Video playback | AndroidX Media3 (ExoPlayer) |
| Image loading | Coil |
| UI | Material Components, ViewBinding |
| Language | Kotlin, Coroutines |

**Build config:** AGP 8.7.0 · Kotlin 1.9.24 · compileSdk/targetSdk 35 · minSdk 24 · `arm64-v8a` only (see [Known limitations](#known-limitations))

---

## Architecture

```
ShareReceiverActivity (+ 5 activity-alias share targets)
        │  inserts a queued DownloadRecord into Room, starts the service
        ▼
DownloadService (foreground service)
        │  drains the queue (1–2 at a time depending on network),
        │  runs yt-dlp (or a direct HTTP fetch for Instagram items),
        │  posts live notifications, updates Room as it goes
        ▼
DownloadsActivity (home screen)
        │  observes Room via Flow, renders the live list
        ▼
PlayerActivity / SettingsActivity
```

- **`ShareReceiverActivity`** — invisible, appears in the Android share sheet. Extracts the URL, detects which quality/type button was tapped (via which `activity-alias` launched it), inserts a DB record, and closes instantly.
- **`DownloadService`** — the core worker. Manages a persistent queue, pause/resume/delete via yt-dlp process control, retry logic, notification channels (a silent progress channel + an alerting result channel), and — for Instagram specifically — an independent direct-fetch fallback that bypasses yt-dlp's extractor when it misclassifies photo/video items.
- **`DownloadsActivity`** — the home screen. A live-updating list (Room `Flow`), a paste-a-link box for manual downloads, and access to Settings.
- **`PlayerActivity`** — a minimal ExoPlayer-backed screen for in-app playback.
- **`Settings` / `SettingsActivity`** — default quality preference and custom storage folder, persisted via SharedPreferences.

---

## Setup

1. Clone the repo and open it in Android Studio (Koala or newer recommended).
2. Let Gradle sync — dependencies resolve from Google, Maven Central, and the Gradle Plugin Portal (no JitPack needed).
3. Generate a launcher icon via **Image Asset Studio** if one isn't already present (not tracked in version control by default for binary-asset reasons).
4. Run on a physical device via USB debugging (emulators work but real-device testing is recommended given the native yt-dlp/ffmpeg binaries involved).

No API keys or backend required — everything runs locally on-device.

---

## Known limitations

- **`arm64-v8a` only.** The APK is deliberately built for a single architecture to keep size down (bundling yt-dlp + Python + ffmpeg for all 4 architectures previously pushed the app past 200MB). This covers virtually all Android devices sold since ~2017, but it will not install on very old 32-bit devices or x86 emulators/Chromebooks.
- **Instagram carousel/type detection is best-effort.** Instagram's page structure isn't a published, stable API. Snatch first tries a direct fetch of the post's own embedded data (bypassing yt-dlp's Instagram extractor), falling back to yt-dlp's own detection if that comes back empty. Either layer can occasionally misclassify or fail if Instagram changes its page structure — this is an ongoing area of refinement, not a "solved forever" feature.
- **Audio extraction from a carousel item is not yet supported.** If "Audio" is requested for a carousel that turns out to contain a video item fetched via the direct-Instagram path, that item currently saves as a video rather than an extracted audio track.
- **Total download size shown live is sometimes an estimate.** When the source platform doesn't report an exact size upfront (common on Instagram/TikTok), the live total is inferred from current progress and refines as the download proceeds; the final saved file's size is always exact.
- **First download after each app/process restart is slower** — this is when the bundled yt-dlp checks for an update (throttled to once per 24 hours) and the Python runtime initializes.

---

## Permissions used

| Permission | Why |
|---|---|
| `INTERNET` | Downloading media |
| `ACCESS_NETWORK_STATE` | Deciding parallel-download count based on Wi-Fi vs. mobile data |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | Keeping downloads running reliably in the background |
| `POST_NOTIFICATIONS` | Progress/result notifications (Android 13+) |
| `WRITE_EXTERNAL_STORAGE` (maxSdk 28 only) | Legacy storage write on very old Android versions; modern versions use scoped storage / MediaStore |

---

## Roadmap ideas

- Custom-styled in-app player controls (currently stock ExoPlayer UI)
- Grouped "N of M complete" progress for carousel downloads, rather than last-item-wins
- Audio extraction support for video items inside a carousel
- Broader architecture support if there's real demand beyond `arm64-v8a`

---

## Disclaimer

Snatch is a personal-use tool for downloading content you have the right to access and save. Respect the terms of service of the platforms you use it with, and the rights of content creators.
