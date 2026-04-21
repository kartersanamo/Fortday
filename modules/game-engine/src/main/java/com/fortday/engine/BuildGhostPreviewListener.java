package com.fortday.engine;

import com.fortday.engine.build.BuildPieceType;
import com.fortday.engine.build.GhostPreviewService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BuildGhostPreviewListener implements Listener {
    private final GhostPreviewService previewService;

    public BuildGhostPreviewListener(GhostPreviewService previewService) {
        this.previewService = previewService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BuildPieceType type = mapSlotToPiece(player.getInventory().getHeldItemSlot());
        previewService.setSelectedPiece(player, type);
        sendPieceHud(player, type);
        previewService.renderPreview(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        previewService.clearPlayer(event.getPlayer());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        BuildPieceType type = mapSlotToPiece(event.getNewSlot());
        Player player = event.getPlayer();
        previewService.setSelectedPiece(player, type);
        sendPieceHud(player, type);
        previewService.renderPreview(player);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().distanceSquared(event.getTo()) < 0.0001
                && event.getFrom().getYaw() == event.getTo().getYaw()
                && event.getFrom().getPitch() == event.getTo().getPitch()) {
            return;
        }
        previewService.renderPreview(event.getPlayer());
    }

    private BuildPieceType mapSlotToPiece(int slot) {
        return switch (slot) {
            case 0 -> BuildPieceType.WALL;
            case 1 -> BuildPieceType.FLOOR;
            case 2 -> BuildPieceType.RAMP;
            case 3 -> BuildPieceType.CONE;
            default -> BuildPieceType.WALL;
        };
    }

    private void sendPieceHud(Player player, BuildPieceType type) {
        player.sendMessage(ChatColor.AQUA + "Build Piece: " + ChatColor.WHITE + type.name()
                + ChatColor.DARK_GRAY + " (use slots 1-4)");
    }
}
