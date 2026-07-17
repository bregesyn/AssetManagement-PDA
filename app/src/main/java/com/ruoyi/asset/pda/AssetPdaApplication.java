package com.ruoyi.asset.pda;

import android.app.Application;

import com.ruoyi.asset.pda.app.AppContainer;

/**
 * PDA 应用入口，只负责创建进程级依赖容器。
 */
public class AssetPdaApplication extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    public AppContainer getAppContainer() {
        if (appContainer == null) {
            throw new IllegalStateException("应用依赖容器尚未初始化");
        }
        return appContainer;
    }
}
