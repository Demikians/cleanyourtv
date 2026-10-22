package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.Locale;

/** Low-cost native brand/prayer card. No WebView, network, artwork or animation loop. */
final class DemiPanel {
    private static final int GREEN = Color.rgb(75, 220, 165);
    private static final int MUTED = Color.rgb(164, 184, 176);
    private static final String PREFS = "demi_prayer_settings";
    private static final String[][] VERSES = {
        {"Sesungguhnya bersama kesulitan ada kemudahan.", "QS. Al-Insyirah 94:5–6"},
        {"Allah tidak membebani seseorang melainkan sesuai kesanggupannya.", "QS. Al-Baqarah 2:286"},
        {"Ingatlah, hanya dengan mengingat Allah hati menjadi tenteram.", "QS. Ar-Ra'd 13:28"},
        {"Sesungguhnya Allah beserta orang-orang yang sabar.", "QS. Al-Baqarah 2:153"},
        {"Janganlah kamu berputus asa dari rahmat Allah.", "QS. Az-Zumar 39:53"},
        {"Barang siapa bertawakal kepada Allah, niscaya Allah akan mencukupkan keperluannya.", "QS. At-Talaq 65:3"},
        {"Maka ingatlah kepada-Ku, Aku pun akan ingat kepadamu.", "QS. Al-Baqarah 2:152"},
        {"Dan Dia bersama kamu di mana saja kamu berada.", "QS. Al-Hadid 57:4"}
    };

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
    private final TextView quoteView;
    private final TextView quoteSource;
    private final TextView imsakLabel;
    private double latitude;
    private double longitude;
    private String method;
    private String location;
    private String ramadanMode;
    private int imsakOffset;

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
        method = prefs.getString("method", defaultMethod);
        location = prefs.getString("location", defaultLocation);
        ramadanMode = prefs.getString("ramadan_mode", defaultRamadan);
        imsakOffset = prefs.getInt("imsak_offset", defaultImsakOffset);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(15), dp(18), dp(14));
        card.setBackground(roundRect(0xE90A100E, 0x5536D99B, 18));
        card.setFocusable(false);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView accent = text("▎", 17, GREEN, Typeface.BOLD);
        accent.setPadding(0, 0, dp(7), 0);
        header.addView(accent, wrap());
        TextView title = text("PRAYER TIMES", 12, Color.WHITE, Typeface.BOLD);
        title.setLetterSpacing(0.16f);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        locationView = text(location, 10, MUTED, Typeface.NORMAL);
        locationView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        locationView.setMaxWidth(dp(220));
        header.addView(locationView, wrap());
        TextView setup = text("  ⚙", 14, GREEN, Typeface.BOLD);
        setup.setFocusable(true);
        setup.setClickable(true);
        setup.setPadding(dp(8), dp(4), 0, dp(4));
        setup.setOnClickListener(v -> showSettings());
        header.addView(setup, wrap());
        card.addView(header, wrap());

        LinearLayout cells = new LinearLayout(context);
        cells.setOrientation(LinearLayout.HORIZONTAL);
        cells.setPadding(0, dp(12), 0, dp(8));
        String[] names = {"SUBUH", "DZUHUR", "ASHAR", "MAGHRIB", "ISYA"};
        for (int i = 0; i < names.length; i++) {
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(3), dp(9), dp(3), dp(9));
            cell.setBackground(roundRect(0x251B2822, 0, 11));
            TextView name = text(names[i], 9, MUTED, Typeface.BOLD);
            name.setGravity(Gravity.CENTER);
            TextView time = text("--:--", 17, Color.WHITE, Typeface.NORMAL);
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
        nextRow.setPadding(0, dp(7), 0, 0);
        TextView nextLabel = text("SHOLAT BERIKUTNYA", 9, GREEN, Typeface.BOLD);
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
        imsakRow.setPadding(0, dp(6), 0, 0);
        imsakLabel = text("IMSAK", 9, GREEN, Typeface.BOLD);
        imsakLabel.setLetterSpacing(0.12f);
        imsakRow.addView(imsakLabel, wrap());
        imsakView = text("", 11, Color.WHITE, Typeface.BOLD);
        imsakView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams imsakLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        imsakLp.leftMargin = dp(8);
        imsakRow.addView(imsakView, imsakLp);
        card.addView(imsakRow, wrap());

        View divider = new View(context);
        divider.setBackgroundColor(0x3336D99B);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(1));
        dividerLp.topMargin = dp(11);
        dividerLp.bottomMargin = dp(9);
        card.addView(divider, dividerLp);

        quoteView = text("", 14, Color.rgb(238, 245, 241), Typeface.NORMAL);
        quoteView.setMaxLines(2);
        quoteView.setLineSpacing(dp(2), 1.0f);
        card.addView(quoteView, wrap());
        quoteSource = text("", 10, GREEN, Typeface.BOLD);
        quoteSource.setPadding(0, dp(5), 0, 0);
        card.addView(quoteSource, wrap());

        if (enabled) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(610), ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.LEFT);
            lp.leftMargin = dp(56);
            lp.topMargin = dp(158);
            root.addView(card, lp);
        } else {
            card.setVisibility(View.GONE);
        }
        update();
    }

    String brandTitle() { return brandTitle; }
    String brandTagline() { return brandTagline; }
    boolean hasBrand() { return !brandTitle.trim().isEmpty(); }

    void update() {
        if (!enabled) return;
        locationView.setText(location);
        LocalDate today = LocalDate.now();
        PrayerTimes.Day day = PrayerTimes.calculate(today, latitude, longitude, PrayerTimes.Method.fromConfig(method));
        int[] times = day.prayers();
        String[] names = day.names();
        java.time.LocalTime timeNow = java.time.LocalTime.now();
        int now = timeNow.getHour() * 60 + timeNow.getMinute();
        int next = -1;
        for (int i = 0; i < times.length; i++) {
            prayerTimes[i].setText(PrayerTimes.format(times[i]));
            if (next < 0 && now < times[i]) next = i;
        }
        for (int i = 0; i < prayerCells.length; i++) {
            boolean selected = i == next;
            prayerCells[i].setBackground(roundRect(selected ? 0x6633D69A : 0x251B2822,
                    selected ? 0xAA36D99B : 0, 11));
            prayerTimes[i].setTextColor(selected ? GREEN : Color.WHITE);
        }
        int target;
        String nextName;
        long remaining;
        if (next >= 0) {
            target = times[next];
            nextName = names[next];
            remaining = target - now;
        } else {
            target = PrayerTimes.calculate(today.plusDays(1), latitude, longitude,
                    PrayerTimes.Method.fromConfig(method)).fajr;
            nextName = "SUBUH";
            remaining = (1440L - now) + target;
        }
        countdownView.setText(nextName + "  ·  " + (remaining / 60) + "J "
                + String.format(Locale.ROOT, "%02d", remaining % 60) + "M");

        boolean ramadan = isRamadan();
        View imsakRow = (View) imsakView.getParent();
        imsakRow.setVisibility(ramadan ? View.VISIBLE : View.GONE);
        if (ramadan) imsakView.setText(PrayerTimes.format(PrayerTimes.imsak(day.fajr, imsakOffset))
                + "  ·  " + imsakOffset + " menit sebelum Subuh");
        int quoteIndex = Math.floorMod((int) today.toEpochDay(), VERSES.length);
        quoteView.setText("“" + VERSES[quoteIndex][0] + "”");
        quoteSource.setText(VERSES[quoteIndex][1]);
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
        addField(form, "Nama lokasi", locationInput);
        addField(form, "Latitude", latInput);
        addField(form, "Longitude", lonInput);

        Spinner methodInput = new Spinner(context);
        String[] methods = {"Kemenag Indonesia", "Muslim World League", "ISNA", "Egypt", "Umm al-Qura"};
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
                        method = methodName(methodInput.getSelectedItemPosition());
                        ramadanMode = new String[]{"auto", "on", "off"}[ramadanInput.getSelectedItemPosition()];
                        prefs.edit().putString("location", location).putFloat("latitude", (float) latitude)
                                .putFloat("longitude", (float) longitude).putString("method", method)
                                .putString("ramadan_mode", ramadanMode).putInt("imsak_offset", imsakOffset).apply();
                        update();
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
