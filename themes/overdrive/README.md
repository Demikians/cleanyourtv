# Overdrive

A low-memory, cinematic CleanYourTV theme preset for TVs with about 1 GB RAM.

## Design choices

- Cinematic app-art shelf and horizon glow for a big-screen look.
- Four-column grid for readable app tiles at 1366×768.
- CPU, free-memory and free-storage line enabled under the clock.
- Minimal top bar: Inputs, network and Settings are grouped into one menu.
- App-library enabled so installed apps can be found without filling Home with every tile.
- No wallpaper bitmap, video, blur or advanced effects; the launcher keeps its lightweight rendering path.
- Reduced motion is enabled to keep navigation smooth on modest TV hardware.

The dock lists YouTube, Netflix, Prime Video and Disney+. Missing apps are skipped automatically.

## Apply

From a computer with this repository and ADB configured, run:

```sh
TV_HOST=<TV-IP> tools/theme.sh themes/overdrive
```

On Windows:

```powershell
powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost <TV-IP> themes\overdrive
```

This is a theme preset only. It does not install an APK, disable packages, or change the TV's default launcher. The clock and system stats are native launcher features. Prayer times are not included in this preset because the launcher currently has no built-in prayer-time data source or prayer-time module.


## Demi Kianz × Nero home

The Overdrive theme also enables a compact, native prayer-and-verse card. The card is built from Android views (no WebView, network calls, animated wallpaper, or background service) to keep the launcher suitable for low-memory TVs. The small gear on the card opens TV-remote-friendly settings for location label, latitude/longitude, calculation method, Ramadan mode, and the minutes between imsak and Subuh. These choices are stored locally on the TV.

Theme defaults:

- Brand: `DEMI KIANZ × NERO` — `STAY IN YOUR ZONE · LIVE IN BALANCE`
- Prayer calculation: offline solar approximation using the Kemenag Indonesia angle convention, with location coordinates editable on the TV
- Ramadan: follows the device's Hijri calendar by default; can be forced on/off manually because local moon-sighting dates can differ
- Imsak: shown only when Ramadan mode is active; defaults to 10 minutes before Subuh
- Daily verse: rotates locally by date from a small set of short Indonesian translations and verse references
- System stats: small native launcher line; its values are real launcher readings when supported by the TV

**Accuracy note:** the prayer card calculates estimated times offline. It is not a live feed or a guarantee of the official local Kemenag timetable. Verify the coordinates and compare the displayed times with your local mosque/Kemenag schedule before relying on them for worship. The initial coordinates are approximate for West Java and should be set to the actual home location in the card settings.
