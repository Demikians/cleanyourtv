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
