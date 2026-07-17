package com.ruoyi.asset.pda.core.network;

final class InMemoryCookieStorage implements SessionCookieJar.CookieStorage {
    private String value;

    @Override
    public String read() {
        return value;
    }

    @Override
    public void write(String value) {
        this.value = value;
    }

    @Override
    public void clear() {
        value = null;
    }

    void setRawValue(String value) {
        this.value = value;
    }
}
