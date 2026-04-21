package com.fortday.world;

public final class MapScaleService {
    private final double scaleFactor;

    public MapScaleService(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public int toMinecraftCoordinate(double fortniteUnits) {
        return (int) Math.round(fortniteUnits * scaleFactor);
    }
}
