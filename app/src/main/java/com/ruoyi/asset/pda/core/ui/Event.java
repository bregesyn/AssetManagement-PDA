package com.ruoyi.asset.pda.core.ui;

/**
 * LiveData 一次性事件，避免配置变化后重复执行页面跳转。
 */
public final class Event<T> {
    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    public synchronized T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    public T peekContent() {
        return content;
    }
}
