package com.bumbumapps.utility.pdfmerge.Ads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

import com.bumbumapps.utility.pdfmerge.BuildConfig;
import com.bumbumapps.utility.pdfmerge.R;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.splash.SplashAdDisplayListener;
import com.huawei.hms.ads.splash.SplashView;

/**
 * Huawei AppGallery build: Petal splash (app-open) ad, shown once per cold start of the launcher activity.
 * Skipped on the very first launch, when launched to open a file/link, offline, or when ads are not allowed.
 * Waits at most {@link #LOAD_TIMEOUT_MS} for the ad, then goes straight to the app. Slot id: R.string.hms_splash_id.
 */
public final class PetalSplash {
    private static final String TAG = "PetalSplash";
    private static final String PREFS = "petal_splash";
    private static final String KEY_LAUNCHES = "launches";
    private static final long LOAD_TIMEOUT_MS = 3000L;
    private static final long MAX_SHOW_MS = 12000L;
    private static final long RECENT_MS = 90_000L;

    private static boolean attempted;
    private static long shownAt;

    private PetalSplash() {
    }

    /** True for a short while after a splash ad was displayed, so no interstitial is stacked on it. */
    public static boolean shownRecently() {
        return shownAt != 0L && System.currentTimeMillis() - shownAt < RECENT_MS;
    }

    /** Call from the launcher activity's onCreate, after the Huawei Ads SDK is initialised. */
    public static void maybeShow(Activity activity, boolean adsAllowed) {
        if (attempted) return;
        attempted = true;
        try {
            SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int launches = prefs.getInt(KEY_LAUNCHES, 0);
            prefs.edit().putInt(KEY_LAUNCHES, launches + 1).apply();
            if (launches < 1 || !adsAllowed) return;
            Intent intent = activity.getIntent();
            if (intent != null && intent.getAction() != null && !Intent.ACTION_MAIN.equals(intent.getAction())) return;
            if (activity.isFinishing() || activity.isDestroyed() || !isOnline(activity)) return;
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null) return;
            new Session(activity, content).start();
        } catch (RuntimeException e) {
            Log.w(TAG, "Splash skipped", e);
        }
    }

    private static boolean isOnline(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
            return info == null || info.isConnected();
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static final class Session extends SplashView.SplashAdLoadListener implements Application.ActivityLifecycleCallbacks {
        private final Activity activity;
        private final ViewGroup content;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final SplashView view;
        private final FrameLayout overlay;
        private OnBackPressedCallback back;
        private boolean loaded;
        private boolean done;

        Session(Activity activity, ViewGroup content) {
            this.activity = activity;
            this.content = content;
            this.view = new SplashView(activity);
            this.overlay = new FrameLayout(activity);
            overlay.setClickable(true);
            overlay.setElevation(1000f);
            overlay.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        void start() {
            content.addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            activity.getApplication().registerActivityLifecycleCallbacks(this);
            if (activity instanceof ComponentActivity) {
                back = new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                };
                ((ComponentActivity) activity).getOnBackPressedDispatcher().addCallback(back);
            }
            view.setAdDisplayListener(new SplashAdDisplayListener() {
                @Override
                public void onAdShowed() {
                    shownAt = System.currentTimeMillis();
                }
            });
            handler.postDelayed(() -> {
                if (!loaded) finish();
            }, LOAD_TIMEOUT_MS);
            boolean landscape = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            try {
                view.load(activity.getString(BuildConfig.DEBUG ? R.string.hms_test_splash_id : R.string.hms_splash_id),
                        landscape ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                        new AdParam.Builder().build(), this);
            } catch (RuntimeException e) {
                Log.w(TAG, "Splash load failed", e);
                finish();
            }
        }

        @Override
        public void onAdLoaded() {
            if (done) return;
            loaded = true;
            handler.postDelayed(this::finish, MAX_SHOW_MS);
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            Log.d(TAG, "Splash failed: " + errorCode);
            finish();
        }

        @Override
        public void onAdDismissed() {
            finish();
        }

        private void finish() {
            if (done) return;
            done = true;
            handler.removeCallbacksAndMessages(null);
            if (back != null) back.remove();
            try {
                activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                view.destroyView();
            } catch (RuntimeException e) {
                Log.w(TAG, "Splash cleanup", e);
            }
            content.removeView(overlay);
        }

        @Override
        public void onActivityPaused(Activity a) {
            if (a == activity && !done) view.pauseView();
        }

        @Override
        public void onActivityResumed(Activity a) {
            if (a == activity && !done) view.resumeView();
        }

        @Override
        public void onActivityDestroyed(Activity a) {
            if (a == activity) finish();
        }

        @Override public void onActivityCreated(Activity a, Bundle b) { }
        @Override public void onActivityStarted(Activity a) { }
        @Override public void onActivityStopped(Activity a) { }
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
    }
}
