# AmongUs — Murder Mystery / Among Us for Paper 1.21.4

A self-contained social-deduction minigame plugin for **Paper 1.21.4** (Java 21).
Crewmates race to finish their tasks while hidden impostors pick them off one by
one. Bodies are left in the world as **Display-entity corpses**, anyone can call
a meeting, and suspects are ejected through a **clickable voting menu**.

> **Быстрый старт:** соберите `mvn package`, положите `AmongUs-1.0.0.jar` в
> `plugins/`, настройте арену командами `/amongus admin …`, затем `/amongus join`
> и `/amongus start`. Подробности ниже.

---

## Features

| Requested | How it's implemented |
|-----------|----------------------|
| **Роли (Roles)** | `CREWMATE` and `IMPOSTOR`, assigned randomly at round start (impostor count is configurable and clamped so the crew always starts in the majority). Each player gets a private role reveal title. |
| **Задания (Tasks)** | Admin-placed task stations. Each crewmate is assigned a random subset. Two mini-games: **Calibrate** (a clickable GUI — hit the moving lime pane) and **Download** (a timed channel with an action-bar progress meter). A global boss bar shows overall completion; 100 % is a crew win. |
| **Голосование (Voting — clickable menu)** | Reports and emergency buttons open a meeting: a discussion timer, then an inventory GUI with one **player head per suspect** plus a **Skip** button. One click = one vote. Votes are tallied; ties or a skip-majority eject no one. |
| **Трупы из Display (Corpses from Display entities)** | When an impostor kills, a corpse is built from real Display entities: a player-head `ItemDisplay` laid on the ground, a flattened `BlockDisplay` blood pool, a glowing `TextDisplay` name tag, and an `Interaction` hitbox players right-click to report. |

Plus the connective tissue you'd expect: a lobby, countdown, ghosts (dead players
keep doing tasks but can't vote or be seen by the living), kill cooldown HUD,
win/lose conditions, and full clean-up between rounds.

---

## Requirements

- **Paper 1.21.4** (or a 1.21.4 fork such as Purpur). Uses Paper/Adventure APIs.
- **Java 21** (required by Minecraft 1.21.4).

## Building

```bash
mvn package
```

The build pulls `io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT` from
`https://repo.papermc.io` (already configured in `pom.xml`), so the machine
running the build needs outbound access to that repository. The result is:

```
target/AmongUs-1.0.0.jar
```

Drop it into your server's `plugins/` folder and restart.

## Recommended setup

This is a **minigame plugin**: players' inventories are cleared when they join
the lobby and when a round starts. Run it on a **dedicated game world/server**.

---

## Setting up an arena

All locations live in `config.yml` and are configured in-game (requires
`amongus.admin`, default OP):

1. `/amongus admin setlobby` — stand where players should wait/return.
2. `/amongus admin setmeeting` — stand at the discussion area *(optional; falls back to the lobby)*.
3. `/amongus admin addspawn` — run once at each round-start position (players are spread across them).
4. Look directly at a block and `/amongus admin addtask calibrate` (or `download`) — repeat to place task stations.
5. `/amongus admin info` — verify everything is set.

## Playing

1. Players run `/amongus join` to enter the lobby.
2. Once `min-players` have joined, anyone runs `/amongus start`.
3. **Crewmates:** right-click your task-station blocks and complete the mini-game. Fill the task bar to 100 % to win.
4. **Impostors:** right-click (or hit) a nearby crewmate with the **Kill** knife while off cooldown. A corpse is left behind.
5. **Everyone alive:** right-click a body to **report** it, or right-click the **Emergency Meeting** bell. Both open the vote.
6. **Voting:** discuss, then click a head to vote — or click **Skip**. The player with the most votes is ejected.
7. **Ghosts:** dead players turn invisible to the living, can fly, keep doing tasks (which still count!), but can't vote, kill or report.

### Win conditions

- **Crew win** — every assigned task is completed, **or** every impostor is ejected/eliminated.
- **Impostors win** — the number of living impostors is greater than or equal to the living crew.

---

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/amongus join` | `amongus.play` | Join the lobby. |
| `/amongus leave` | `amongus.play` | Leave the game / stop spectating. |
| `/amongus start` | `amongus.play` | Start the round (validates the arena & player count). |
| `/amongus status` | `amongus.play` | Show game state and arena setup. |
| `/amongus stop` | `amongus.admin` | Force-stop the current round. |
| `/amongus admin setlobby` | `amongus.admin` | Set the lobby spawn to your position. |
| `/amongus admin setmeeting` | `amongus.admin` | Set the meeting point to your position. |
| `/amongus admin addspawn` | `amongus.admin` | Add a game spawn at your position. |
| `/amongus admin clearspawns` | `amongus.admin` | Remove all game spawns. |
| `/amongus admin addtask <calibrate\|download>` | `amongus.admin` | Add a task station at the block you're looking at. |
| `/amongus admin cleartasks` | `amongus.admin` | Remove all task stations. |
| `/amongus admin reload` | `amongus.admin` | Reload `config.yml`. |
| `/amongus admin info` | `amongus.admin` | Same as `status`. |

Aliases: `/au`, `/mm`, `/mafia`.

## Configuration (`config.yml`)

| Key | Default | Meaning |
|-----|---------|---------|
| `settings.min-players` | `4` | Minimum players to start. |
| `settings.impostor-count` | `1` | Impostors to assign (clamped to keep the crew in the majority). |
| `settings.start-countdown` | `10` | Seconds before the round begins. |
| `settings.kill-cooldown` | `30` | Seconds between kills (also reset after meetings). |
| `settings.kill-range` | `3.0` | Max kill distance in blocks. |
| `settings.tasks-per-player` | `4` | Tasks assigned to each crewmate (clamped to available stations). |
| `settings.emergency-meetings` | `1` | Emergency buttons per player per game. |
| `settings.discussion-seconds` | `20` | Free discussion time before voting opens. |
| `settings.voting-seconds` | `30` | Time allowed to vote. |
| `settings.confirm-ejects` | `true` | Announce whether an ejected player was an impostor. |
| `settings.end-screen-seconds` | `8` | How long the win screen shows before resetting. |
| `arena.*` | — | Managed by the in-game admin commands; editing by hand is not recommended. |

---

## Project layout

```
src/main/java/net/innerpine/amongus/
├── AmongUsPlugin.java        # entry point / wiring
├── command/                  # /amongus command + tab completion
├── config/GameSettings.java  # config + arena persistence
├── corpse/                   # Display-entity corpses
├── game/                     # Game state machine, GamePlayer
├── listener/                 # connection, protection, combat, interact, menu
├── meeting/MeetingManager.java # discussion + voting flow
├── menu/                     # VoteMenu & TaskMenu inventory GUIs
├── role/Role.java
├── task/                     # task types, stations, TaskManager + mini-games
└── util/                     # items, messages, sounds, keys
```

## Notes & possible extensions

- **Verified to compile against the Paper 1.21.4 API.** Display/Interaction,
  Adventure text/boss-bar, and inventory signatures were cross-checked against
  the official Paper API sources.
- Deliberately scoped to the four requested pillars. Natural next steps: sabotage
  events (lights/reactor), a separate ghost chat channel, vent travel, per-arena
  configs, and customisable task mini-games.
