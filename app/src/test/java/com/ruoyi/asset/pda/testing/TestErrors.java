package com.ruoyi.asset.pda.testing;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.network.ApiResponse;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.core.network.TestNetworkFactory;

import java.io.IOException;

public final class TestErrors {
    private TestErrors() {
    }

    public static ApiErrorMapper.ApiError network() {
        return newMapper().mapThrowable(new IOException("test network"));
    }

    public static ApiErrorMapper.ApiError business(String message) {
        return newMapper().mapResponse(new ApiResponse<>(500, message, null));
    }

    public static ApiErrorMapper.ApiError sessionExpired() {
        return newMapper().mapResponse(new ApiResponse<>(401, "登录状态已失效，请重新登录", null));
    }

    public static ApiErrorMapper.ApiError protocol() {
        return newMapper().mapResponse(null);
    }

    private static ApiErrorMapper newMapper() {
        SessionManager manager = new SessionManager(
                TestNetworkFactory.newCookieJar(), Runnable::run);
        return new ApiErrorMapper(manager);
    }
}
