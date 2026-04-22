package com.fortday.engine.build;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.util.Transformation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GhostPreviewService {
    private static final int TILE_SIZE = 5;
    private static final int TILE_STRIDE = 4; // overlap corners by 1 block across adjacent pieces
    private static final int HALF_SPAN = TILE_SIZE / 2; // 2 for 5x5

    private final JavaPlugin plugin;
    private final ConcurrentMap<UUID, BuildPieceType> selectedPiece = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Map<BlockPosition, BlockDisplay>> renderedDisplays = new ConcurrentHashMap<>();

    public GhostPreviewService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public BuildPieceType selectedPiece(Player player) {
        return selectedPiece.getOrDefault(player.getUniqueId(), BuildPieceType.WALL);
    }

    public void setSelectedPiece(Player player, BuildPieceType type) {
        selectedPiece.put(player.getUniqueId(), type);
    }

    public void clearPlayer(Player player) {
        clearPreview(player);
        selectedPiece.remove(player.getUniqueId());
    }

    public void renderPreview(Player player) {
        if (!player.isOnline() || player.isDead()) {
            clearPlayer(player);
            return;
        }

        BuildPieceType piece = selectedPiece(player);
        Anchor anchor = resolveAnchor(player);
        if (anchor == null) {
            clearPreview(player);
            return;
        }

        rebuildDisplays(player, anchor, piece, player.getLocation().getYaw());
    }

    public void clearPreview(Player player) {
        Map<BlockPosition, BlockDisplay> displays = renderedDisplays.remove(player.getUniqueId());
        if (displays == null || displays.isEmpty()) {
            return;
        }
        for (BlockDisplay display : displays.values()) {
            display.remove();
        }
    }

    public int placePreview(Player player, Material material) {
        Map<BlockPosition, BlockDisplay> displays = renderedDisplays.get(player.getUniqueId());
        if (displays == null || displays.isEmpty()) {
            return 0;
        }

        World world = player.getWorld();
        int placed = 0;
        for (BlockPosition pos : displays.keySet()) {
            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
            if (block.getType().isAir()) {
                block.setType(material, false);
                placed++;
            }
        }
        return placed;
    }

    private Anchor resolveAnchor(Player player) {
        RayTraceResult result = player.rayTraceBlocks(6.0, FluidCollisionMode.NEVER);
        if (result == null || result.getHitBlock() == null) {
            return fallbackAnchor(player);
        }

        Block hit = result.getHitBlock();
        BlockFace face = result.getHitBlockFace();
        if (face == null) {
            return fallbackAnchor(player);
        }

        Block target = hit.getRelative(face);
        Location loc = target.getLocation();
        return new Anchor(
                target.getWorld(),
                snapToGrid(loc.getBlockX()),
                snapToGrid(loc.getBlockY()),
                snapToGrid(loc.getBlockZ()),
                face
        );
    }

    private Anchor fallbackAnchor(Player player) {
        // Fallback makes preview visible even when no block is directly targeted.
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize().multiply(3.0);
        Location target = eye.add(dir);
        return new Anchor(
                player.getWorld(),
                snapToGrid(target.getBlockX()),
                snapToGrid(target.getBlockY()),
                snapToGrid(target.getBlockZ()),
                yawToFace(player.getLocation().getYaw())
        );
    }

    private void rebuildDisplays(Player player, Anchor anchor, BuildPieceType piece, float yaw) {
        int x = anchor.x();
        int y = anchor.y();
        int z = anchor.z();
        BlockFace horizontal = toHorizontalFace(anchor.face(), yaw);
        World world = anchor.world();
        Set<BlockPosition> next = switch (piece) {
            case FLOOR -> floorBlocks(x, y, z);
            case CONE -> coneBlocks(x, y, z);
            case WALL -> wallBlocks(x, y, z, horizontal);
            case RAMP -> rampBlocks(x, y, z, horizontal);
        };

        Map<BlockPosition, BlockDisplay> current = renderedDisplays.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        Set<BlockPosition> toRemove = new HashSet<>(current.keySet());
        toRemove.removeAll(next);
        for (BlockPosition pos : toRemove) {
            BlockDisplay display = current.remove(pos);
            if (display != null) {
                display.remove();
            }
        }

        for (BlockPosition pos : next) {
            if (!current.containsKey(pos)) {
                current.put(pos, spawnDisplay(world, player, pos));
            }
        }
    }

    private Set<BlockPosition> floorBlocks(int x, int y, int z) {
        Set<BlockPosition> out = new HashSet<>();
        for (int dx = -HALF_SPAN; dx <= HALF_SPAN; dx++) {
            for (int dz = -HALF_SPAN; dz <= HALF_SPAN; dz++) {
                out.add(new BlockPosition(x + dx, y, z + dz));
            }
        }
        return out;
    }

    private Set<BlockPosition> coneBlocks(int x, int y, int z) {
        Set<BlockPosition> out = new HashSet<>();
        for (int dx = -HALF_SPAN; dx <= HALF_SPAN; dx++) {
            for (int dz = -HALF_SPAN; dz <= HALF_SPAN; dz++) {
                out.add(new BlockPosition(x + dx, y, z + dz));
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                out.add(new BlockPosition(x + dx, y + 1, z + dz));
            }
        }
        out.add(new BlockPosition(x, y + 2, z));
        return out;
    }

    private Set<BlockPosition> wallBlocks(int x, int y, int z, BlockFace face) {
        Set<BlockPosition> out = new HashSet<>();
        int wx = x;
        int wz = z;
        if (face == BlockFace.NORTH) {
            wz = z - HALF_SPAN;
        } else if (face == BlockFace.SOUTH) {
            wz = z + HALF_SPAN;
        } else if (face == BlockFace.EAST) {
            wx = x + HALF_SPAN;
        } else if (face == BlockFace.WEST) {
            wx = x - HALF_SPAN;
        }

        boolean northSouth = face == BlockFace.NORTH || face == BlockFace.SOUTH;
        for (int dy = 0; dy < TILE_SIZE; dy++) {
            for (int side = -HALF_SPAN; side <= HALF_SPAN; side++) {
                int bx = northSouth ? wx + side : wx;
                int bz = northSouth ? wz : wz + side;
                out.add(new BlockPosition(bx, y + dy, bz));
            }
        }
        return out;
    }

    private Set<BlockPosition> rampBlocks(int x, int y, int z, BlockFace face) {
        Set<BlockPosition> out = new HashSet<>();
        for (int step = 0; step < TILE_SIZE; step++) {
            int by = y + step;
            for (int side = -HALF_SPAN; side <= HALF_SPAN; side++) {
                int bx;
                int bz;
                switch (face) {
                    case NORTH -> {
                        bx = x + side;
                        bz = z + HALF_SPAN - step;
                    }
                    case SOUTH -> {
                        bx = x + side;
                        bz = z - HALF_SPAN + step;
                    }
                    case EAST -> {
                        bx = x - HALF_SPAN + step;
                        bz = z + side;
                    }
                    case WEST -> {
                        bx = x + HALF_SPAN - step;
                        bz = z + side;
                    }
                    default -> {
                        bx = x + side;
                        bz = z - HALF_SPAN + step;
                    }
                }
                out.add(new BlockPosition(bx, by, bz));
            }
        }
        return out;
    }

    private BlockFace toHorizontalFace(BlockFace face, float yaw) {
        if (face == BlockFace.UP || face == BlockFace.DOWN || !face.isCartesian()) {
            return yawToFace(yaw);
        }
        if (face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.WEST) {
            return face;
        }
        return yawToFace(yaw);
    }

    private BlockFace yawToFace(float yaw) {
        float rot = (yaw % 360 + 360) % 360;
        if (rot >= 45 && rot < 135) {
            return BlockFace.WEST;
        }
        if (rot >= 135 && rot < 225) {
            return BlockFace.NORTH;
        }
        if (rot >= 225 && rot < 315) {
            return BlockFace.EAST;
        }
        return BlockFace.SOUTH;
    }

    private int snapToGrid(int value) {
        return Math.floorDiv(value, TILE_STRIDE) * TILE_STRIDE;
    }

    private BlockDisplay spawnDisplay(World world, Player owner, BlockPosition pos) {
        Location origin = new Location(world, pos.x(), pos.y(), pos.z());
        BlockDisplay display = world.spawn(origin, BlockDisplay.class, entity -> {
            entity.setBlock(ghostMaterial().createBlockData());
            entity.setInterpolationDelay(0);
            entity.setInterpolationDuration(1);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setGlowColorOverride(org.bukkit.Color.fromRGB(85, 190, 255));
            entity.setGlowing(true);
            entity.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(1f, 1f, 1f),
                    new Quaternionf()
            ));
        });

        for (Player online : world.getPlayers()) {
            if (!online.getUniqueId().equals(owner.getUniqueId())) {
                online.hideEntity(plugin, display);
            } else {
                online.showEntity(plugin, display);
            }
        }
        return display;
    }

    private Material ghostMaterial() {
        return Material.LIGHT_BLUE_STAINED_GLASS;
    }

    private record Anchor(World world, int x, int y, int z, BlockFace face) {
    }

    private record BlockPosition(int x, int y, int z) {
    }

}
