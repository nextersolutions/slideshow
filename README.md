# Slideshow

A minimal Android signage-style app that fetches a playlist from
`https://test.onsignage.com/PlayerBackend/` and plays its images and videos
in a loop with crossfade transitions.

## How it works

### User flow

1. On first launch the app seeds the screen key to
   `7d47b6d7-8294-4b33-8887-066961d79993` (the test key from the spec) and
   persists it via DataStore.
2. The user can edit the key, tap **Save key** to persist it and trigger an
   immediate metadata sync, then tap **Start playback** once at least one
   media item has finished downloading.
3. While playing, a floating bar overlays **Stop** and **Skip**.

### Data flow

```
Network (DTO)        →  Repository         →  Room (Entity)       →  UseCase  →  UI (ViewData)
PlaylistResponseDto  →  PlaylistRepository →  PlaylistItemEntity  →  Observe- →  PlaylistItem-
                                                                    PlaylistUC    ViewData
```

- **`network/`** — Ktor 3.2.3 `HttpClient` + `SlideshowApi` with streaming
  file downloads. DTOs parse only `duration`, `creativeKey`, and `orderKey`
  as the spec requires; every other field is dropped via
  `ignoreUnknownKeys = true`.
- **`database/`** — Room entity/DAO/database. One row per playlist item
  keyed by `orderKey`, with a nullable `localPath` that the download worker
  fills in when a file lands on disk.
- **`data/`** — `PlaylistRepository` maps network DTO → Room entity,
  preserving existing `localPath` values for unchanged creatives.
  `FileManager` owns the on-disk media directory and prunes unreferenced
  files on each sync. `ScreenKeyPreferences` persists the screen key.
- **`domain/`** — `PlaylistItemViewData` (the shape the UI consumes),
  `PlaylistMappers` (DTO → entity → ViewData), and a small set of
  `@Inject`-friendly use cases: `ObservePlaylistUseCase`,
  `SyncPlaylistUseCase`, `SetScreenKeyUseCase`, `ObserveScreenKeyUseCase`.
- **`work/`** — Two `@HiltWorker` classes. `SyncPlaylistWorker` fetches
  metadata and enqueues `DownloadCreativeWorker` for each missing file.
  `PlaylistScheduler` manages scheduling: an immediate one-shot sync plus
  a self-rescheduling 1-minute `AlarmManager.setExactAndAllowWhileIdle`
  loop driven by `PlaylistAlarmReceiver`. (Using AlarmManager rather than
  `PeriodicWorkRequest` because the latter's minimum interval is 15 min.)
- **`di/`** — Hilt modules for Ktor `HttpClient` + Json, Room, and the
  `SlideshowApp : Configuration.Provider` wiring for `HiltWorkerFactory`.
- **`ui/player/SlideshowPlayer`** — Two-slot crossfade renderer. Images go
  through Coil; videos through Media3 ExoPlayer. Slots are keyed on
  `item.id` so the same ExoPlayer keeps playing across the A ↔ B role
  swap (no restart when a fading-in video becomes the foreground item).
- **`ui/SlideshowScreen`** — Entry UI with the screen-key input and
  Start/Stop/Skip controls.

### Crossfade behaviour

- Images hold for `duration` seconds; the fade-out into the next item
  occupies the final 500 ms of that window.
- Videos play to their natural end. The 500 ms crossfade begins
  `duration - 500 ms` before the video's end, so the incoming item fades
  in while the current video plays its tail frames. Metadata `duration`
  is ignored for videos per the task spec.

### Offline behaviour

Once metadata has been fetched and creatives downloaded, Room and the
on-disk media directory are the single source of truth. A launch with no
network connectivity simply reuses the last-known playlist; the sync
worker will retry on its next tick.

## Build

The project targets AGP 9.1.1, Kotlin 2.2.10, compileSdk 36, minSdk 29.
All dependency versions are pinned in `gradle/libs.versions.toml`
(Ktor 3.2.3, Hilt 2.57.2, KSP 2.2.10-2.0.2, Room 2.6.1, Media3 1.4.1,
Coil 2.7.0, WorkManager 2.9.1, DataStore 1.1.1).

```
./gradlew :app:assembleDebug
```
