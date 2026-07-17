package com.ruoyi.asset.pda.core.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ruoyi.asset.pda.core.session.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 进程内唯一的 Retrofit、OkHttp 与 Gson 入口。
 */
public final class ApiClient {
    private final Gson gson;
    private final OkHttpClient okHttpClient;
    private final Retrofit retrofit;
    private final ApiErrorMapper errorMapper;

    public ApiClient(String baseUrl, SessionCookieJar cookieJar, SessionManager sessionManager) {
        if (cookieJar == null || sessionManager == null) {
            throw new IllegalArgumentException("网络会话组件不能为空");
        }
        String checkedBaseUrl = validateBaseUrl(baseUrl);
        gson = new GsonBuilder().create();
        errorMapper = new ApiErrorMapper(sessionManager);
        okHttpClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                // 只依据真实的 HTTP 401 清理会话，不读取或记录响应正文。
                .addInterceptor(new SessionExpirationInterceptor(sessionManager))
                .build();
        retrofit = new Retrofit.Builder()
                .baseUrl(checkedBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public <T> T create(Class<T> serviceType) {
        if (serviceType == null) {
            throw new IllegalArgumentException("API 类型不能为空");
        }
        return retrofit.create(serviceType);
    }

    public Gson getGson() {
        return gson;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public ApiErrorMapper getErrorMapper() {
        return errorMapper;
    }

    private static String validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("当前构建环境尚未配置 BASE_URL");
        }
        String checked = baseUrl.trim();
        if (!checked.endsWith("/")) {
            throw new IllegalStateException("BASE_URL 必须以 / 结尾");
        }
        return checked;
    }

    private static final class SessionExpirationInterceptor implements Interceptor {
        private final SessionManager sessionManager;

        private SessionExpirationInterceptor(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            long requestGeneration = sessionManager.getGeneration();
            Response response = chain.proceed(chain.request());
            if (response.code() == 401) {
                sessionManager.invalidate(requestGeneration);
            }
            return response;
        }
    }
}
