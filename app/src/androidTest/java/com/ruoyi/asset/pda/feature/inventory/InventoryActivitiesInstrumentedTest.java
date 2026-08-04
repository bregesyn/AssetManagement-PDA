package com.ruoyi.asset.pda.feature.inventory;

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

@RunWith(AndroidJUnit4.class)
public class InventoryActivitiesInstrumentedTest {
    private AssetPdaApplication application;

    @Before
    public void createValidLocalSession() {
        application = ApplicationProvider.getApplicationContext();
        AppContainer container = application.getAppContainer();
        // 直接清理测试 Cookie，避免 invalidate 的异步导航通知在新页面注册监听后迟到。
        container.getSessionCookieJar().clear();
        container.getSessionManager().markPendingValidation();
        HttpUrl url = HttpUrl.get(BuildConfig.BASE_URL + "asset/pda/profile");
        Cookie cookie = new Cookie.Builder().name("JSESSIONID")
                .value("inventory-instrumented-session").hostOnlyDomain(url.host())
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
    public void inventoryListAndExecuteExposePrototypePrimaryControls() {
        android.content.Intent listIntent = new android.content.Intent(
                application, InventoryTaskListActivity.class)
                .putExtra(InventoryTaskListActivity.EXTRA_SKIP_INITIAL_LOAD, true);
        try (ActivityScenario<InventoryTaskListActivity> ignored =
                     ActivityScenario.launch(listIntent)) {
            onView(withId(R.id.inventoryTaskToolbar)).check(matches(isDisplayed()));
            onView(withId(R.id.inventoryTaskActionTab)).check(matches(isDisplayed()));
        }
        try (ActivityScenario<InventoryExecuteActivity> ignored = ActivityScenario.launch(
                new android.content.Intent(application, InventoryExecuteActivity.class)
                        .putExtra(InventoryExecuteActivity.EXTRA_TASK_ID, 7L)
                        .putExtra(InventoryExecuteActivity.EXTRA_TASK_NO, "TASK-001")
                        .putExtra(InventoryExecuteActivity.EXTRA_SKIP_INITIAL_LOAD, true))) {
            onView(withId(R.id.inventoryProgress)).check(matches(isDisplayed()));
            onView(withId(R.id.inventoryExpectedList)).check(matches(isDisplayed()));
            onView(withId(R.id.inventoryLocationInput)).check(matches(isDisplayed()));
            onView(withId(R.id.inventoryPrimaryAction)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void inventoryActivitiesAreNotExported() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertFalse(activityInfo(context, InventoryTaskListActivity.class).exported);
        assertFalse(activityInfo(context, InventoryExecuteActivity.class).exported);
    }

    private ActivityInfo activityInfo(Context context, Class<?> type) throws Exception {
        return context.getPackageManager().getActivityInfo(new ComponentName(context, type), 0);
    }
}
