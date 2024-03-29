package com.bumbumapps.utility.pdfmerge.AppDefault;

import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import com.bumbumapps.utility.pdfmerge.R;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public abstract class AppDefaultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentViewLayoutRes());
        setUpInitialFragment(savedInstanceState);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

    }

    private void setUpInitialFragment(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, createInitialFragment())
                    .commit();
        }
    }

    @LayoutRes
    protected abstract int contentViewLayoutRes();

    @NonNull
    protected abstract Fragment createInitialFragment();
}