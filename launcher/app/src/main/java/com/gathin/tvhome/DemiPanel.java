package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.Locale;

/** Low-cost native brand/prayer card. No WebView, network, artwork or animation loop. */
final class DemiPanel {
    private static final int GREEN = Color.rgb(143, 191, 159);
    private int accentColor = GREEN;
    private View cardView;
    private View accentDivider;
    private final java.util.List<TextView> accentTextViews = new java.util.ArrayList<>();
    private static final int MUTED = Color.rgb(164, 184, 176);
    private static final String PREFS = "demi_prayer_settings";
    // References are fixed; Arabic text and translation are fetched from MyQuran API v3.
    private static final int[][] VERSE_REFS = {
        {94, 5}, {2, 286}, {13, 28}, {2, 153},
        {39, 53}, {65, 3}, {2, 152}, {57, 4}
    };
    private static final long RETRY_AFTER_MS = 15L * 60L * 1000L;
    private static final String PRAYER_CACHE_LAST = "prayer_cache_last";
    private static final String QURAN_CACHE = "quran_cache";

    private final Context context;
    private final SharedPreferences prefs;
    private final String brandTitle;
    private final String brandTagline;
    private final boolean enabled;
    private final double defaultLatitude;
    private final double defaultLongitude;
    private final String defaultMethod;
    private final String defaultLocation;
    private final String defaultRamadan;
    private final int defaultImsakOffset;
    private final TextView locationView;
    private final TextView[] prayerTimes = new TextView[5];
    private final View[] prayerCells = new View[5];
    private final TextView imsakView;
    private final TextView countdownView;
    private final TextView arabicVerseView;
    private final TextView quoteView;
    private final TextView quoteSource;
    private final TextView imsakLabel;
    private double latitude;
    private double longitude;
    private String method;
    private String location;
    private String ramadanMode;
    private int imsakOffset;
    private volatile int[] apiPrayerMinutes;
    private volatile String apiPrayerDate = "";
    private volatile boolean apiPrayerFromCache = true;
    private volatile boolean apiPrayerOffline = true;
    private volatile int tomorrowMaghrib = -1;
    private volatile String tomorrowPrayerDate = "";
    private volatile String verseArabic = "";
    private volatile String verseTranslation = "";
    private volatile String verseReference = "";
    private volatile String verseDate = "";
    private volatile boolean verseFromCache = true;
    private volatile long lastRefreshAttempt;
    private volatile boolean refreshInFlight;

    DemiPanel(Context context, FrameLayout root) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONObject json = readThemeConfig(context);
        brandTitle = string(json, "brand_title", "DEMI KIANZ × NERO");
        brandTagline = string(json, "brand_tagline", "STAY IN YOUR ZONE · LIVE IN BALANCE");
        enabled = bool(json, "show_prayer_panel", true);
        defaultLatitude = number(json, "prayer_latitude", -6.9);
        defaultLongitude = number(json, "prayer_longitude", 107.1);
        defaultMethod = string(json, "prayer_method", "kemenag");
        defaultLocation = string(json, "prayer_location_label", "JAWA BARAT · WIB");
        defaultRamadan = string(json, "ramadan_mode", "auto");
        defaultImsakOffset = integer(json, "imsak_offset_minutes", 10, 0, 60);

        latitude = prefs.getFloat("latitude", (float) defaultLatitude);
        longitude = prefs.getFloat("longitude", (float) defaultLongitude);
        method = "kemenag"; // Use the user-approved Aladhan method 20.
        location = prefs.getString("location", defaultLocation);
        ramadanMode = prefs.getString("ramadan_mode", defaultRamadan);
        imsakOffset = prefs.getInt("imsak_offset", defaultImsakOffset);
        applyLastKnownLocationIfAllowed();
        loadCachedData();

