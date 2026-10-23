# Demi Kianz × Nero — completion checklist

This checklist is the source of truth for the Overdrive branch. Do not start the release APK workflow until the user explicitly approves **FIX JADI**.

## Implemented in source

- [x] Default visual language uses deep black/emerald with a restrained gold accent; wallpaper-derived accent updates the focus glass and prayer panel.
- [x] A clean APK install gets twelve small, locally generated landscape wallpapers; the curated twelve JPG collection still takes precedence when the Overdrive theme folder is installed.
- [x] Wallpapers rotate locally, decode one image at a time, use RGB_565 by default, and do not require network access.
- [x] Wallpaper rotation timing is persisted and rechecked on Home resume; existing helper tests cover sequential/shuffle selection, interval boundaries and clock rollback.
- [x] Adaptive palette is applied only after the matching wallpaper is displayed, preventing a late decode from tinting the UI with a stale image.
- [x] The currently displayed wallpaper bitmap is detached and released when the launcher Activity is destroyed.
- [x] Wallpaper choices are exposed in launcher settings even when the external theme bundle is absent.
- [x] Clock/date header and compact CPU, free-memory, and free-storage stats are present in source.
- [x] System stats are read on a background thread while Home is resumed.
- [x] Cinematic-shelf artwork cache detaches its ImageView and promptly recycles native bitmaps on low-memory trim, app-list rebuild, and Activity destruction.
- [x] Prayer-times card is reduced in width, padding and text size; the next-prayer highlight follows the active wallpaper accent.
- [x] App tiles are modestly larger (200 dp wide) and icon-only apps use 60 dp artwork for TV-distance readability.
- [x] App Library uses the top-bar button when available instead of showing a duplicate floating footer.
- [x] Existing D-pad/remote navigation and launcher settings are retained.
- [x] Release workflow is manual-only to prevent an APK being produced before approval.
- [x] Source-validation workflow runs unit tests and lint without assembling an APK.


## Full agreed-plan cross-check

This section cross-checks the accepted lightweight Demi Kianz × Nero plan against the actual source, not only the visual theme.

- [x] Minimal dark emerald/black design with restrained gold, clock/date, compact locale-aware month calendar, and one-line CPU/RAM/storage readings.
- [x] Lightweight local wallpapers with 45-minute rotation in the Overdrive preset; no live video, WebView, or real-time blur.
- [x] Manually configurable app order through the launcher reorder mode; configured dock favorites are read from the theme and missing apps are skipped.
- [x] Native prayer panel: Subuh–Isya times, next-prayer countdown, editable location label/coordinates, calculation method, and locally saved preferences.
- [x] Daily rotating Indonesian Quran verse/reference; Ramadan imsak display can follow Hijri month or be forced on/off, with editable minutes before Subuh.
- [x] Remote-accessible quick settings for TV settings, display, network, app management, and launcher settings.
- [x] App-management shortcut now explains that app info can expose cache clearing and force-stop where the TV firmware supports those controls.
- [ ] Directly clearing another app's cache or force-stopping arbitrary background apps from inside this launcher is not implemented: ordinary Android apps cannot reliably do that without privileged/system access. The shortcut opens the TV's app-management settings instead; Android/TV firmware controls which actions are available. YouTube is not force-stopped automatically.
- [ ] Weather data panel is not in the accepted final minimal home-screen composition. Weather was discussed earlier, but was deferred from the final layout to avoid adding an unverified network data source or extra background work to a 1 GB RAM TV. Do not count it as a final-design blocker unless it is explicitly brought back into scope.

## Outstanding implementation

- [x] Add a compact local month-calendar grid beneath the date, with localized month title, weekday initials ordered by the locale's first day of week, and today's date highlighted in gold; it is non-focusable and refreshes with the existing minute tick.
- [x] Keep the optional race-weekend panel in the vertical home layout flow so it cannot float over the mini calendar, stats, or first app row.
- [ ] Verify the complete home-screen composition at native 16:9 resolution on the target TV; source layout is improved, but visual verification remains outstanding.

## Required final verification

These items cannot be truthfully marked complete from source inspection alone. Run them only after the user approves **FIX JADI** and the APK is built:

- [ ] CI: `testReleaseUnitTest`, `lintRelease`, `assembleRelease`, and APK-size check all pass.
- [ ] On the Polytron PLD 32BAG9858 (Android TV 11, 1 GB RAM): first launch shows wallpaper, clock/date, stats, apps, compact calendar, and compact prayer card together.
- [ ] Verify no clipping or overlap at native 16:9 resolution and no red artifact at the bottom-right.
- [ ] Verify remote focus order, top-bar App Library, settings, wallpaper picker and back navigation.
- [ ] Leave Home open for at least 45 minutes to confirm rotation and adaptive accent change.
- [ ] Confirm app remains responsive after opening and returning from several apps, and after TV restart.

## Build gate

The workflow at `.github/workflows/overdrive-build.yml` is intentionally manual-only. No release APK should be built until explicit user approval.
