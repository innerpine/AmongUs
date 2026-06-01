package net.innerpine.amongus.game;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.corpse.CorpseManager;
import net.innerpine.amongus.meeting.MeetingManager;
import net.innerpine.amongus.role.Role;
import net.innerpine.amongus.sabotage.SabotageManager;
import net.innerpine.amongus.task.TaskManager;
import net.innerpine.amongus.util.ItemFactory;
import net.innerpine.amongus.util.Messages;
import net.innerpine.amongus.util.SoundUtil;
import net.innerpine.amongus.vent.VentManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Central game controller: owns the {@link GameState} machine, the participant
 * roster and the round lifecycle (start, kills, meetings, win checks, reset).
 */
public final class Game {

    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float DEFAULT_FLY_SPEED = 0.1f;

    private final AmongUsPlugin plugin;
    private final GameSettings settings;
    private final TaskManager tasks;
    private final CorpseManager corpses;
    private MeetingManager meetings;
    private SabotageManager sabotage;
    private VentManager vent;

    private final Map<UUID, GamePlayer> participants = new LinkedHashMap<>();
    private final Set<UUID> vented = new HashSet<>();
    private GameState state = GameState.WAITING;

    private BukkitTask countdownTask;
    private BukkitTask cooldownTicker;

    public Game(AmongUsPlugin plugin, GameSettings settings, TaskManager tasks, CorpseManager corpses) {
        this.plugin = plugin;
        this.settings = settings;
        this.tasks = tasks;
        this.corpses = corpses;
    }

    public void setMeetingManager(MeetingManager meetings) {
        this.meetings = meetings;
    }

    public void setSabotageManager(SabotageManager sabotage) {
        this.sabotage = sabotage;
    }

    public void setVentManager(VentManager vent) {
        this.vent = vent;
    }

    // --- accessors ----------------------------------------------------------

    public AmongUsPlugin plugin() {
        return plugin;
    }

    public GameSettings settings() {
        return settings;
    }

    public TaskManager tasks() {
        return tasks;
    }

    public CorpseManager corpses() {
        return corpses;
    }

    public MeetingManager meetings() {
        return meetings;
    }

    public SabotageManager sabotage() {
        return sabotage;
    }

    public VentManager vent() {
        return vent;
    }

    public GameState state() {
        return state;
    }

    public boolean isState(GameState other) {
        return state == other;
    }

    public boolean isParticipant(UUID uuid) {
        return participants.containsKey(uuid);
    }

    public GamePlayer player(UUID uuid) {
        return participants.get(uuid);
    }

    public Collection<GamePlayer> players() {
        return participants.values();
    }

    public List<Player> onlineParticipants() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : participants.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    public List<Player> aliveOnline() {
        List<Player> list = new ArrayList<>();
        for (Player player : onlineParticipants()) {
            GamePlayer gp = participants.get(player.getUniqueId());
            if (gp != null && gp.isAlive()) {
                list.add(player);
            }
        }
        return list;
    }

    // --- broadcasting -------------------------------------------------------

    public void broadcast(Component message) {
        for (Player player : onlineParticipants()) {
            player.sendMessage(message);
        }
    }

    public void broadcastTitle(Component title, Component subtitle, int stayTicks) {
        Title.Times times = Title.Times.times(
                Duration.ofMillis(200), Duration.ofMillis(stayTicks * 50L), Duration.ofMillis(400));
        Title shown = Title.title(title, subtitle, times);
        for (Player player : onlineParticipants()) {
            player.showTitle(shown);
        }
    }

    public void broadcastSound(String key, float pitch) {
        for (Player player : onlineParticipants()) {
            SoundUtil.play(player, key, pitch);
        }
    }

    // --- lobby --------------------------------------------------------------

