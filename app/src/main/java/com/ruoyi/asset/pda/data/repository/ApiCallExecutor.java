package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.network.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * 统一执行 AjaxResult 风格请求，避免各 Repository 重复处理协议与错误。
 */
public final class ApiCallExecutor {
    private final ApiErrorMapper errorMapper;

    public ApiCallExecutor(ApiErrorMapper errorMapper) {
        if (errorMapper == null) {
            throw new IllegalArgumentException("ApiErrorMapper 不能为空");
        }
        this.errorMapper = errorMapper;
    }

    public <T> RequestHandle execute(Call<ApiResponse<T>> call, boolean requireData,
            RepositoryCallback<T> callback) {
        if (call == null || callback == null) {
            throw new IllegalArgumentException("请求和回调不能为空");
        }
        long requestGeneration = errorMapper.getSessionGeneration();
        call.enqueue(new Callback<ApiResponse<T>>() {
            @Override
            public void onResponse(Call<ApiResponse<T>> currentCall,
                    Response<ApiResponse<T>> response) {
                if (currentCall.isCanceled()) {
                    return;
                }
                if (!response.isSuccessful()) {
                    callback.onError(errorMapper.mapThrowable(new HttpException(response)));
                    return;
                }
                ApiResponse<T> body = response.body();
                if (body == null || body.getCode() == null) {
                    callback.onError(protocolError());
                    return;
                }
                if (!body.isSuccess()) {
                    callback.onError(errorMapper.mapResponse(body, requestGeneration));
                    return;
                }
                if (requireData && body.getData() == null) {
                    callback.onError(protocolError());
                    return;
                }
                callback.onSuccess(body.getData());
            }

            @Override
            public void onFailure(Call<ApiResponse<T>> currentCall, Throwable throwable) {
                if (!currentCall.isCanceled()) {
                    callback.onError(errorMapper.mapThrowable(throwable));
                }
            }
        });
        return call::cancel;
    }

    ApiErrorMapper.ApiError protocolError() {
        return errorMapper.mapResponse(null);
    }
}
