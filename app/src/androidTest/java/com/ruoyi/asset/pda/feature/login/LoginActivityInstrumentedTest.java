package com.ruoyi.asset.pda.feature.login;

import android.text.InputType;
import android.text.method.PasswordTransformationMethod;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LoginActivityInstrumentedTest {
    @Before
    public void clearSession() {
        AssetPdaApplication application = ApplicationProvider.getApplicationContext();
        application.getAppContainer().getSessionManager().invalidate();
    }

    @Test
    public void emptySessionShowsFormAndValidatesRequiredUsername() {
        try (ActivityScenario<LoginActivity> ignored =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.usernameEditText)).check(matches(isDisplayed()));
            onView(withId(R.id.passwordEditText)).check(matches(isDisplayed()));

            onView(withId(R.id.loginButton)).perform(click());

            onView(withId(R.id.loginErrorText))
                    .check(matches(withText(R.string.login_username_required)))
                    .check(matches(isDisplayed()));

            // C6200 的中文输入法会保留按键组合，直接替换文本可避免测试依赖当前 IME。
            onView(withId(R.id.usernameEditText)).perform(replaceText("ceshi"));
            onView(withId(R.id.loginButton)).perform(click());
            onView(withId(R.id.loginErrorText))
                    .check(matches(withText(R.string.login_password_required)))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void credentialFieldsDoNotSaveViewState() {
        try (ActivityScenario<LoginActivity> scenario =
                     ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> {
                assertFalse(activity.findViewById(R.id.usernameEditText).isSaveEnabled());
                assertFalse(activity.findViewById(R.id.passwordEditText).isSaveEnabled());
                android.widget.EditText password =
                        activity.findViewById(R.id.passwordEditText);
                assertTrue(password.getTransformationMethod()
                        instanceof PasswordTransformationMethod);
                assertTrue((password.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0);
            });
        }
    }
}
