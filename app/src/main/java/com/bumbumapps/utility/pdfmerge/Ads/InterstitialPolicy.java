package com.bumbumapps.utility.pdfmerge.Ads;

/**
 * Pure frequency-cap rules for interstitials (unit tested). Banners are the main ad; a full-screen ad
 * is shown at most once per {@link #MIN_GAP_MS} and never within the first {@link #MIN_GAP_MS} after
 * the app was first opened. Times are wall-clock millis so the caller can persist them across restarts.
 */
public final class InterstitialPolicy {
    public static final long MIN_GAP_MS = 60 * 60_000L;

    private final long firstSeen;
    private long lastShown;

    /** @param lastShownMs 0 when no interstitial has been shown yet. */
    public InterstitialPolicy(long firstSeenMs, long lastShownMs) {
        this.firstSeen = firstSeenMs;
        this.lastShown = lastShownMs;
    }

    public boolean canShow(long nowMs) {
        return nowMs - Math.max(firstSeen, lastShown) >= MIN_GAP_MS;
    }

    public void onShown(long nowMs) {
        lastShown = nowMs;
    }
}