        LinearLayout card = new LinearLayout(context);
        cardView = card;
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackground(roundRect(0xE90A100E, 0x558FBF9F, 18));
        card.setFocusable(false);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView accent = accentText("▎", 17, Typeface.BOLD);
        accent.setPadding(0, 0, dp(7), 0);
        header.addView(accent, wrap());
        TextView title = text("PRAYER TIMES", 12, Color.WHITE, Typeface.BOLD);
        title.setLetterSpacing(0.16f);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        locationView = text(location, 10, MUTED, Typeface.NORMAL);
        locationView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        locationView.setMaxWidth(dp(220));
        header.addView(locationView, wrap());
        TextView setup = accentText("  ⚙", 14, Typeface.BOLD);
        setup.setFocusable(true);
        setup.setClickable(true);
        setup.setPadding(dp(8), dp(4), 0, dp(4));
        setup.setOnClickListener(v -> showSettings());
        header.addView(setup, wrap());
        card.addView(header, wrap());

        LinearLayout cells = new LinearLayout(context);
        cells.setOrientation(LinearLayout.HORIZONTAL);
        cells.setPadding(0, dp(7), 0, dp(5));
        String[] names = {"SUBUH", "DZUHUR", "ASHAR", "MAGHRIB", "ISYA"};
        for (int i = 0; i < names.length; i++) {
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(3), dp(6), dp(3), dp(6));
            cell.setBackground(roundRect(0x251B2822, 0, 11));
            TextView name = text(names[i], 9, MUTED, Typeface.BOLD);
            name.setGravity(Gravity.CENTER);
            TextView time = text("--:--", 15, Color.WHITE, Typeface.NORMAL);
            time.setGravity(Gravity.CENTER);
            cell.addView(name, wrap());
            LinearLayout.LayoutParams timeLp = wrap();
            timeLp.topMargin = dp(4);
            cell.addView(time, timeLp);
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) cellLp.leftMargin = dp(4);
            cells.addView(cell, cellLp);
            prayerCells[i] = cell;
            prayerTimes[i] = time;
        }
        card.addView(cells, wrap());

        LinearLayout nextRow = new LinearLayout(context);
        nextRow.setGravity(Gravity.CENTER_VERTICAL);
        nextRow.setPadding(0, dp(4), 0, 0);
        TextView nextLabel = accentText("MENUJU MAGHRIB", 9, Typeface.BOLD);
        nextLabel.setLetterSpacing(0.12f);
        nextRow.addView(nextLabel, wrap());
        countdownView = text("", 11, Color.WHITE, Typeface.BOLD);
        countdownView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        countLp.leftMargin = dp(8);
        nextRow.addView(countdownView, countLp);
        card.addView(nextRow, wrap());

        LinearLayout imsakRow = new LinearLayout(context);
        imsakRow.setGravity(Gravity.CENTER_VERTICAL);
        imsakRow.setPadding(0, dp(4), 0, 0);
        imsakLabel = accentText("IMSAK", 9, Typeface.BOLD);
        imsakLabel.setLetterSpacing(0.12f);
        imsakRow.addView(imsakLabel, wrap());
        imsakView = text("", 11, Color.WHITE, Typeface.BOLD);
        imsakView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams imsakLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        imsakLp.leftMargin = dp(8);
        imsakRow.addView(imsakView, imsakLp);
        card.addView(imsakRow, wrap());

        View divider = new View(context);
        accentDivider = divider;
        divider.setBackgroundColor(0x338FBF9F);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(1));
        dividerLp.topMargin = dp(7);
        dividerLp.bottomMargin = dp(6);
        card.addView(divider, dividerLp);

        TextView verseHeading = accentText("AYAT HARIAN", 9, Typeface.BOLD);
        verseHeading.setLetterSpacing(0.14f);
        LinearLayout.LayoutParams verseHeadingLp = wrap();
        verseHeadingLp.bottomMargin = dp(5);
        card.addView(verseHeading, verseHeadingLp);
        arabicVerseView = text("", 18, Color.WHITE, Typeface.NORMAL);
        arabicVerseView.setGravity(Gravity.RIGHT);
        arabicVerseView.setTextDirection(View.TEXT_DIRECTION_RTL);
        arabicVerseView.setMaxLines(2);
        arabicVerseView.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams arabicLp = wrap();
        arabicLp.bottomMargin = dp(5);
        card.addView(arabicVerseView, arabicLp);
        quoteView = text("Ayat harian menunggu koneksi data.", 12, Color.rgb(238, 245, 241), Typeface.NORMAL);
        quoteView.setMaxLines(3);
        quoteView.setLineSpacing(dp(2), 1.0f);
        card.addView(quoteView, wrap());
        quoteSource = accentText("SUMBER: MYQURAN API V3", 9, Typeface.BOLD);
        quoteSource.setPadding(0, dp(5), 0, 0);
        quoteSource.setMaxLines(2);
        card.addView(quoteSource, wrap());

        if (enabled) {
            // Keep the prayer card on the right half of the 16:9 home screen.
            // The clock/calendar/stats occupy the left; pinning both groups to the left made
            // the entire composition look stacked in one corner on a TV.
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(390), ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.RIGHT);
            lp.rightMargin = dp(56);
            lp.topMargin = dp(142);
            root.addView(card, lp);
        } else {
            card.setVisibility(View.GONE);
        }
        update();
        refreshOnlineData(true);
    }

    String brandTitle() { return brandTitle; }
    String brandTagline() { return brandTagline; }
    boolean hasBrand() { return !brandTitle.trim().isEmpty(); }

    private TextView accentText(String value, float size, int style) {
        TextView view = text(value, size, accentColor, style);
        accentTextViews.add(view);
        return view;
    }

    /** Wallpaper accent changes are applied only when a new wallpaper is decoded. */
    void setAccent(int color) {
        if (color == 0) return;
        accentColor = color;
        for (TextView view : accentTextViews) view.setTextColor(color);
        if (cardView != null) cardView.setBackground(roundRect(0xB8182220, (color & 0x00FFFFFF) | 0x55000000, 18));
        if (accentDivider != null) accentDivider.setBackgroundColor((color & 0x00FFFFFF) | 0x33000000);
    }
    void update() {
        if (!enabled) return;
        maybeRefreshOnlineData();
        LocalDate today = LocalDate.now();
        String todayKey = today.toString();
        int[] times = apiPrayerMinutes;
        boolean scheduleFresh = times != null && times.length == 5 && todayKey.equals(apiPrayerDate);
        boolean hasSchedule = times != null && times.length == 5;

        String status = location;
        if (scheduleFresh) {
            if (apiPrayerFromCache || apiPrayerOffline) status += " · CACHE";
        } else if (hasSchedule) {
            status += " · DATA LAMA";
        } else {
            status += " · OFFLINE";
        }
        locationView.setText(status);

        int now = java.time.LocalTime.now().getHour() * 60 + java.time.LocalTime.now().getMinute();
        if (hasSchedule) {
            for (int i = 0; i < times.length; i++) prayerTimes[i].setText(PrayerTimes.format(times[i]));
        } else {
            for (TextView value : prayerTimes) value.setText("--:--");
        }

        // Maghrib is the fixed countdown target. Never calculate one from an old day's schedule.
        boolean beforeMaghrib = scheduleFresh && now < times[3];
        for (int i = 0; i < prayerCells.length; i++) {
            boolean selected = beforeMaghrib && i == 3;
            prayerCells[i].setBackground(roundRect(
                    selected ? ((accentColor & 0x00FFFFFF) | 0x33000000) : 0x251B2822,
                    selected ? ((accentColor & 0x00FFFFFF) | 0x88000000) : 0, 11));
            prayerTimes[i].setTextColor(selected ? accentColor : Color.WHITE);
        }
        if (!scheduleFresh) {
            countdownView.setText(hasSchedule
                    ? "JADWAL LAMA · SAMBUNGKAN INTERNET"
                    : "DATA TIDAK TERSEDIA · PERIKSA WIFI");
        } else if (now < times[3]) {
            setCountdown(times[3] - now);
        } else if (today.plusDays(1).toString().equals(tomorrowPrayerDate) && tomorrowMaghrib >= 0) {
            setCountdown((1440L - now) + tomorrowMaghrib);
        } else {
            countdownView.setText("MAGHRIB BERIKUTNYA · MENUNGGU DATA");
        }

        boolean ramadan = isRamadan();
        View imsakRow = (View) imsakView.getParent();
        imsakRow.setVisibility(ramadan ? View.VISIBLE : View.GONE);
        if (ramadan && hasSchedule) {
            imsakView.setText(PrayerTimes.format(PrayerTimes.imsak(times[0], imsakOffset))
                    + "  ·  " + imsakOffset + " menit sebelum Subuh"
                    + (scheduleFresh ? "" : " · DATA LAMA"));
        } else if (ramadan) {
            imsakView.setText("DATA TIDAK TERSEDIA");
        }
        renderVerse(today);
    }

    private void setCountdown(long remainingMinutes) {
        long bounded = Math.max(0L, remainingMinutes);
        countdownView.setText((bounded / 60) + "J "
                + String.format(Locale.ROOT, "%02d", bounded % 60) + "M");
    }

    private void renderVerse(LocalDate today) {
        if (verseTranslation == null || verseTranslation.trim().isEmpty()) {
            arabicVerseView.setText("");
            quoteView.setText("Ayat hari ini belum tersedia. Sambungkan TV ke internet.");
            quoteSource.setText("DATA TIDAK TERSEDIA · MYQURAN API V3");
            return;
        }
        arabicVerseView.setText(verseArabic);
        quoteView.setText("“" + verseTranslation + "”");
        boolean fresh = today.toString().equals(verseDate);
        String status = verseReference + " · Sumber: MyQuran API v3";
        if (!fresh) status += " · DATA TERSIMPAN TANGGAL " + verseDate + " (LAMA)";
        else if (verseFromCache) status += " · CACHE";
        quoteSource.setText(status);
    }

    private void maybeRefreshOnlineData() {
        if (refreshInFlight) return;
        long now = System.currentTimeMillis();
        boolean prayerMissing = apiPrayerMinutes == null || !LocalDate.now().toString().equals(apiPrayerDate);
        boolean verseMissing = verseTranslation.isEmpty() || !LocalDate.now().toString().equals(verseDate);
        if ((prayerMissing || verseMissing || apiPrayerOffline) && now - lastRefreshAttempt >= RETRY_AFTER_MS) {
            refreshOnlineData(false);
        }
    }

    private synchronized void refreshOnlineData(boolean force) {
        if (!enabled || refreshInFlight) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastRefreshAttempt < RETRY_AFTER_MS) return;
        refreshInFlight = true;
        lastRefreshAttempt = now;
        final LocalDate requestedDay = LocalDate.now();
        final double requestedLat = latitude;
        final double requestedLon = longitude;
        Thread worker = new Thread(() -> {
            try {
                int[] todayTimes = fetchPrayerMinutes(requestedDay, requestedLat, requestedLon);
                savePrayerCache(requestedDay, requestedLat, requestedLon, todayTimes);
                if (Math.abs(latitude - requestedLat) < 0.0001 && Math.abs(longitude - requestedLon) < 0.0001) {
                    apiPrayerMinutes = todayTimes;
                    apiPrayerDate = requestedDay.toString();
                    apiPrayerFromCache = false;
                    apiPrayerOffline = false;
                }
            } catch (Exception ignored) {
                apiPrayerOffline = true;
            }
            try {
                int[] tomorrow = fetchPrayerMinutes(requestedDay.plusDays(1), requestedLat, requestedLon);
                savePrayerCache(requestedDay.plusDays(1), requestedLat, requestedLon, tomorrow);
                if (Math.abs(latitude - requestedLat) < 0.0001 && Math.abs(longitude - requestedLon) < 0.0001) {
                    tomorrowMaghrib = tomorrow[3];
                    tomorrowPrayerDate = requestedDay.plusDays(1).toString();
                }
            } catch (Exception ignored) {
                // Without a valid next-day schedule, don't invent tomorrow's countdown.
            }
            try {
                int index = Math.floorMod((int) requestedDay.toEpochDay(), VERSE_REFS.length);
                int sura = VERSE_REFS[index][0];
                int aya = VERSE_REFS[index][1];
                JSONObject response = requestJson("https://api.myquran.com/v3/quran/" + sura + "/" + aya);
                JSONObject data = response.optJSONObject("data");
                // MyQuran's /quran/{surah}/{ayah} detail endpoint returns the verse object
                // directly under data; surah metadata belongs to the /quran/{surah} endpoint.
                if (!response.optBoolean("status", false) || data == null
                        || data.optInt("surah_number", sura) != sura
                        || data.optInt("ayah_number", aya) != aya) {
                    throw new IOException("Unexpected MyQuran response");
                }
                String arab = data.optString("arab", "").trim();
                String translation = data.optString("translation", "").trim();
                if (arab.isEmpty() || translation.isEmpty()) throw new IOException("Incomplete verse response");
                String reference = "QS. " + surahName(sura) + " " + sura + ":" + aya;
                JSONObject cache = new JSONObject();
                cache.put("date", requestedDay.toString());
                cache.put("arab", arab);
                cache.put("translation", translation);
                cache.put("reference", reference);
                prefs.edit().putString(QURAN_CACHE, cache.toString()).apply();
                verseArabic = arab;
                verseTranslation = translation;
                verseReference = reference;
                verseDate = requestedDay.toString();
                verseFromCache = false;
            } catch (Exception ignored) {
                // Keep the last valid verse only, with its original date visibly marked as old.
            }
            refreshInFlight = false;
            if (cardView != null) cardView.post(this::update);
        }, "cleanyourtv-data-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private static String surahName(int number) {
        switch (number) {
            case 2: return "Al-Baqarah";
            case 13: return "Ar-Ra'd";
            case 39: return "Az-Zumar";
            case 57: return "Al-Hadid";
            case 65: return "At-Talaq";
            case 94: return "Al-Insyirah";
            default: return "Surah " + number;
        }
    }

    private int[] fetchPrayerMinutes(LocalDate date, double lat, double lon) throws Exception {
        String day = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT));
        String zone = java.net.URLEncoder.encode(java.time.ZoneId.systemDefault().getId(), "UTF-8");
        String url = "https://api.aladhan.com/v1/timings/" + day
                + "?latitude=" + String.format(Locale.ROOT, "%.6f", lat)
                + "&longitude=" + String.format(Locale.ROOT, "%.6f", lon)
                + "&method=20&school=0&timezonestring=" + zone;
        JSONObject response = requestJson(url);
        JSONObject data = response.optJSONObject("data");
        JSONObject timings = data == null ? null : data.optJSONObject("timings");
        if (response.optInt("code", 0) != 200 || timings == null) {
            throw new IOException("Unexpected Aladhan response");
        }
        String[] keys = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
        int[] result = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            result[i] = parsePrayerMinute(timings.optString(keys[i], ""));
            if (result[i] < 0 || result[i] >= 1440) throw new IOException("Invalid prayer time");
        }
        if (!(result[0] < result[1] && result[1] < result[2]
                && result[2] < result[3] && result[3] < result[4])) {
            throw new IOException("Invalid prayer time sequence");
        }
        return result;
    }

    private static int parsePrayerMinute(String value) {
        if (value == null) return -1;
        String clean = value.trim();
        if (clean.length() < 5) return -1;
        clean = clean.substring(0, 5);
        if (clean.charAt(2) != ':') return -1;
        try {
            int hour = Integer.parseInt(clean.substring(0, 2));
            int minute = Integer.parseInt(clean.substring(3, 5));
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return -1;
            return hour * 60 + minute;
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    private JSONObject requestJson(String address) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setUseCaches(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "CleanYourTV-Forest-Edition/1.0");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code);
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                    if (body.length() > 1024 * 1024) throw new IOException("Response too large");
                }
                return new JSONObject(body.toString());
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void savePrayerCache(LocalDate date, double lat, double lon, int[] times) {
        try {
            JSONObject cache = new JSONObject();
            cache.put("date", date.toString());
            cache.put("latitude", lat);
            cache.put("longitude", lon);
            cache.put("method", 20);
            JSONArray array = new JSONArray();
            for (int value : times) array.put(value);
            cache.put("times", array);
            SharedPreferences.Editor editor = prefs.edit().putString("prayer_cache_" + date, cache.toString());
            // Tomorrow's preload must not overwrite the last-known current/past-day fallback.
            if (!date.isAfter(LocalDate.now())) editor.putString(PRAYER_CACHE_LAST, cache.toString());
            editor.apply();
        } catch (Exception ignored) {
            // A cache failure must not take Home down.
        }
    }

    private void loadCachedData() {
        String today = LocalDate.now().toString();
        JSONObject prayer = cachedPrayerFor(today);
        if (prayer == null) prayer = cachedPrayerLast();
        if (prayer != null) {
            int[] times = readPrayerArray(prayer);
            if (times != null) {
                apiPrayerMinutes = times;
                apiPrayerDate = prayer.optString("date", "");
                apiPrayerFromCache = true;
                apiPrayerOffline = true;
            }
        }
        JSONObject tomorrow = cachedPrayerFor(LocalDate.now().plusDays(1).toString());
        if (tomorrow != null) {
            int[] times = readPrayerArray(tomorrow);
            if (times != null) {
                tomorrowMaghrib = times[3];
                tomorrowPrayerDate = tomorrow.optString("date", "");
            }
        }
        try {
            JSONObject verse = new JSONObject(prefs.getString(QURAN_CACHE, "{}"));
            verseArabic = verse.optString("arab", "").trim();
            verseTranslation = verse.optString("translation", "").trim();
            verseReference = verse.optString("reference", "").trim();
            verseDate = verse.optString("date", "");
            verseFromCache = true;
        } catch (Exception ignored) {
            verseArabic = "";
            verseTranslation = "";
            verseReference = "";
            verseDate = "";
        }
    }

    private JSONObject cachedPrayerFor(String date) {
        try {
            JSONObject cache = new JSONObject(prefs.getString("prayer_cache_" + date, "{}"));
            return matchesCoordinates(cache) ? cache : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private JSONObject cachedPrayerLast() {
        try {
            JSONObject cache = new JSONObject(prefs.getString(PRAYER_CACHE_LAST, "{}"));
            return matchesCoordinates(cache) ? cache : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean matchesCoordinates(JSONObject cache) {
        if (cache == null) return false;
        return Math.abs(cache.optDouble("latitude", 999) - latitude) < 0.0001
                && Math.abs(cache.optDouble("longitude", 999) - longitude) < 0.0001
                && cache.optInt("method", 0) == 20;
    }

    private static int[] readPrayerArray(JSONObject cache) {
        JSONArray array = cache == null ? null : cache.optJSONArray("times");
        if (array == null || array.length() != 5) return null;
        int[] result = new int[5];
        for (int i = 0; i < result.length; i++) {
            int value = array.optInt(i, -1);
            if (value < 0 || value >= 1440) return null;
            result[i] = value;
        }
        return result;
    }

    private void applyLastKnownLocationIfAllowed() {
        boolean manual = prefs.contains("latitude") && !prefs.getBoolean("auto_location", false);
        if (manual || context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        try {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (manager == null) return;
            Location best = null;
            for (String provider : manager.getProviders(true)) {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) best = candidate;
            }
            if (best != null) {
                latitude = best.getLatitude();
                longitude = best.getLongitude();
                location = "LOKASI TV";
                prefs.edit().putFloat("latitude", (float) latitude)
                        .putFloat("longitude", (float) longitude)
                        .putString("location", location).putBoolean("auto_location", true).apply();
            }
        } catch (SecurityException ignored) {
            // Location permission may be withdrawn while Home is running; the manual selector remains available.
        } catch (RuntimeException ignored) {
            // TVs without a working location provider use the saved/configured fallback.
        }
    }

    private boolean isRamadan() {
        if ("on".equalsIgnoreCase(ramadanMode)) return true;
        if ("off".equalsIgnoreCase(ramadanMode)) return false;
        try {
            return HijrahDate.from(LocalDate.now()).get(ChronoField.MONTH_OF_YEAR) == 9;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showSettings() {
        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText locationInput = input(location, InputType.TYPE_CLASS_TEXT);
        EditText latInput = input(Double.toString(latitude), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText lonInput = input(Double.toString(longitude), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText imsakInput = input(Integer.toString(imsakOffset), InputType.TYPE_CLASS_NUMBER);

        // Optional city presets make setup usable with a TV remote; coordinates remain editable below.
        String[] cityNames = {"Pilih kota (opsional)", "Jakarta", "Bandung", "Surabaya", "Yogyakarta", "Semarang", "Surakarta", "Medan", "Makassar", "Denpasar", "Palembang", "Balikpapan", "Banjarmasin", "Pontianak", "Manado", "Mataram", "Aceh"};
        double[][] cityCoordinates = {
                {0, 0}, {-6.2088, 106.8456}, {-6.9175, 107.6191}, {-7.2575, 112.7521},
                {-7.7956, 110.3695}, {-6.9667, 110.4167}, {-7.5755, 110.8243},
                {3.5952, 98.6722}, {-5.1477, 119.4327}, {-8.6705, 115.2126},
                {-2.9761, 104.7754}, {-1.2379, 116.8529}, {-3.3186, 114.5944},
                {-0.0263, 109.3425}, {1.4748, 124.8421}, {-8.5833, 116.1167}, {5.5483, 95.3238}
        };
        Spinner cityInput = new Spinner(context);
        cityInput.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, cityNames));
        addField(form, "Pilih kota Indonesia (opsional)", cityInput);
        cityInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position <= 0) return;
                locationInput.setText(cityNames[position]);
                latInput.setText(Double.toString(cityCoordinates[position][0]));
                lonInput.setText(Double.toString(cityCoordinates[position][1]));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        addField(form, "Nama lokasi", locationInput);
        addField(form, "Latitude", latInput);
        addField(form, "Longitude", lonInput);

        Spinner methodInput = new Spinner(context);
        String[] methods = {"Kemenag Indonesia · Aladhan method 20"};
        methodInput.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, methods));
        methodInput.setSelection(methodIndex(method));
        addField(form, "Metode perhitungan", methodInput);

        Spinner ramadanInput = new Spinner(context);
        String[] ramadanOptions = {"Otomatis ikut kalender Hijriah", "Paksa tampil", "Matikan imsak"};
        ramadanInput.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ramadanOptions));
        ramadanInput.setSelection("on".equalsIgnoreCase(ramadanMode) ? 1 : "off".equalsIgnoreCase(ramadanMode) ? 2 : 0);
        addField(form, "Mode Ramadan", ramadanInput);
        addField(form, "Imsak sebelum Subuh (menit)", imsakInput);

        new AlertDialog.Builder(context)
                .setTitle("Pengaturan Demi Kianz × Nero")
                .setView(form)
                .setPositiveButton("SIMPAN", (dialog, which) -> {
                    try {
                        String newLocation = locationInput.getText().toString().trim();
                        double newLat = Double.parseDouble(latInput.getText().toString().trim());
                        double newLon = Double.parseDouble(lonInput.getText().toString().trim());
                        int newOffset = Integer.parseInt(imsakInput.getText().toString().trim());
                        if (newLat < -90 || newLat > 90 || newLon < -180 || newLon > 180
                                || newOffset < 0 || newOffset > 60) throw new IllegalArgumentException();
                        location = newLocation.isEmpty() ? "LOKASI KUSTOM" : newLocation;
                        latitude = newLat;
                        longitude = newLon;
                        imsakOffset = newOffset;
                        method = "kemenag";
                        ramadanMode = new String[]{"auto", "on", "off"}[ramadanInput.getSelectedItemPosition()];
                        prefs.edit().putString("location", location).putFloat("latitude", (float) latitude)
                                .putFloat("longitude", (float) longitude).putString("method", method)
                                .putString("ramadan_mode", ramadanMode).putInt("imsak_offset", imsakOffset)
                                .putBoolean("auto_location", false).apply();
                        apiPrayerMinutes = null;
                        apiPrayerDate = "";
                        apiPrayerFromCache = true;
                        apiPrayerOffline = true;
                        tomorrowMaghrib = -1;
                        tomorrowPrayerDate = "";
                        update();
                        refreshOnlineData(true);
                    } catch (RuntimeException error) {
                        new AlertDialog.Builder(context)
                                .setMessage("Periksa angka: latitude -90 sampai 90, longitude -180 sampai 180, imsak 0 sampai 60 menit.")
                                .setPositiveButton("OK", null).show();
                    }
                })
                .setNegativeButton("BATAL", null)
                .show();
    }

    private void addField(LinearLayout form, String label, View field) {
        TextView caption = text(label, 12, MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams capLp = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        capLp.topMargin = dp(8);
        form.addView(caption, capLp);
        LinearLayout.LayoutParams fieldLp = new LinearLayout.LayoutParams(-1, dp(44));
        fieldLp.topMargin = dp(3);
        form.addView(field, fieldLp);
    }

    private EditText input(String value, int type) {
        EditText edit = new EditText(context);
        edit.setSingleLine(true);
        edit.setText(value);
        edit.setInputType(type);
        edit.setTextSize(16);
        edit.setTextColor(Color.WHITE);
        edit.setSelectAllOnFocus(true);
        edit.setPadding(dp(8), 0, dp(8), 0);
        edit.setBackground(roundRect(0xFF202824, 0xFF477A64, 6));
        return edit;
    }

    private static int methodIndex(String value) {
        switch (PrayerTimes.Method.fromConfig(value)) {
            case MUSLIM_WORLD_LEAGUE: return 1;
            case ISNA: return 2;
            case EGYPT: return 3;
            case UMM_AL_QURA: return 4;
            default: return 0;
        }
    }

    private static String methodName(int index) {
        switch (index) {
            case 1: return "mwl";
            case 2: return "isna";
            case 3: return "egypt";
            case 4: return "umm_al_qura";
            default: return "kemenag";
        }
    }

    private TextView text(String value, float size, int color, int style) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundRect(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private static JSONObject readThemeConfig(Context context) {
        File external = null;
        try {
            File dir = Config.dir(context);
            external = dir == null ? null : new File(dir, Config.FILE_NAME);
            File file = external != null && external.isFile()
                    ? external : new File(context.getFilesDir(), Config.FILE_NAME);
            if (!file.isFile() || file.length() > 64 * 1024) return new JSONObject();
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] data = new byte[(int) file.length()];
                int count = in.read(data);
                String source = new String(data, 0, Math.max(0, count), StandardCharsets.UTF_8);
                if (!source.isEmpty() && source.charAt(0) == 0xFEFF) source = source.substring(1);
                return new JSONObject(source);
            }
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private static String string(JSONObject json, String key, String fallback) {
        Object value = json.opt(key);
        return value instanceof String ? ((String) value).trim() : fallback;
    }

    private static boolean bool(JSONObject json, String key, boolean fallback) {
        Object value = json.opt(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private static double number(JSONObject json, String key, double fallback) {
        Object value = json.opt(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private static int integer(JSONObject json, String key, int fallback, int min, int max) {
        Object value = json.opt(key);
        if (!(value instanceof Number)) return fallback;
        int result = ((Number) value).intValue();
        return result >= min && result <= max ? result : fallback;
    }
}
