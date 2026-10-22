package com.bumbumapps.utility.pdfmerge.Ads;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumbumapps.utility.pdfmerge.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * App-wide ads: UMP consent first, then a single MobileAds init, one preloaded interstitial that is
 * reused across screens and shown only at natural break points behind a frequency cap, and anchored
 * adaptive banners.
 */
public final class AdsManager {
    private static final String TAG = "AdsManager";
    private static AdsManager instance;

    public static synchronized AdsManager get(Context c) {
        if (instance == null) instance = new AdsManager(c.getApplicationContext());
        return instance;
    }

    private final Context app;
    private final ConsentInformation consent;
    private final AtomicBoolean initStarted = new AtomicBoolean(false);
    private final InterstitialPolicy policy = new InterstitialPolicy(SystemClock.elapsedRealtime());
    private final List<Runnable> whenReady = new ArrayList<>();
    private boolean sdkReady;
    @Nullable private InterstitialAd interstitial;
    private boolean loading;

    private AdsManager(Context app) {
        this.app = app;
        this.consent = UserMessagingPlatform.getConsentInformation(app);
    }

    /** Call from the launcher activity's onCreate. */
    public void gatherConsent(Activity activity) {
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(activity, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (formError != null) Log.w(TAG, "Consent form: " + formError.getMessage());
                    activity.invalidateOptionsMenu();
                    initIfAllowed();
                }),
                error -> {
                    Log.w(TAG, "Consent info: " + error.getMessage());
                    initIfAllowed();
                });
        // Consent from a previous session is enough to start right away.
        initIfAllowed();
    }

    public boolean isPrivacyOptionsRequired() {
        return consent.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void showPrivacyOptions(Activity activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, e -> {
            if (e != null) Log.w(TAG, "Privacy options: " + e.getMessage());
            initIfAllowed();
        });
    }

    private void initIfAllowed() {
        if (!consent.canRequestAds() || !initStarted.compareAndSet(false, true)) return;
        new Thread(() -> MobileAds.initialize(app, status -> {
            sdkReady = true;
            List<Runnable> pending = new ArrayList<>(whenReady);
            whenReady.clear();
            for (Runnable r : pending) r.run();
            preloadInterstitial();
        }), "ads-init").start();
    }

    private void runWhenReady(Runnable r) {
        if (sdkReady) r.run();
        else whenReady.add(r);
    }

    /** Loads an anchored adaptive banner into {@code container} (hidden until an ad is filled). */
    public void loadBanner(Activity activity, FrameLayout container) {
        container.setVisibility(View.GONE);
        runWhenReady(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            AdView adView = new AdView(activity);
            adView.setAdUnitId(activity.getString(R.string.banner_id));
            adView.setAdSize(adaptiveSize(activity, container));
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    container.setVisibility(View.VISIBLE);
                }
            });
            container.removeAllViews();
            container.addView(adView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER));
            adView.loadAd(new AdRequest.Builder().build());
        });
    }

    private static AdSize adaptiveSize(Activity activity, View container) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        float widthPx = container.getWidth() > 0 ? container.getWidth() : dm.widthPixels;
        int widthDp = (int) (widthPx / dm.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp);
    }

    private void preloadInterstitial() {
        if (interstitial != null || loading || !consent.canRequestAds()) return;
        loading = true;
        InterstitialAd.load(app, app.getString(R.string.interstitial_id), new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        loading = false;
                        interstitial = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError e) {
                        loading = false;
                        interstitial = null;
                        Log.d(TAG, "Interstitial failed: " + e.getMessage());
                    }
                });
    }

    /**
     * Natural break point (e.g. a finished merge). Shows the preloaded interstitial if the frequency
     * cap allows it, then runs {@code next}; otherwise runs {@code next} immediately.
     */
    public void onBreakPoint(Activity activity, Runnable next) {
        long now = SystemClock.elapsedRealtime();
        InterstitialAd ad = interstitial;
        if (ad == null || !policy.canShow(now) || activity.isFinishing()) {
            if (ad == null) runWhenReady(this::preloadInterstitial);
            next.run();
            return;
        }
        interstitial = null;
        policy.onShown(now);
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                preloadInterstitial();
                next.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError e) {
                preloadInterstitial();
                next.run();
            }
        });
        ad.show(activity);
    }
}
