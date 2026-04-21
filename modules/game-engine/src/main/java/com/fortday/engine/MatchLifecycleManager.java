package com.fortday.engine;

import java.util.logging.Logger;

public final class MatchLifecycleManager {
    private final Logger logger;

    public MatchLifecycleManager(Logger logger) {
        this.logger = logger;
    }

    public void bootstrap() {
        logger.info("Fortday match lifecycle initialized");
    }

    public void shutdown() {
        logger.info("Fortday match lifecycle shutdown complete");
    }
}
