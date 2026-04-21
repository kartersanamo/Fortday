package com.fortday.engine.build;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GhostPreviewService {
    private final ConcurrentMap<UUID, BuildPieceType> selectedPiece = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Set<BlockPosition>> renderedBlocks = new ConcurrentHashMap<>();

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

        Set<BlockPosition> next = computePreview(anchor, piece, player.getLocation().getYaw());
        Set<BlockPosition> prev = renderedBlocks.getOrDefault(player.getUniqueId(), Set.of());
        World world = anchor.world();

        Material ghostMaterial = ghostMaterial(piece);
        BlockData ghostData = ghostMaterial.createBlockData();

        for (BlockPosition oldPos : prev) {
            if (!next.contains(oldPos)) {
                Block realBlock = world.getBlockAt(oldPos.x(), oldPos.y(), oldPos.z());
                player.sendBlockChange(toLocation(world, oldPos), realBlock.getBlockData());
            }
        }

        for (BlockPosition pos : next) {
            player.sendBlockChange(toLocation(world, pos), ghostData);
        }

        renderedBlocks.put(player.getUniqueId(), next);
    }

    public void clearPreview(Player player) {
        Set<BlockPosition> prev = renderedBlocks.remove(player.getUniqueId());
        if (prev == null || prev.isEmpty()) {
            return;
        }
        World world = player.getWorld();
        for (BlockPosition pos : prev) {
            Block realBlock = world.getBlockAt(pos.x(), pos.y(), pos.z());
            player.sendBlockChange(toLocation(world, pos), realBlock.getBlockData());
        }
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
        return new Anchor(target.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), face);
    }

    private Anchor fallbackAnchor(Player player) {
        // Fallback makes preview visible even when no block is directly targeted.
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize().multiply(3.0);
        Location target = eye.add(dir);
        return new Anchor(
                player.getWorld(),
                target.getBlockX(),
                target.getBlockY(),
                target.getBlockZ(),
                yawToFace(player.getLocation().getYaw())
        );
    }

    private Set<BlockPosition> computePreview(Anchor anchor, BuildPieceType piece, float yaw) {
        Set<BlockPosition> out = new HashSet<>();
        int x = anchor.x();
        int y = anchor.y();
        int z = anchor.z();
        BlockFace horizontal = toHorizontalFace(anchor.face(), yaw);

        switch (piece) {
            case WALL -> {
                Vector normal = faceToNormal(horizontal);
                Vector axis = new Vector(normal.getZ(), 0, -normal.getX());
                for (int dy = 0; dy < 3; dy++) {
                    for (int side = -1; side <= 1; side++) {
                        int bx = x + axis.getBlockX() * side;
                        int bz = z + axis.getBlockZ() * side;
                        out.add(new BlockPosition(bx, y + dy, bz));
                    }
                }
            }
            case FLOOR -> {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        out.add(new BlockPosition(x + dx, y, z + dz));
                    }
                }
            }
            case CONE -> {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        out.add(new BlockPosition(x + dx, y, z + dz));
                    }
                }
                out.add(new BlockPosition(x, y + 1, z));
            }
            case RAMP -> {
                Vector forward = faceToNormal(horizontal);
                for (int step = 0; step < 3; step++) {
                    int by = y + step;
                    int cx = x + forward.getBlockX() * step;
                    int cz = z + forward.getBlockZ() * step;
                    Vector axis = new Vector(forward.getZ(), 0, -forward.getX());
                    for (int side = -1; side <= 1; side++) {
                        int bx = cx + axis.getBlockX() * side;
                        int bz = cz + axis.getBlockZ() * side;
                        out.add(new BlockPosition(bx, by, bz));
                    }
                }
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

    private Vector faceToNormal(BlockFace face) {
        return switch (face) {
            case NORTH -> new Vector(0, 0, -1);
            case SOUTH -> new Vector(0, 0, 1);
            case EAST -> new Vector(1, 0, 0);
            case WEST -> new Vector(-1, 0, 0);
            default -> new Vector(0, 0, 1);
        };
    }

    private Material ghostMaterial(BuildPieceType piece) {
        return switch (piece) {
            // Reserved preview-only materials. Resource pack remaps these to ghost visuals.
            case WALL -> Material.AMETHYST_BLOCK;
            case FLOOR -> Material.CALCITE;
            case RAMP -> Material.TUFF;
            case CONE -> Material.POLISHED_DEEPSLATE;
        };
    }

    private Location toLocation(World world, BlockPosition pos) {
        return new Location(world, pos.x(), pos.y(), pos.z());
    }

    private record Anchor(World world, int x, int y, int z, BlockFace face) {
    }

    private record BlockPosition(int x, int y, int z) {
    }
}
