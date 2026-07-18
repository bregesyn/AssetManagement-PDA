package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.dto.PdaLoginRequest;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;

public interface AuthRepository {
    SessionManager.State getSessionState();

    RequestHandle login(PdaLoginRequest request, RepositoryCallback<PdaUserDto> callback);

    RequestHandle profile(RepositoryCallback<PdaUserDto> callback);

    RequestHandle logout(RepositoryCallback<Void> callback);

    void clearLocalSession();
}
