package com.fortday.build;

public final class BuildPlacementService {
    public boolean canPlace(BuildPieceType type, boolean hasMaterials, boolean supportValid) {
        return hasMaterials && supportValid && type != null;
    }
}
