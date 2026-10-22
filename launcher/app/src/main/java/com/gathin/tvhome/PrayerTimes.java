package com.gathin.tvhome;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small offline prayer-time calculator for the Demi Kianz × Nero home screen.
 * Uses solar-position approximations; times are estimates and must be checked against the
 * user's local mosque/Kemenag timetable. No network service or background worker is used.
 */
final class PrayerTimes {
    enum Method {
        KEMENAG(20.0, 18.0, -1),
        MUSLIM_WORLD_LEAGUE(18.0, 17.0, -1),
        ISNA(15.0, 15.0, -1),
        EGYPT(19.5, 17.5, -1),
        UMM_AL_QURA(18.5, 0, 90);

        final double fajrAngle;
        final double ishaAngle;
        final int ishaMinutesAfterSunset;

        Method(double fajrAngle, double ishaAngle, int ishaMinutesAfterSunset) {
            this.fajrAngle = fajrAngle;
            this.ishaAngle = ishaAngle;
            this.ishaMinutesAfterSunset = ishaMinutesAfterSunset;
        }

        static Method fromConfig(String value) {
            if (value == null) return KEMENAG;
            switch (value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_')) {
                case "mwl":
                case "muslim_world_league": return MUSLIM_WORLD_LEAGUE;
                case "isna": return ISNA;
                case "egypt":
                case "egyptian": return EGYPT;
                case "umm_al_qura":
                case "ummalqura": return UMM_AL_QURA;
                default: return KEMENAG;
            }
        }
    }

    static final class Day {
        final int fajr;
        final int sunrise;
        final int dhuhr;
        final int asr;
        final int maghrib;
        final int isha;

        Day(int fajr, int sunrise, int dhuhr, int asr, int maghrib, int isha) {
            this.fajr = fajr;
            this.sunrise = sunrise;
            this.dhuhr = dhuhr;
            this.asr = asr;
            this.maghrib = maghrib;
            this.isha = isha;
        }

        int[] prayers() {
            return new int[]{fajr, dhuhr, asr, maghrib, isha};
        }

        String[] names() {
            return new String[]{"SUBUH", "DZUHUR", "ASHAR", "MAGHRIB", "ISYA"};
        }
    }

    private PrayerTimes() {}

    static Day calculate(LocalDate date, double latitude, double longitude, Method method) {
        if (date == null) date = LocalDate.now();
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) latitude = -6.9;
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) longitude = 107.1;
        if (method == null) method = Method.KEMENAG;

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime localNoon = date.atTime(12, 0).atZone(zone);
        double timezone = localNoon.getOffset().getTotalSeconds() / 3600.0;
        double jd = date.toEpochDay() + 2440587.5 + 0.5;
        double d = jd - 2451545.0;
        double g = radians(normalize(357.529 + 0.98560028 * d));
        double q = normalize(280.459 + 0.98564736 * d);
        double l = radians(normalize(q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g)));
        double e = radians(23.439 - 0.00000036 * d);
        double ra = degrees(Math.atan2(Math.cos(e) * Math.sin(l), Math.cos(l)));
        ra = normalize(ra) / 15.0;
        double eq = normalizeHours(q / 15.0 - ra);
        double decl = Math.asin(Math.sin(e) * Math.sin(l));
        double lat = radians(latitude);

        double noon = 12.0 + timezone - longitude / 15.0 - eq;
        int fajr = minute(noon - hourAngle(-method.fajrAngle, lat, decl) / 15.0);
        int sunrise = minute(noon - hourAngle(-0.833, lat, decl) / 15.0);
        int dhuhr = minute(noon + 1.0 / 60.0);
        double asrAltitude = degrees(Math.atan(1.0 / (1.0 + Math.tan(Math.abs(lat - decl)))));
        int asr = minute(noon + hourAngle(asrAltitude, lat, decl) / 15.0);
        int maghrib = minute(noon + hourAngle(-0.833, lat, decl) / 15.0);
        int isha = method.ishaMinutesAfterSunset >= 0
                ? minute(maghrib / 60.0 + (maghrib % 60) / 60.0 + method.ishaMinutesAfterSunset / 60.0)
                : minute(noon + hourAngle(-method.ishaAngle, lat, decl) / 15.0);

        // Very high latitudes can make an angle unreachable. Keep a usable bounded result instead
        // of emitting NaN or crashing; the location can be adjusted in the theme config.
        fajr = validOr(fajr, minute(noon - 1.5));
        sunrise = validOr(sunrise, minute(noon - 1.0));
        asr = validOr(asr, minute(noon + 3.0));
        maghrib = validOr(maghrib, minute(noon + 1.0));
        isha = validOr(isha, minute(noon + 1.5));
        return new Day(fajr, sunrise, dhuhr, asr, maghrib, isha);
    }

    static String format(int minute) {
        int m = Math.floorMod(minute, 1440);
        return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60);
    }

    static int imsak(int fajr, int offsetMinutes) {
        return Math.floorMod(fajr - Math.max(0, Math.min(60, offsetMinutes)), 1440);
    }

    private static int validOr(int value, int fallback) {
        return value >= 0 && value < 1440 ? value : fallback;
    }

    private static int minute(double hour) {
        if (!Double.isFinite(hour)) return -1;
        return (int) Math.round(hour * 60.0);
    }

    private static double hourAngle(double altitude, double latitude, double declination) {
        double numerator = Math.sin(radians(altitude)) - Math.sin(latitude) * Math.sin(declination);
        double denominator = Math.cos(latitude) * Math.cos(declination);
        double c = numerator / denominator;
        if (!Double.isFinite(c) || c < -1.0 || c > 1.0) return Double.NaN;
        return degrees(Math.acos(c));
    }

    private static double radians(double value) { return Math.toRadians(value); }
    private static double degrees(double value) { return Math.toDegrees(value); }
    private static double normalize(double value) { return (value % 360.0 + 360.0) % 360.0; }
    private static double normalizeHours(double value) {
        double h = (value % 24.0 + 24.0) % 24.0;
        return h > 12.0 ? h - 24.0 : h;
    }
}
