package com.ruoyi.asset.pda.feature.inbound;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.BuildConfig;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
public class InboundActivityInstrumentedTest {
    private AssetPdaApplication application;

    @Before
    public void createValidLocalSession() {
        application = ApplicationProvider.getApplicationContext();
        AppContainer container = application.getAppContainer();
        container.getSessionCookieJar().clear();
        container.getSessionManager().markPendingValidation();
        HttpUrl url = HttpUrl.get(BuildConfig.BASE_URL + "asset/pda/profile");
        Cookie cookie = new Cookie.Builder().name("JSESSIONID")
                .value("inbound-instrumented-session").hostOnlyDomain(url.host())
                .path("/").httpOnly().build();
        container.getSessionCookieJar().saveFromResponse(
                url, Collections.singletonList(cookie));
        container.getSessionManager().markValid();
    }

    @After
    public void clearSession() {
        application.getAppContainer().getSessionCookieJar().clear();
        application.getAppContainer().getSessionManager().markPendingValidation();
    }

    @Test
    public void inboundPageExposesPrimaryIndustrialControls() {
        Intent intent = new Intent(application, InboundActivity.class)
                .putExtra(InboundActivity.EXTRA_SKIP_INITIAL_LOAD, true)
                .putExtra(InboundActivity.EXTRA_CAN_CONFIRM, true);
        try (ActivityScenario<InboundActivity> ignored =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.inboundToolbar)).check(matches(isDisplayed()));
            onView(withId(R.id.inboundScanButton)).check(matches(isDisplayed()));
            onView(withId(R.id.inboundAssetList)).check(matches(isDisplayed()));
            onView(withId(R.id.inboundConfirmButton)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void inboundActivityIsNotExported() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ActivityInfo info = context.getPackageManager().getActivityInfo(
                new ComponentName(context, InboundActivity.class), 0);
        assertFalse(info.exported);
    }
}
