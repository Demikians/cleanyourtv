<!-- theme-status: draft; no genuine on-TV screenshot captured yet. Do not promote to the public themes table until captured. -->
# CleanYourTV Forest Edition

A low-memory CleanYourTV theme preset for TVs with about 1 GB RAM, styled as a calm, nature-first glass interface.

## Design choices

- Landscape-first home layout with bright nature scenes behind a restrained glass UI.
- The theme starts with an included green tea-hills wallpaper. The existing wallpaper selector and the bundled nature wallpapers remain available, so the backdrop can be changed later without rebuilding the APK.
- Forest emerald is the fixed UI accent; it does not shift to gold or sample wallpaper colours. The supplied wallpaper can be changed from the existing on-TV picker.
- Wallpaper decoding uses RGB_565 by default (about 4 MB for a 1080p bitmap), avoids real-time blur, and keeps reduced motion enabled.
- Four-column app grid, compact system stats, and a minimal top bar.
- Prayer times use the Aladhan API with method 20 (KEMENAG Indonesia) and school 0. MyQuran API v3 supplies the daily Arabic verse and Indonesian translation, with the surah/ayah reference shown in the card.

The dock lists YouTube, Netflix, Prime Video and Disney+. Missing apps are skipped automatically.

## Build and install without a PC

CI runs unit tests and lint only; it does not build an APK during implementation. Release APK creation must wait for the project owner's explicit `FIX JADI` approval. Do not trigger a release build before then.

The preset and its wallpaper assets live in `themes/overdrive`. This theme remains draft-only until its UI is checked on the target television and a genuine 16:9 TV screenshot is captured; no screenshot should be fabricated.

## Apply theme configuration

From a computer with this repository and ADB configured, run:

```sh
TV_HOST=<TV-IP> tools/theme.sh themes/overdrive
```

On Windows:

```powershell
powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost <TV-IP> themes\overdrive
```

This preset does not disable packages or change the TV's default launcher by itself; install the matching APK first, then apply the theme.

## Demi Kianz × Nero home

The Overdrive theme enables a compact native prayer-and-verse card. HTTPS requests run on a daemon worker thread; there is no WebView, animated wallpaper, or persistent background service. Successful data is cached locally, old data is marked stale, and failed requests must not produce invented times or verse text. The small gear on the card opens TV-remote-friendly settings for location label, latitude/longitude, calculation method, Ramadan mode, and the minutes between imsak and Subuh. Choices are stored locally on the TV.

Theme defaults:

- Brand: `DEMI KIANZ × NERO` — `CleanYourTV · Forest Edition`
- Prayer calculation: Aladhan API with `method=20` (KEMENAG Indonesia) and `school=0`; use an already-authorized TV location when available, otherwise editable saved coordinates. The selected location never changes Android system time or time zone.
- Ramadan: follows the device's Hijri calendar by default; can be forced on/off manually because local moon-sighting dates can differ
- Imsak: shown only when Ramadan mode is active; defaults to 10 minutes before Subuh
- Daily verse: a deterministic daily reference is fetched from MyQuran API v3 and shown with returned Arabic text, Indonesian translation, surah/ayah reference, and source attribution. The last valid verse is cached and its original date is shown when stale.
- System stats: native launcher readings when supported by the TV

**Accuracy note:** Aladhan method 20 is the KEMENAG calculation method but is not a guarantee of exact agreement with every local mosque or official timetable. Verify the saved coordinates and compare with the local Kemenag schedule before relying on it for worship.
