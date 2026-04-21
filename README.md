# Fortday

Mechanics-first Fortnite-in-Minecraft implementation using Spigot plugins and a custom resource-pack pipeline.

## Modules
- `modules/core-api`: shared contracts and event bus
- `modules/game-engine`: match state, storm, lifecycle
- `modules/build-edit`: building and editing systems
- `modules/combat-loot`: weapons, loot, inventory rules
- `modules/ui-hud`: HUD channels and render gateway
- `modules/map-world`: POIs, map metadata, traversal hooks
- `modules/lobby-matchmaking`: lobby and queue flow

## Quick Start
1. Install JDK 21 and Maven 3.9+
2. Run `mvn -q -DskipTests package`
3. Use module jars from each `modules/*/target/` directory
4. Build pack with `python3 scripts/build_resource_pack.py`
