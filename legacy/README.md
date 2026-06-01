# AmongUs — Paper 1.16.5 build

A backport of the [main 1.21.4 plugin](../README.md) for **Paper 1.16.5**. Same
game (roles, tasks, clickable voting, sabotages, ghost chat, vents) — the only
differences are the platform pieces that don't exist on 1.16.5:

| Area | 1.21.4 build | This 1.16.5 build |
|------|--------------|-------------------|
| Corpses | Display entities + Interaction hitbox | **Armor stand** wearing the dead player's head (right-click it to report) |
| Text/UI | Adventure `Component` | Legacy `§` strings, `sendTitle`, action bar via Spigot |
| Boss bar | Adventure BossBar | Bukkit `BossBar` (`Bukkit.createBossBar`) |
| Lights sabotage | `DARKNESS` + `BLINDNESS` | `BLINDNESS` only (`DARKNESS` is 1.19+) |
| Ghost chat | `AsyncChatEvent` | `AsyncPlayerChatEvent` |

## Requirements

- **Paper 1.16.5** (build ~794, i.e. a final 1.16.5 build).
- **Java 16 or newer** (the build targets Java 16; 1.16.5 runs fine on 16/17).

## Building

```bash
cd legacy
mvn package
```

Pulls `com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT` from
`repo.papermc.io`. Output: `legacy/target/AmongUs-1.16.5-1.0.0.jar`.

Everything else — commands, config, gameplay and arena setup — is identical to
the main build, so see the [root README](../README.md). Note the extra optional
step on 1.16.5 is the same: `/amongus admin addfix`, `addvent`, etc.
