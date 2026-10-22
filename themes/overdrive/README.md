# Overdrive

A low-memory CleanYourTV theme preset for TVs with about 1 GB RAM, styled as a calm, nature-first glass interface.

## Design choices

- Landscape-first home layout with bright nature scenes behind a restrained glass UI.
- Twelve local nature wallpapers rotate every 45 minutes; no network request is needed.
- The focused-app glass rim and glow sample the current wallpaper once when that image is decoded, so ocean, sunset, forest, and sky scenes can shift the accent colour.
- Wallpaper decoding uses RGB_565 by default (about 4 MB for a 1080p bitmap), avoids real-time blur, and keeps reduced motion enabled.
- Four-column app grid, compact system stats, and a minimal top bar.
- Prayer times, countdown, optional Ramadan imsak, and a daily Quran verse remain native Android views.

The dock lists YouTube, Netflix, Prime Video and Disney+. Missing apps are skipped automatically.

## Build and install without a PC

The branch includes a GitHub Actions workflow that runs launcher unit tests, lint, release build, and an APK-size check. When the run succeeds, open the [Overdrive build workflow](https://github.com/Demikians/cleanyourtv/actions/workflows/overdrive-build.yml), open the latest successful run, and download the `demi-kianz-nero-overdrive-apk` artifact. Extract the ZIP to get `app-release.apk`.

You can copy that APK to a USB drive and install it from the TV's file manager, if the TV permits installation from that source. The APK's built-in defaults include the Demi Kianz × Nero branding, prayer card, compact stats, and reduced motion. The nature wallpaper collection and its JSON theme configuration are stored in `themes/overdrive`; applying those external theme files still requires copying them to the launcher's external theme folder or using the project's theme deployment tools.

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

The Overdrive theme enables a compact, native prayer-and-verse card. It uses Android views, without a WebView, network calls, animated wallpaper, or a background service. The small gear on the card opens TV-remote-friendly settings for location label, latitude/longitude, calculation method, Ramadan mode, and the minutes between imsak and Subuh. Choices are stored locally on the TV.

Theme defaults:

- Brand: `DEMI KIANZ × NERO` — `STAY IN YOUR ZONE · LIVE IN BALANCE`
- Prayer calculation: offline solar approximation using the Kemenag Indonesia angle convention; coordinates are editable on the TV
- Ramadan: follows the device's Hijri calendar by default; can be forced on/off manually because local moon-sighting dates can differ
- Imsak: shown only when Ramadan mode is active; defaults to 10 minutes before Subuh
- Daily verse: rotates locally by date from a small set of Indonesian translations and verse references
- System stats: native launcher readings when supported by the TV

**Accuracy note:** prayer times are estimates, not a live feed or a guarantee of the official local Kemenag timetable. Set the coordinates to the actual home location and compare the display with your local mosque/Kemenag schedule before relying on it for worship.
