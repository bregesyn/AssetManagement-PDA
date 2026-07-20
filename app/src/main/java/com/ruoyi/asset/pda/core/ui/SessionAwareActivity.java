package com.ruoyi.asset.pda.core.ui;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.feature.login.LoginActivity;

/** 只负责阶段 3 作业页的 Session 失效导航，不承载网络或硬件业务。 */
public abstract class SessionAwareActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private boolean navigatingToLogin;
    private final SessionManager.Listener sessionListener = this::navigateToLogin;

    protected final boolean initializeSessionGuard(SessionManager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("SessionManager 不能为空");
        }
        sessionManager = manager;
        if (manager.getState() != SessionManager.State.VALID) {
            navigateToLogin();
            return false;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sessionManager != null) {
            sessionManager.addListener(sessionListener);
            if (sessionManager.getState() != SessionManager.State.VALID) {
                navigateToLogin();
            }
        }
    }

    @Override
    protected void onStop() {
        if (sessionManager != null) {
            sessionManager.removeListener(sessionListener);
        }
        super.onStop();
    }

    protected final void navigateToLogin() {
        if (navigatingToLogin || isFinishing()) {
            return;
        }
        navigatingToLogin = true;
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
