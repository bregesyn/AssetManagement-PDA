package com.ruoyi.asset.pda.core.network;

import com.google.gson.JsonParseException;
import com.google.gson.stream.MalformedJsonException;
import com.ruoyi.asset.pda.core.session.SessionManager;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.HttpException;

/**
 * 将传输、协议和后端业务失败转换为稳定且不泄露内部细节的错误。
 */
public final class ApiErrorMapper {
    private static final String SESSION_EXPIRED_MESSAGE = "登录状态已失效，请重新登录";

    private final SessionManager sessionManager;

    public ApiErrorMapper(SessionManager sessionManager) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("SessionManager 不能为空");
        }
        this.sessionManager = sessionManager;
    }

    public ApiError mapResponse(ApiResponse<?> response) {
        return mapResponse(response, null);
    }

    /**
     * 按请求发出时的会话代次处理业务 401，避免旧请求迟到后清除新账号 Session。
     */
    public ApiError mapResponse(ApiResponse<?> response, Long expectedGeneration) {
        if (response == null || response.getCode() == null) {
            return new ApiError(Kind.PROTOCOL, "服务响应格式异常，请稍后重试");
        }
        if (response.isSuccess()) {
            throw new IllegalArgumentException("成功响应不应映射为错误");
        }
        if (response.isSessionExpired()) {
            if (expectedGeneration == null) {
                sessionManager.invalidate();
            } else {
                sessionManager.invalidate(expectedGeneration);
            }
            return new ApiError(Kind.SESSION_EXPIRED, SESSION_EXPIRED_MESSAGE);
        }
        return new ApiError(Kind.BUSINESS,
                hasText(response.getMessage()) ? response.getMessage().trim() : "操作失败，请重试");
    }

    public long getSessionGeneration() {
        return sessionManager.getGeneration();
    }

    public ApiError mapThrowable(Throwable throwable) {
        if (throwable instanceof HttpException) {
            return mapHttpStatus(((HttpException) throwable).code());
        }
        if (throwable instanceof SocketTimeoutException) {
            return new ApiError(Kind.TIMEOUT, "请求超时，请检查网络后重试");
        }
        if (throwable instanceof JsonParseException || throwable instanceof MalformedJsonException
                || throwable instanceof EOFException) {
            return new ApiError(Kind.PROTOCOL, "服务响应格式异常，请稍后重试");
        }
        if (throwable instanceof UnknownHostException || throwable instanceof ConnectException
                || throwable instanceof IOException) {
            return new ApiError(Kind.NETWORK, "网络连接失败，请检查网络后重试");
        }
        return new ApiError(Kind.SYSTEM, "系统暂时不可用，请稍后重试");
    }

    private ApiError mapHttpStatus(int statusCode) {
        if (statusCode == 401) {
            // HTTP 401 的会话清理由拦截器按请求代次完成，避免旧请求误清除新登录会话。
            return new ApiError(Kind.SESSION_EXPIRED, SESSION_EXPIRED_MESSAGE);
        }
        if (statusCode == 403) {
            return new ApiError(Kind.BUSINESS, "无权执行当前操作");
        }
        if (statusCode >= 500) {
            return new ApiError(Kind.SYSTEM, "服务暂时不可用，请稍后重试");
        }
        return new ApiError(Kind.PROTOCOL, "请求未被服务端接受，请稍后重试");
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public enum Kind {
        NETWORK,
        TIMEOUT,
        PROTOCOL,
        SESSION_EXPIRED,
        BUSINESS,
        SYSTEM
    }

    public static final class ApiError {
        private final Kind kind;
        private final String message;

        private ApiError(Kind kind, String message) {
            this.kind = kind;
            this.message = message;
        }

        public Kind getKind() {
            return kind;
        }

        public String getMessage() {
            return message;
        }
    }
}
