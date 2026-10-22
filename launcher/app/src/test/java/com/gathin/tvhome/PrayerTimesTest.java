package com.gathin.tvhome;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrayerTimesTest {
    @Test public void jakartaTimesAreFiniteAndInDay() {
        PrayerTimes.Day day = PrayerTimes.calculate(LocalDate.of(2026, 10, 22),
                -6.2088, 106.8456, PrayerTimes.Method.KEMENAG);
        for (int minute : new int[]{day.fajr, day.sunrise, day.dhuhr, day.asr, day.maghrib, day.isha}) {
            assertTrue("minute must be within a day: " + minute, minute >= 0 && minute < 1440);
        }
        assertTrue(day.fajr < day.sunrise);
        assertTrue(day.sunrise < day.dhuhr);
        assertTrue(day.dhuhr < day.asr);
        assertTrue(day.asr < day.maghrib);
        assertTrue(day.maghrib < day.isha);
    }

    @Test public void formatsTwentyFourHourClockAndWraps() {
        assertEquals("00:05", PrayerTimes.format(5));
        assertEquals("23:59", PrayerTimes.format(-1));
        assertEquals("00:00", PrayerTimes.format(1440));
    }

    @Test public void imsakOffsetWrapsAroundMidnight() {
        assertEquals(285, PrayerTimes.imsak(295, 10));
        assertEquals(1435, PrayerTimes.imsak(5, 10));
    }

    @Test public void methodsAreSelectedFromThemeConfig() {
        assertEquals(PrayerTimes.Method.KEMENAG, PrayerTimes.Method.fromConfig("kemenag"));
        assertEquals(PrayerTimes.Method.MUSLIM_WORLD_LEAGUE, PrayerTimes.Method.fromConfig("mwl"));
        assertEquals(PrayerTimes.Method.ISNA, PrayerTimes.Method.fromConfig("isna"));
        assertEquals(PrayerTimes.Method.EGYPT, PrayerTimes.Method.fromConfig("egypt"));
        assertEquals(PrayerTimes.Method.UMM_AL_QURA, PrayerTimes.Method.fromConfig("umm_al_qura"));
    }
}
