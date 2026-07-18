package com.ruoyi.asset.pda.data.repository;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;

/**
 * Repository 对 ViewModel 暴露的最小异步结果接口。
 */
public interface RepositoryCallback<T> {
    void onSuccess(T data);

    void onError(ApiErrorMapper.ApiError error);
}
