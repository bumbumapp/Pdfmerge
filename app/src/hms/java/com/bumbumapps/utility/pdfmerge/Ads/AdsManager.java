package com.bumbumapps.utility.pdfmerge.Ads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.bumbumapps.utility.pdfmerge.BuildConfig;
import com.bumbumapps.utility.pdfmerge.R;
import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.InterstitialAd;
import com.huawei.hms.ads.NonPersonalizedAd;
import com.huawei.hms.ads.RequestOptions;
import com.huawei.hms.ads.banner.BannerView;

/**
 * Huawei AppGallery build: same API and interstitial cap as the Play build, but served by Huawei Ads
 * (Petal) instead of AdMob. Only non-personalized ads are requested, so no consent form is needed.
 */
public final class AdsManager {
    private static final String TAG = "AdsManager";
    private static AdsManager instance;

    public static synchronized AdsManager get(Context c) {
        if (instance == null) instance = new AdsManager(c.getApplicationContext());
        return instance;
    }

    private final Context app;
    private static final String PREFS = "interstitial_cap";
    private static final String KEY_LAST = "last_shown_ms";
    private static final String KEY_FIRST_SEEN = "first_seen_ms";
    private final SharedPreferences capPrefs;
    private final InterstitialPolicy policy;
    private boolean sdkReady;
    @Nullable private InterstitialAd interstitial;

    private AdsManager(Context app) {
        this.app = app;
        this.capPrefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long first = capPrefs.getLong(KEY_FIRST_SEEN, 0L);
        if (first == 0L) {
            first = System.currentTimeMillis();
            capPrefs.edit().putLong(KEY_FIRST_SEEN, first).apply();
        }
        this.policy = new InterstitialPolicy(first, capPrefs.getLong(KEY_LAST, 0L));
    }

    /** Call from the launcher activity's onCreate. */
    public void gatherConsent(Activity activity) {
        if (sdkReady) return;
        try {
            HwAds.init(app);
            RequestOptions options = HwAds.getRequestOptions().toBuilder()
                    .setNonPersonalizedAd(NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
                    .build();
            HwAds.setRequestOptions(options);
            sdkReady = true;
            preloadInterstitial();
        } catch (RuntimeException e) {
            Log.w(TAG, "Huawei Ads init failed", e);
        }
    }

    public boolean isPrivacyOptionsRequired() {
        return false;
    }

    public void showPrivacyOptions(Activity activity) {
    }

    private String slot(int real, int test) {
        return app.getString(BuildConfig.DEBUG ? test : real);
    }

    /** Loads a banner into {@code container} (hidden until an ad is filled). */
    public void loadBanner(Activity activity, FrameLayout container) {
        container.setVisibility(View.GONE);
        if (!sdkReady || activity.isFinishing() || activity.isDestroyed()) return;
        BannerView banner = new BannerView(activity);
        banner.setAdId(slot(R.string.hms_banner_id, R.string.hms_test_banner_id));
        banner.setBannerAdSize(bannerSize(activity, container));
        banner.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailed(int code) {
                Log.d(TAG, "Banner failed: " + code);
            }
        });
        container.removeAllViews();
        container.addView(banner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER));
        banner.loadAd(new AdParam.Builder().build());
    }

    private static BannerAdSize bannerSize(Activity activity, View container) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        float widthPx = container.getWidth() > 0 ? container.getWidth() : dm.widthPixels;
        return BannerAdSize.getCurrentDirectionBannerSize(activity, (int) (widthPx / dm.density));
    }

    private void preloadInterstitial() {
        if (!sdkReady || interstitial != null) return;
        InterstitialAd ad = new InterstitialAd(app);
        ad.setAdId(slot(R.string.hms_interstitial_id, R.string.hms_test_interstitial_id));
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdFailed(int code) {
                if (interstitial == ad) interstitial = null;
                Log.d(TAG, "Interstitial failed: " + code);
            }
        });
        interstitial = ad;
        ad.loadAd(new AdParam.Builder().build());
    }

    /**
     * Natural break point (e.g. a finished merge). Shows the preloaded interstitial if the frequency
     * cap allows it, then runs {@code next}; otherwise runs {@code next} immediately.
     */
    public void onBreakPoint(Activity activity, Runnable next) {
        long now = System.currentTimeMillis();
        InterstitialAd ad = interstitial;
        if (ad == null || !ad.isLoaded() || !policy.canShow(now) || activity.isFinishing()) {
            if (ad == null) preloadInterstitial();
            next.run();
            return;
        }
        interstitial = null;
        policy.onShown(now);
        capPrefs.edit().putLong(KEY_LAST, now).apply();
        // Huawei only reports load failures via onAdFailed: if show() never opens the ad, do not wait forever.
        final boolean[] opened = {false};
        final boolean[] done = {false};
        final Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            preloadInterstitial();
            next.run();
        };
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdOpened() {
                opened[0] = true;
            }

            @Override
            public void onAdClosed() {
                finish.run();
            }

            @Override
            public void onAdFailed(int code) {
                finish.run();
            }
        });
        ad.show(activity);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!opened[0]) {
                finish.run();
            }
        }, 3000L);
    }
}
