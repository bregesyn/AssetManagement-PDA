package com.ruoyi.asset.pda;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ruoyi.asset.pda.core.network.SessionCookieJar;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ApplicationFoundationInstrumentedTest {
    @Test
    public void applicationContainerIsProcessScopedAndBackupIsDisabled() {
        AssetPdaApplication application = ApplicationProvider.getApplicationContext();

        assertSame(application.getAppContainer(), application.getAppContainer());
        assertFalse((application.getApplicationInfo().flags & ApplicationInfo.FLAG_ALLOW_BACKUP) != 0);
    }

    @Test
    public void encryptedSessionCookieSurvivesStorageRecreation() {
        Context context = ApplicationProvider.getApplicationContext();
        HttpUrl url = HttpUrl.get("http://192.168.0.105:8030/asset/pda/profile");
        SessionCookieJar firstJar = new SessionCookieJar(context);
        firstJar.clear();
        Cookie cookie = new Cookie.Builder()
                .name("JSESSIONID")
                .value("instrumented-session-value")
                .hostOnlyDomain("192.168.0.105")
                .path("/")
                .httpOnly()
                .build();
        firstJar.saveFromResponse(url, Collections.singletonList(cookie));

        SessionCookieJar restoredJar = new SessionCookieJar(context);
        List<Cookie> restoredCookies = restoredJar.loadForRequest(url);

        assertEquals(1, restoredCookies.size());
        assertEquals("instrumented-session-value", restoredCookies.get(0).value());
        assertTrue(restoredCookies.get(0).httpOnly());
        restoredJar.clear();
    }
}
