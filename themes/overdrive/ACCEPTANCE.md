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
- [x] Cinematic-shelf artwork cache detaches its ImageView and promptly recycles native bitmaps on low-memory trim and app-list rebuild.
- [x] Prayer-times card is reduced in width, padding and text size; the next-prayer highlight follows the active wallpaper accent.
- [x] App tiles are modestly larger (200 dp wide) and icon-only apps use 60 dp artwork for TV-distance readability.
- [x] App Library uses the top-bar button when available instead of showing a duplicate floating footer.
- [x] Existing D-pad/remote navigation and launcher settings are retained.
- [x] Release workflow is manual-only to prevent an APK being produced before approval.
- [x] Source-validation workflow runs unit tests and lint without assembling an APK.

## Outstanding implementation

- [x] Add a compact local month-calendar grid beneath the date, with localized month title, weekday initials, and today's date highlighted in gold; it is non-focusable and refreshes with the existing minute tick.
- [ ] Review the home-screen composition at 16:9 so the calendar, clock, stats, apps, and prayer panel remain minimal and do not overlap or clip.

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
