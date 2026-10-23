package com.bumbumapps.utility.pdfmerge.Review;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

/**
 * Asks for a Play Store rating with the In-App Review API after the user has had real success
 * with the app: from the 3rd successful merge/split on, never in the session of the first app
 * launch, and at most once per {@link #COOLDOWN_MS} locally (Google applies its own quota on top
 * and may decide not to show the sheet at all). Never bound to a button.
 */
public final class ReviewPrompt {
    static final int MIN_SUCCESSES = 3;
    static final int MIN_LAUNCHES = 2;
    static final long COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000;

    private static final String TAG = "ReviewPrompt";
    private static final String PREFS = "review_prompt";
    private static final String KEY_LAUNCHES = "launches";
    private static final String KEY_SUCCESSES = "successes";
    private static final String KEY_LAST_REQUEST = "last_request";

    private ReviewPrompt() {
    }

    /** Pure decision logic (unit tested). */
    static boolean shouldRequest(int successes, int launches, long lastRequestMs, long nowMs) {
        if (successes < MIN_SUCCESSES) return false;
        if (launches < MIN_LAUNCHES) return false; // first launch session
        return lastRequestMs <= 0 || nowMs - lastRequestMs >= COOLDOWN_MS;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Call once per process start (Application.onCreate). */
    public static void onAppLaunch(Context c) {
        SharedPreferences p = prefs(c);
        p.edit().putInt(KEY_LAUNCHES, p.getInt(KEY_LAUNCHES, 0) + 1).apply();
    }

    /**
     * Records a successful merge/split. Returns true if the review flow should be started now
     * (the request time is stored right away so it is never asked twice).
     */
    public static boolean recordSuccess(Context c) {
        SharedPreferences p = prefs(c);
        int successes = p.getInt(KEY_SUCCESSES, 0) + 1;
        long now = System.currentTimeMillis();
        boolean ask = shouldRequest(successes, p.getInt(KEY_LAUNCHES, 0), p.getLong(KEY_LAST_REQUEST, 0L), now);
        SharedPreferences.Editor e = p.edit().putInt(KEY_SUCCESSES, successes);
        if (ask) e.putLong(KEY_LAST_REQUEST, now);
        e.apply();
        Log.i(TAG, "success #" + successes + ", request review: " + ask);
        return ask;
    }

    /** Starts Play's in-app review flow; Google decides whether the sheet is actually shown. */
    public static void launch(Activity activity) {
        try {
            ReviewManager manager = ReviewManagerFactory.create(activity);
            manager.requestReviewFlow().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "requestReviewFlow failed", task.getException());
                    return;
                }
                if (activity.isFinishing() || activity.isDestroyed()) return;
                ReviewInfo info = task.getResult();
                manager.launchReviewFlow(activity, info);
            });
        } catch (RuntimeException e) {
            // Play Store missing/outdated: the review is optional, never break the result screen.
            Log.w(TAG, "In-app review unavailable", e);
        }
    }
}
