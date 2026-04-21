package com.fortday.engine;

import org.bukkit.plugin.java.JavaPlugin;

public final class FortdayEnginePlugin extends JavaPlugin {
    private MatchLifecycleManager lifecycleManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.lifecycleManager = new MatchLifecycleManager(getLogger());
        lifecycleManager.bootstrap();
    }

    @Override
    public void onDisable() {
        if (lifecycleManager != null) {
            lifecycleManager.shutdown();
        }
    }
}
