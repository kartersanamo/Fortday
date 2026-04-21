package com.fortday.engine;

import org.bukkit.plugin.java.JavaPlugin;

public final class FortdayEnginePlugin extends JavaPlugin {
    private MatchLifecycleManager lifecycleManager;

    @Override
    public void onEnable() {
        // Some build pipelines may not package config.yml yet; avoid hard-failing startup.
        if (getResource("config.yml") != null) {
            saveDefaultConfig();
        } else {
            getLogger().warning("No embedded config.yml found; using runtime defaults.");
        }
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
