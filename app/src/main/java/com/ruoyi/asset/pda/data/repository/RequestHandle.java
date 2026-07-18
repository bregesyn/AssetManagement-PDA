package com.ruoyi.asset.pda.data.repository;

/**
 * 隐藏 Retrofit Call，页面只需要表达取消当前请求。
 */
public interface RequestHandle {
    RequestHandle NONE = () -> {
    };

    void cancel();
}
