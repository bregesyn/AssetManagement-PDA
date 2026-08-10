package com.ruoyi.asset.pda.feature.repair;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.Intent;

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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertFalse;

/** 只验证 C6200 首屏关键控件和 Activity 暴露边界，不依赖开发库在线。 */
@RunWith(AndroidJUnit4.class)
public class RepairActivitiesInstrumentedTest {
    private AssetPdaApplication application;

    @Before
    public void createValidLocalSession() {
        application = ApplicationProvider.getApplicationContext();
        AppContainer container = application.getAppContainer();
        container.getSessionCookieJar().clear();
        container.getSessionManager().markPendingValidation();
        HttpUrl url = HttpUrl.get(BuildConfig.BASE_URL + "asset/pda/profile");
        Cookie cookie = new Cookie.Builder().name("JSESSIONID")
                .value("repair-instrumented-session").hostOnlyDomain(url.host())
                .path("/").httpOnly().build();
        container.getSessionCookieJar().saveFromResponse(url, Collections.singletonList(cookie));
        container.getSessionManager().markValid();
    }

    @After
    public void clearSession() {
        application.getAppContainer().getSessionCookieJar().clear();
        application.getAppContainer().getSessionManager().markPendingValidation();
    }

    @Test
    public void workbenchAndSubmitExposePrototypePrimaryControls() {
        Intent workbench = new Intent(application, RepairWorkbenchActivity.class)
                .putExtra(RepairWorkbenchActivity.EXTRA_CAN_SUBMIT, true);
        try (ActivityScenario<RepairWorkbenchActivity> ignored = ActivityScenario.launch(workbench)) {
            onView(withId(R.id.repairWorkbenchToolbar)).check(matches(isDisplayed()));
            onView(withId(R.id.repairSubmitEntry)).check(matches(isDisplayed()));
        }
        try (ActivityScenario<RepairSubmitActivity> ignored = ActivityScenario.launch(
                new Intent(application, RepairSubmitActivity.class))) {
            onView(withId(R.id.repairSubmitToolbar)).check(matches(isDisplayed()));
            onView(withId(R.id.repairSubmitEpcContainer)).check(matches(isDisplayed()));
            onView(withId(R.id.repairSubmitAction)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void repairActivitiesAreNotExported() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertFalse(activityInfo(context, RepairWorkbenchActivity.class).exported);
        assertFalse(activityInfo(context, RepairSubmitActivity.class).exported);
        assertFalse(activityInfo(context, RepairDetailActivity.class).exported);
        assertFalse(activityInfo(context, RepairStartActivity.class).exported);
        assertFalse(activityInfo(context, RepairFinishActivity.class).exported);
        assertFalse(activityInfo(context, RepairerPickerActivity.class).exported);
    }

    private ActivityInfo activityInfo(Context context, Class<?> type) throws Exception {
        return context.getPackageManager().getActivityInfo(new ComponentName(context, type), 0);
    }
}
