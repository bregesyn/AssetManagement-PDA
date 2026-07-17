package com.ruoyi.asset.pda.core.network;

public final class TestNetworkFactory {
    private TestNetworkFactory() {
    }

    public static SessionCookieJar newCookieJar() {
        return new SessionCookieJar(new InMemoryCookieStorage());
    }
}
