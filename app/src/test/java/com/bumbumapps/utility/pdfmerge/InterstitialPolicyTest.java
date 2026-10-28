package com.bumbumapps.utility.pdfmerge;

import static org.junit.Assert.*;

import com.bumbumapps.utility.pdfmerge.Ads.InterstitialPolicy;

import org.junit.Test;

public class InterstitialPolicyTest {
    private static final long MIN = 60_000L;
    private static final long T0 = 1_700_000_000_000L;

    @Test public void gapIsTenMinutesAndGraceThree() {
        assertEquals(10 * MIN, InterstitialPolicy.MIN_GAP_MS);
        assertEquals(3 * MIN, InterstitialPolicy.FIRST_GRACE_MS);
    }

    @Test public void noAdRightAfterFirstOpen() {
        InterstitialPolicy p = new InterstitialPolicy(T0, 0);
        assertFalse(p.canShow(T0 + 2 * MIN));
        assertTrue(p.canShow(T0 + 3 * MIN));
    }

    @Test public void atMostOneAdPerTenMinutes() {
        InterstitialPolicy p = new InterstitialPolicy(T0, 0);
        long shown = T0 + 30 * MIN;
        p.onShown(shown);
        assertFalse(p.canShow(shown + 5 * MIN));
        assertFalse(p.canShow(shown + 10 * MIN - 1));
        assertTrue(p.canShow(shown + 10 * MIN));
    }

    @Test public void lastShowSurvivesRestart() {
        long shown = T0 + 30 * MIN;
        InterstitialPolicy restarted = new InterstitialPolicy(T0, shown);
        assertFalse(restarted.canShow(shown + 9 * MIN));
    }
}
