package com.bumbumapps.utility.pdfmerge;

import static org.junit.Assert.*;

import com.bumbumapps.utility.pdfmerge.Ads.InterstitialPolicy;

import org.junit.Test;

public class InterstitialPolicyTest {
    private static final long HOUR = 60 * 60_000L;
    private static final long T0 = 1_700_000_000_000L;

    @Test public void gapIsOneHour() {
        assertEquals(HOUR, InterstitialPolicy.MIN_GAP_MS);
    }

    @Test public void noAdInTheFirstHourAfterInstall() {
        InterstitialPolicy p = new InterstitialPolicy(T0, 0);
        assertFalse(p.canShow(T0 + 59 * 60_000L));
        assertTrue(p.canShow(T0 + HOUR));
    }

    @Test public void atMostOneAdPerHour() {
        InterstitialPolicy p = new InterstitialPolicy(T0, 0);
        long shown = T0 + 3 * HOUR;
        p.onShown(shown);
        assertFalse(p.canShow(shown + 5 * 60_000L));
        assertFalse(p.canShow(shown + HOUR - 1));
        assertTrue(p.canShow(shown + HOUR));
    }

    @Test public void lastShowSurvivesRestart() {
        long shown = T0 + 3 * HOUR;
        InterstitialPolicy restarted = new InterstitialPolicy(T0, shown);
        assertFalse(restarted.canShow(shown + 30 * 60_000L));
    }
}
