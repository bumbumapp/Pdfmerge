package com.bumbumapps.utility.pdfmerge.Ads;

/**
 * Pure frequency-cap rules for interstitials (unit tested):
 * never within {@link #MIN_GAP_MS} of the previous show and never before the user has been in the
 * app for {@link #SESSION_GRACE_MS} (so the very first action of a session is not interrupted).
 */
public final class InterstitialPolicy {
    public static final long MIN_GAP_MS = 60_000L;
    public static final long SESSION_GRACE_MS = 45_000L;

    private final long sessionStart;
    private long lastShown = Long.MIN_VALUE;

    public InterstitialPolicy(long sessionStartMs) {
        this.sessionStart = sessionStartMs;
    }

    public boolean canShow(long nowMs) {
        if (nowMs - sessionStart < SESSION_GRACE_MS) return false;
        return lastShown == Long.MIN_VALUE || nowMs - lastShown >= MIN_GAP_MS;
    }

    public void onShown(long nowMs) {
        lastShown = nowMs;
    }
}
