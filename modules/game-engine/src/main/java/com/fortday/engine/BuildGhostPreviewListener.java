package com.fortday.engine;

import com.fortday.engine.build.BuildPieceType;
import com.fortday.engine.build.GhostPreviewService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuildGhostPreviewListener implements Listener {
    private static final long PLACE_DEBOUNCE_MS = 75L;

    private final GhostPreviewService previewService;
    private final Map<UUID, Long> lastPlaceAt = new ConcurrentHashMap<>();

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
        lastPlaceAt.remove(event.getPlayer().getUniqueId());
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

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long last = lastPlaceAt.get(player.getUniqueId());
        if (last != null && now - last < PLACE_DEBOUNCE_MS) {
            return;
        }
        int placed = previewService.placePreview(player, Material.OAK_PLANKS);
        if (placed > 0) {
            lastPlaceAt.put(player.getUniqueId(), now);
            event.setCancelled(true);
            previewService.renderPreview(player);
        }
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
