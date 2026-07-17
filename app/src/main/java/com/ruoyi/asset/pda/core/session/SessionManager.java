package com.ruoyi.asset.pda.core.session;

import com.ruoyi.asset.pda.core.network.SessionCookieJar;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

/**
 * 统一维护本地会话状态；权限与真实用户事实仍由后端校验。
 */
public final class SessionManager {
    private final SessionCookieJar cookieJar;
    private final Executor listenerExecutor;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private volatile State state;

    public SessionManager(SessionCookieJar cookieJar, Executor listenerExecutor) {
        if (cookieJar == null) {
            throw new IllegalArgumentException("SessionCookieJar 不能为空");
        }
        if (listenerExecutor == null) {
            throw new IllegalArgumentException("Session 监听器执行器不能为空");
        }
        this.cookieJar = cookieJar;
        this.listenerExecutor = listenerExecutor;
        state = cookieJar.isEmpty() ? State.INVALID : State.PENDING_VALIDATION;
    }

    public State getState() {
        return state;
    }

    public synchronized void markPendingValidation() {
        state = cookieJar.isEmpty() ? State.INVALID : State.PENDING_VALIDATION;
    }

    public synchronized void markValid() {
        if (cookieJar.isEmpty()) {
            state = State.INVALID;
            throw new IllegalStateException("没有可用的 Session Cookie");
        }
        state = State.VALID;
    }

    public synchronized long getGeneration() {
        return cookieJar.getGeneration();
    }

    public void invalidate() {
        invalidateInternal(null);
    }

    /**
     * 仅当响应仍属于当前会话代次时失效会话，防止旧请求的迟到 401 清除新登录 Cookie。
     */
    public boolean invalidate(long expectedGeneration) {
        return invalidateInternal(expectedGeneration);
    }

    private boolean invalidateInternal(Long expectedGeneration) {
        boolean shouldNotify;
        synchronized (this) {
            shouldNotify = state != State.INVALID || !cookieJar.isEmpty();
            if (expectedGeneration != null
                    && !cookieJar.clearIfGeneration(expectedGeneration)) {
                return false;
            }
            if (expectedGeneration == null) {
                cookieJar.clear();
            }
            state = State.INVALID;
        }
        if (shouldNotify) {
            // 网络拦截器可能在后台线程触发失效，页面监听器必须由装配层派发到 UI 线程。
            listenerExecutor.execute(() -> {
                for (Listener listener : listeners) {
                    listener.onSessionInvalidated();
                }
            });
        }
        return true;
    }

    public void addListener(Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public enum State {
        PENDING_VALIDATION,
        VALID,
        INVALID
    }

    public interface Listener {
        void onSessionInvalidated();
    }
}
