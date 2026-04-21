package com.fortday.engine;

import com.fortday.engine.build.GhostPreviewService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FortdayEnginePlugin extends JavaPlugin {
    private MatchLifecycleManager lifecycleManager;
    private GhostPreviewService ghostPreviewService;
    private int previewTaskId = -1;

    @Override
    public void onEnable() {
        // Some build pipelines may not package config.yml yet; avoid hard-failing startup.
        if (getResource("config.yml") != null) {
            saveDefaultConfig();
        } else {
            getLogger().warning("No embedded config.yml found; using runtime defaults.");
        }
        this.ghostPreviewService = new GhostPreviewService();
        Bukkit.getPluginManager().registerEvents(
                new BuildGhostPreviewListener(ghostPreviewService),
                this
        );
        this.previewTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ghostPreviewService.renderPreview(player);
            }
        }, 1L, 2L);
        this.lifecycleManager = new MatchLifecycleManager(getLogger());
        lifecycleManager.bootstrap();
    }

    @Override
    public void onDisable() {
        if (previewTaskId != -1) {
            Bukkit.getScheduler().cancelTask(previewTaskId);
        }
        if (ghostPreviewService != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ghostPreviewService.clearPreview(player);
            }
        }
        if (lifecycleManager != null) {
            lifecycleManager.shutdown();
        }
    }
}
