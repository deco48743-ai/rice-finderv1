# Asian Rice Finder

Asian Rice Finder is a Meteor Client addon that scores **already-loaded client chunks** for amethyst/geode evidence and nearby player activity. It never loads chunks, reads packets, probes the server, or bypasses server protections.

## Compatibility

This project follows the current official Meteor addon template:

- Minecraft 26.2
- Fabric Loader 0.19.3
- Meteor Client 26.2-SNAPSHOT
- Java 25

Use the matching Meteor development build. Version numbers are centralized in `gradle/libs.versions.toml`.

## Build

1. Install JDK 25 and Gradle 9.2 or newer.
2. In this folder run `gradle build` in PowerShell.
3. Copy `build/libs/asian-rice-finder-1.0.0.jar` to the Fabric instance's `mods` directory alongside the matching Meteor Client build.

## How it works

The module queues loaded chunks around the player and scans a configurable number per update. It recognizes connected amethyst evidence as a single geode, caches findings, finds cross-chunk clusters from cached geode centers, and samples player motion from entities the client can see. Missing chunks are marked unscannable and are not treated as evidence.

The HUD-style look-at message is intentionally a chat action-bar message, rather than a screen overlay. It only appears for highlighted chunks when enabled.
