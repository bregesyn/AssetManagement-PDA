package com.ruoyi.asset.pda.feature.rfid;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.BuildConfig;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.feature.identify.AssetIdentifyActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class Stage3ActivitiesInstrumentedTest {
    private AssetPdaApplication application;

    @Before
    public void createValidLocalSession() {
        application = ApplicationProvider.getApplicationContext();
        AppContainer container = application.getAppContainer();
        container.getSessionManager().invalidate();
        HttpUrl url = HttpUrl.get(BuildConfig.BASE_URL + "asset/pda/profile");
        Cookie cookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value("stage3-instrumented-session")
                .hostOnlyDomain(url.host())
                .path("/")
                .httpOnly()
                .build();
        container.getSessionCookieJar().saveFromResponse(
                url, Collections.singletonList(cookie));
        container.getSessionManager().markValid();
    }

    @After
    public void clearSession() {
        application.getAppContainer().getSessionManager().invalidate();
    }

    @Test
    public void identifyAndBatchExposeScrollablePrimaryControls() {
        try (ActivityScenario<AssetIdentifyActivity> ignored =
                     ActivityScenario.launch(AssetIdentifyActivity.class)) {
            onView(withId(R.id.identifyScanButton)).check(matches(isDisplayed()));
            onView(withText(R.string.identify_subtitle)).check(matches(isDisplayed()));
        }
        createValidLocalSession();
        try (ActivityScenario<RfidTagBatchActivity> ignored =
                     ActivityScenario.launch(RfidTagBatchActivity.class)) {
            onView(withId(R.id.batchScanButton)).check(matches(isDisplayed()));
            onView(withId(R.id.batchSubmitButton)).check(matches(not(isEnabled())));
        }
    }

    @Test
    public void bindRequiresAssetBeforeScanAndUnbindStartsFromSingleScan() {
        try (ActivityScenario<RfidBindActivity> ignored =
                     ActivityScenario.launch(RfidBindActivity.class)) {
            onView(withId(R.id.operationAssetCodeEditText)).check(matches(isDisplayed()));
            onView(withId(R.id.operationScanButton)).check(matches(not(isEnabled())));
            onView(withId(R.id.operationSubmitButton)).check(matches(not(isEnabled())));
        }
        createValidLocalSession();
        try (ActivityScenario<RfidUnbindActivity> ignored =
                     ActivityScenario.launch(RfidUnbindActivity.class)) {
            onView(withId(R.id.operationScanButton)).check(matches(isEnabled()));
            onView(withId(R.id.operationSubmitButton)).check(matches(not(isEnabled())));
            onView(withText(R.string.unbind_subtitle)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void allStage3ActivitiesAreNotExported() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertNotExported(context, AssetIdentifyActivity.class);
        assertNotExported(context, RfidTagBatchActivity.class);
        assertNotExported(context, RfidBindActivity.class);
        assertNotExported(context, RfidUnbindActivity.class);
    }

    private void assertNotExported(Context context, Class<?> activityClass) throws Exception {
        ActivityInfo info = context.getPackageManager().getActivityInfo(
                new ComponentName(context, activityClass), 0);
        assertFalse(info.exported);
    }
}
