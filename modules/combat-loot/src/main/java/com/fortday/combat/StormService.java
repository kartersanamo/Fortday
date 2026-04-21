package com.fortday.combat;

import java.util.List;

public final class StormService {
    private final List<StormPhase> phases;

    public StormService(List<StormPhase> phases) {
        this.phases = phases;
    }

    public StormPhase phaseAt(int index) {
        return phases.get(Math.max(0, Math.min(index, phases.size() - 1)));
    }
}
