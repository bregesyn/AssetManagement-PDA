package com.ruoyi.asset.pda.testing;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.dto.PdaLoginRequest;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.repository.AuthRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

public final class FakeAuthRepository implements AuthRepository {
    private SessionManager.State sessionState = SessionManager.State.INVALID;
    private RepositoryCallback<PdaUserDto> loginCallback;
    private RepositoryCallback<PdaUserDto> profileCallback;
    private RepositoryCallback<Void> logoutCallback;
    private PdaLoginRequest loginRequest;
    private RecordingHandle lastHandle;
    private int loginCount;
    private int profileCount;
    private int logoutCount;
    private int clearCount;

    public void setSessionState(SessionManager.State sessionState) {
        this.sessionState = sessionState;
    }

    @Override
    public SessionManager.State getSessionState() {
        return sessionState;
    }

    @Override
    public RequestHandle login(PdaLoginRequest request,
            RepositoryCallback<PdaUserDto> callback) {
        loginCount++;
        loginRequest = request;
        loginCallback = callback;
        lastHandle = new RecordingHandle();
        return lastHandle;
    }

    @Override
    public RequestHandle profile(RepositoryCallback<PdaUserDto> callback) {
        profileCount++;
        profileCallback = callback;
        lastHandle = new RecordingHandle();
        return lastHandle;
    }

    @Override
    public RequestHandle logout(RepositoryCallback<Void> callback) {
        logoutCount++;
        logoutCallback = callback;
        lastHandle = new RecordingHandle();
        return lastHandle;
    }

    @Override
    public void clearLocalSession() {
        clearCount++;
        sessionState = SessionManager.State.INVALID;
    }

    public void completeLogin(PdaUserDto user) {
        sessionState = SessionManager.State.VALID;
        loginCallback.onSuccess(user);
    }

    public void failLogin(ApiErrorMapper.ApiError error) {
        loginCallback.onError(error);
    }

    public void completeProfile(PdaUserDto user) {
        sessionState = SessionManager.State.VALID;
        profileCallback.onSuccess(user);
    }

    public void failProfile(ApiErrorMapper.ApiError error) {
        if (error.getKind() == ApiErrorMapper.Kind.SESSION_EXPIRED) {
            sessionState = SessionManager.State.INVALID;
        }
        profileCallback.onError(error);
    }

    public void completeLogout() {
        sessionState = SessionManager.State.INVALID;
        logoutCallback.onSuccess(null);
    }

    public void failLogout(ApiErrorMapper.ApiError error) {
        sessionState = SessionManager.State.INVALID;
        logoutCallback.onError(error);
    }

    public PdaLoginRequest getLoginRequest() {
        return loginRequest;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public int getProfileCount() {
        return profileCount;
    }

    public int getLogoutCount() {
        return logoutCount;
    }

    public int getClearCount() {
        return clearCount;
    }

    public boolean isLastRequestCanceled() {
        return lastHandle != null && lastHandle.canceled;
    }

    private static final class RecordingHandle implements RequestHandle {
        private boolean canceled;

        @Override
        public void cancel() {
            canceled = true;
        }
    }
}
