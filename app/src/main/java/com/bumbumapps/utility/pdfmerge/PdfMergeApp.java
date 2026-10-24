package com.bumbumapps.utility.pdfmerge;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PdfMergeApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Fixed navy brand palette on every Android version (no wallpaper-based dynamic colors).
        com.bumbumapps.utility.pdfmerge.Review.ReviewPrompt.onAppLaunch(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // Same edge-to-edge look (and correct status bar icon colors) on every API level.
                if (activity instanceof ComponentActivity) EdgeToEdge.enable((ComponentActivity) activity);
            }

            @Override public void onActivityStarted(@NonNull Activity activity) { }
            @Override public void onActivityResumed(@NonNull Activity activity) { }
            @Override public void onActivityPaused(@NonNull Activity activity) { }
            @Override public void onActivityStopped(@NonNull Activity activity) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
            @Override public void onActivityDestroyed(@NonNull Activity activity) { }
        });
    }
}
