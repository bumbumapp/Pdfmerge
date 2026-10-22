package com.bumbumapps.utility.pdfmerge;

import static org.junit.Assert.*;

import com.bumbumapps.utility.pdfmerge.Ads.InterstitialPolicy;

import org.junit.Test;

public class InterstitialPolicyTest {
    @Test public void respectsGraceAndGap() {
        InterstitialPolicy p = new InterstitialPolicy(0);
        assertFalse("not right after launch", p.canShow(10_000));
        assertTrue(p.canShow(InterstitialPolicy.SESSION_GRACE_MS));
        p.onShown(50_000);
        assertFalse(p.canShow(50_000 + InterstitialPolicy.MIN_GAP_MS - 1));
        assertTrue(p.canShow(50_000 + InterstitialPolicy.MIN_GAP_MS));
        assertTrue("at least 60 s between interstitials", InterstitialPolicy.MIN_GAP_MS >= 60_000);
    }
}