    public void addToLobby(Player player) {
        UUID uuid = player.getUniqueId();
        if (state != GameState.WAITING) {
            player.sendMessage(Messages.info("A round is in progress — you are now spectating."));
            player.setGameMode(GameMode.SPECTATOR);
            Location target = settings.meeting();
            if (target != null) {
                player.teleport(target);
            }
            return;
        }
        if (participants.containsKey(uuid)) {
            player.sendMessage(Messages.error("You are already in the lobby."));
            return;
        }
        participants.put(uuid, new GamePlayer(uuid));
        restorePlayer(player);
        if (settings.lobby() != null) {
            player.teleport(settings.lobby());
        }
        showToEveryone(player);
        broadcast(Messages.success(player.getName() + " joined ("
                + onlineParticipants().size() + "/" + settings.minPlayers() + ")."));
        SoundUtil.play(player, "entity.experience_orb.pickup");
    }

    public void leave(Player player) {
        UUID uuid = player.getUniqueId();
        if (!participants.containsKey(uuid)) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                restorePlayer(player);
                if (settings.lobby() != null) {
                    player.teleport(settings.lobby());
                }
                player.sendMessage(Messages.info("You stopped spectating."));
            } else {
                player.sendMessage(Messages.error("You are not in a game."));
            }
            return;
        }
        removeParticipant(player, true);
        if (settings.lobby() != null) {
            player.teleport(settings.lobby());
        }
        player.sendMessage(Messages.info("You left the game."));
    }

    /** Handles a player disconnecting (called from the connection listener). */
    public void handleQuit(Player player) {
        if (participants.containsKey(player.getUniqueId())) {
            removeParticipant(player, false);
        }
    }

    private void removeParticipant(Player player, boolean restore) {
        GamePlayer removed = participants.remove(player.getUniqueId());
        if (removed == null) {
            return;
        }
        if (restore) {
            restorePlayer(player);
        }
        showToEveryone(player);
        refreshVisibility();
        if (state == GameState.COUNTDOWN || state == GameState.RUNNING || state == GameState.MEETING) {
            broadcast(Messages.info(player.getName() + " left the game."));
            checkWin();
        }
    }

    // --- starting a round ---------------------------------------------------

    public boolean start(CommandSender initiator) {
        if (state != GameState.WAITING) {
            initiator.sendMessage(Messages.error("A round is already in progress."));
            return false;
        }
        List<String> problems = settings.validateArena();
        if (!problems.isEmpty()) {
            initiator.sendMessage(Messages.error("Cannot start — the arena is not set up. Missing:"));
            for (String problem : problems) {
                initiator.sendMessage(Component.text("  • " + problem, NamedTextColor.RED));
            }
            return false;
        }
        participants.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        List<Player> online = onlineParticipants();
        if (online.size() < settings.minPlayers()) {
            initiator.sendMessage(Messages.error("Not enough players: "
                    + online.size() + "/" + settings.minPlayers() + "."));
            return false;
        }

        assignRoles(online);
        tasks.assignTasks();

        teleportToSpawns();
        freezeAll(true);
        for (Player player : online) {
            player.setGameMode(GameMode.ADVENTURE);
            heal(player);
            giveLoadout(player);
            showRole(player);
        }
        refreshVisibility();

        state = GameState.COUNTDOWN;
        startCountdown();
        return true;
    }

    private void assignRoles(List<Player> online) {
        List<UUID> ids = new ArrayList<>();
        for (Player player : online) {
            ids.add(player.getUniqueId());
        }
        Collections.shuffle(ids);

        int maxImpostors = Math.max(1, (ids.size() - 1) / 2);
        int impostorCount = Math.min(settings.impostorCount(), maxImpostors);

        for (int i = 0; i < ids.size(); i++) {
            GamePlayer gp = participants.get(ids.get(i));
            if (gp == null) {
                continue;
            }
            gp.setAlive(true);
            gp.setRole(i < impostorCount ? Role.IMPOSTOR : Role.CREWMATE);
            gp.setEmergencyMeetingsLeft(settings.emergencyMeetings());
            gp.clearKillCooldown();
        }
    }

    private void showRole(Player player) {
        GamePlayer gp = participants.get(player.getUniqueId());
        if (gp == null) {
            return;
        }
        Role role = gp.role();
        Component title = Component.text(role.displayName(), role.color(), TextDecoration.BOLD);
        Component subtitle = role.isImpostor()
                ? Component.text("Eliminate the crew without being caught.", NamedTextColor.GRAY)
                : Component.text("Finish your tasks and find the impostors.", NamedTextColor.GRAY);
        player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500))));
    }

    private void startCountdown() {
        countdownTask = new BukkitRunnable() {
            int seconds = settings.startCountdown();

            @Override
            public void run() {
                if (state != GameState.COUNTDOWN) {
                    cancel();
                    return;
                }
                if (seconds <= 0) {
                    cancel();
                    beginRunning();
                    return;
                }
                if (seconds <= 5 || seconds % 5 == 0) {
                    broadcastTitle(Component.text(seconds, NamedTextColor.YELLOW, TextDecoration.BOLD),
                            Component.text("Get ready...", NamedTextColor.GRAY), 20);
                    broadcastSound("block.note_block.hat", 1.5f);
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void beginRunning() {
        state = GameState.RUNNING;
        freezeAll(false);
        for (GamePlayer gp : participants.values()) {
            if (gp.isImpostor()) {
                gp.startKillCooldown(settings.killCooldown());
            }
        }
        tasks.showBossBar();
        tasks.updateBossBar();
        startCooldownTicker();
        broadcast(Messages.success("The round has begun!"));
        broadcastTitle(Component.text("GO!", NamedTextColor.GREEN, TextDecoration.BOLD), Component.empty(), 20);
    }

    // --- kills & ghosts -----------------------------------------------------

    public boolean canKill(Player killer, Player victim) {
        if (state != GameState.RUNNING) {
            return false;
        }
        GamePlayer kg = participants.get(killer.getUniqueId());
        GamePlayer vg = participants.get(victim.getUniqueId());
        if (kg == null || vg == null) {
            return false;
        }
        if (!kg.isImpostor() || kg.isGhost() || vg.isGhost() || vg.isImpostor()) {
            return false;
        }
        if (!kg.canKill()) {
            return false;
        }
        return killer.getWorld().equals(victim.getWorld())
                && killer.getLocation().distanceSquared(victim.getLocation())
                <= settings.killRange() * settings.killRange();
    }

    public void kill(Player killer, Player victim) {
        if (!canKill(killer, victim)) {
            return;
        }
        GamePlayer kg = participants.get(killer.getUniqueId());
        corpses.spawn(victim);
        makeGhost(victim);
        kg.startKillCooldown(settings.killCooldown());

        victim.showTitle(Title.title(
                Component.text("You died!", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Keep doing tasks as a ghost.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(400))));
        SoundUtil.play(victim, "entity.player.hurt", 0.8f);
        SoundUtil.play(killer, "entity.player.attack.crit", 1.2f);
        checkWin();
    }

    /** Turns a player into a ghost (no corpse is spawned here). */
    public void makeGhost(Player player) {
        GamePlayer gp = participants.get(player.getUniqueId());
        if (gp == null) {
            return;
        }
        if (vented.remove(player.getUniqueId())) {
            player.closeInventory();
        }
        gp.setAlive(false);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);
        player.getInventory().clear();
        player.setLevel(0);
        player.setExp(0f);
        giveLoadout(player);
        refreshVisibility();
        player.sendMessage(Messages.error("You are dead. Finish your tasks to help the crew — "
                + "but the living can't see or hear you."));
    }

    // --- meetings (called by MeetingManager) --------------------------------

    public void teleportToMeeting() {
        Location target = settings.meeting();
        if (target == null) {
            return;
        }
        for (Player player : onlineParticipants()) {
            player.teleport(target);
        }
    }

    /** Transitions into a meeting: clears bodies, gathers and freezes everyone. */
    public void beginMeeting() {
        if (state != GameState.RUNNING) {
            return;
        }
        state = GameState.MEETING;
        if (sabotage != null) {
            sabotage.clearAll();
        }
        clearAllVents();
        corpses.removeAll();
        teleportToMeeting();
        freezeAll(true);
    }

    public void eject(UUID uuid) {
        GamePlayer gp = participants.get(uuid);
        if (gp == null) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        String name = player != null ? player.getName() : "A player";
        boolean wasImpostor = gp.isImpostor();
        if (player != null) {
            makeGhost(player);
        } else {
            gp.setAlive(false);
        }
        broadcast(Messages.accent(name + " was ejected."));
        if (settings.confirmEjects()) {
            broadcast(wasImpostor
                    ? Messages.error(name + " was an Impostor.")
                    : Messages.info(name + " was not an Impostor."));
        }
        refreshVisibility();
    }

    /** Called by the meeting manager once voting resolves. */
    public void finishMeeting(UUID ejected) {
        if (ejected != null) {
            eject(ejected);
        } else {
            broadcast(Messages.info("No one was ejected."));
        }
        checkWin();
        if (state == GameState.ENDING) {
            return;
        }
        resumeAfterMeeting();
    }

    private void resumeAfterMeeting() {
        state = GameState.RUNNING;
        freezeAll(false);
        teleportToSpawns();
        for (GamePlayer gp : participants.values()) {
            if (gp.isImpostor() && gp.isAlive()) {
                gp.startKillCooldown(settings.killCooldown());
            }
        }
        for (Player player : onlineParticipants()) {
            giveLoadout(player);
        }
        tasks.updateBossBar();
        broadcast(Messages.info("Back to it — good luck!"));
    }

    // --- win conditions -----------------------------------------------------

    public void checkWin() {
        if (state != GameState.RUNNING && state != GameState.MEETING) {
            return;
        }
        int impostors = 0;
        int crew = 0;
        for (GamePlayer gp : participants.values()) {
            if (!gp.isAlive()) {
                continue;
            }
            if (gp.isImpostor()) {
                impostors++;
            } else {
                crew++;
            }
        }
        if (impostors == 0) {
            end(Role.CREWMATE, "All impostors are gone!");
        } else if (impostors >= crew) {
            end(Role.IMPOSTOR, "The impostors overwhelmed the crew!");
        } else if (tasks.allComplete()) {
            end(Role.CREWMATE, "The crew finished every task!");
        }
    }

    public void end(Role winner, String reason) {
        if (state == GameState.ENDING || state == GameState.WAITING) {
            return;
        }
        state = GameState.ENDING;
        cancelTasks();
        if (meetings != null) {
            meetings.cancel();
        }
        if (sabotage != null) {
            sabotage.reset();
        }
        clearAllVents();

        Component title = winner.isImpostor()
                ? Component.text("Impostors Win", NamedTextColor.RED, TextDecoration.BOLD)
                : Component.text("Crewmates Win", NamedTextColor.AQUA, TextDecoration.BOLD);
        broadcastTitle(title, Component.text(reason, NamedTextColor.GRAY), settings.endScreenSeconds() * 20);
        broadcastSound(winner.isImpostor() ? "entity.wither.spawn" : "ui.toast.challenge_complete", 1.0f);

        List<String> impostorNames = new ArrayList<>();
        for (GamePlayer gp : participants.values()) {
            if (gp.isImpostor()) {
                Player player = Bukkit.getPlayer(gp.uuid());
                impostorNames.add(player != null ? player.getName() : gp.uuid().toString());
            }
        }
        broadcast(Messages.accent("The impostor(s): " + String.join(", ", impostorNames)));

        Bukkit.getScheduler().runTaskLater(plugin, this::resetToLobby, settings.endScreenSeconds() * 20L);
    }

    /** Force-stops the round immediately (admin command). */
    public boolean stop() {
        if (state == GameState.WAITING) {
            return false;
        }
        broadcast(Messages.error("The round was stopped by an admin."));
        resetToLobby();
        return true;
    }

    private void resetToLobby() {
        cancelTasks();
        if (meetings != null) {
            meetings.cancel();
        }
        corpses.removeAll();
        tasks.reset();
        if (sabotage != null) {
            sabotage.reset();
        }
        clearAllVents();

        for (Player player : onlineParticipants()) {
            participants.put(player.getUniqueId(), new GamePlayer(player.getUniqueId()));
            restorePlayer(player);
            if (settings.lobby() != null) {
                player.teleport(settings.lobby());
            }
        }
        showEveryone();
        state = GameState.WAITING;
        broadcast(Messages.info("Returned to the lobby. Use /amongus start to play again."));
    }

    private void cancelTasks() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (cooldownTicker != null) {
            cooldownTicker.cancel();
            cooldownTicker = null;
        }
    }

    // --- player state helpers ----------------------------------------------

    public void giveLoadout(Player player) {
        GamePlayer gp = participants.get(player.getUniqueId());
        PlayerInventory inv = player.getInventory();
        inv.clear();
        if (gp == null || gp.isGhost()) {
            return;
        }
        if (gp.isImpostor()) {
            inv.setItem(0, ItemFactory.killKnife());
            inv.setItem(1, ItemFactory.sabotageItem());
        }
        if (settings.emergencyMeetings() > 0) {
            inv.setItem(4, ItemFactory.emergencyButton());
        }
    }

    private void teleportToSpawns() {
        List<Location> spawns = settings.spawns();
        if (spawns.isEmpty()) {
            return;
        }
        int index = 0;
        for (Player player : onlineParticipants()) {
            player.teleport(spawns.get(index % spawns.size()));
            index++;
        }
    }

    public void freezeAll(boolean frozen) {
        for (Player player : onlineParticipants()) {
            player.setWalkSpeed(frozen ? 0f : DEFAULT_WALK_SPEED);
            player.setFlySpeed(frozen ? 0f : DEFAULT_FLY_SPEED);
        }
    }

    private void heal(Player player) {
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void restorePlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCollidable(true);
        player.setWalkSpeed(DEFAULT_WALK_SPEED);
        player.setFlySpeed(DEFAULT_FLY_SPEED);
        player.setLevel(0);
        player.setExp(0f);
        player.getInventory().clear();
        heal(player);
    }

    private void startCooldownTicker() {
        cooldownTicker = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int cooldown = Math.max(1, settings.killCooldown());
            for (GamePlayer gp : participants.values()) {
                if (!gp.isImpostor() || gp.isGhost()) {
                    continue;
                }
                Player player = Bukkit.getPlayer(gp.uuid());
                if (player == null) {
                    continue;
                }
                int remaining = gp.killCooldownRemaining();
                if (remaining > 0) {
                    player.setLevel(remaining);
                    float progress = 1f - Math.min(1f, (float) remaining / cooldown);
                    player.setExp(Math.max(0f, Math.min(1f, progress)));
                } else {
                    player.setLevel(0);
                    player.setExp(1f);
                }
            }
        }, 20L, 20L);
    }

    // --- visibility ---------------------------------------------------------

    /** Hides ghosts from living participants; everyone else sees everyone. */
    public void refreshVisibility() {
        List<Player> online = onlineParticipants();
        for (Player viewer : online) {
            GamePlayer viewerGp = participants.get(viewer.getUniqueId());
            boolean viewerAlive = viewerGp != null && viewerGp.isAlive();
            for (Player target : online) {
                if (viewer.equals(target)) {
                    continue;
                }
                GamePlayer targetGp = participants.get(target.getUniqueId());
                boolean targetGhost = targetGp != null && targetGp.isGhost();
                boolean targetVented = vented.contains(target.getUniqueId());
                if (targetVented || (targetGhost && viewerAlive)) {
                    viewer.hidePlayer(plugin, target);
                } else {
                    viewer.showPlayer(plugin, target);
                }
            }
        }
    }

    // --- vents --------------------------------------------------------------

    public boolean isVented(UUID uuid) {
        return vented.contains(uuid);
    }

    public void markVented(UUID uuid) {
        vented.add(uuid);
        refreshVisibility();
    }

    public void unmarkVented(UUID uuid) {
        vented.remove(uuid);
        refreshVisibility();
    }

    private void clearAllVents() {
        if (vented.isEmpty()) {
            return;
        }
        for (UUID id : new ArrayList<>(vented)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.closeInventory();
            }
        }
        vented.clear();
        refreshVisibility();
    }

    private void showToEveryone(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            other.showPlayer(plugin, player);
            player.showPlayer(plugin, other);
        }
    }

    private void showEveryone() {
        for (Player a : Bukkit.getOnlinePlayers()) {
            for (Player b : Bukkit.getOnlinePlayers()) {
                if (!a.equals(b)) {
                    a.showPlayer(plugin, b);
                }
            }
        }
    }
}
