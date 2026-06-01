package net.innerpine.amongus.sabotage;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.menu.SabotageMenu;
import net.innerpine.amongus.role.Role;
import net.innerpine.amongus.util.ActionBar;
import net.innerpine.amongus.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Impostor sabotages: a lights blackout and a reactor meltdown countdown. */
public final class SabotageManager {

    private static final double REACTOR_RANGE_SQ = 2.5 * 2.5;

    private final AmongUsPlugin plugin;
    private final GameSettings settings;
    private final Game game;

    private long teamCooldownUntil;

    private boolean lightsActive;
    private BukkitTask lightsTask;

    private boolean reactorActive;
    private BukkitTask reactorTask;
    private int reactorSecondsLeft;
    private final BossBar reactorBar = Bukkit.createBossBar("Reactor", BarColor.RED, BarStyle.SOLID);

    public SabotageManager(AmongUsPlugin plugin, GameSettings settings, Game game) {
        this.plugin = plugin;
        this.settings = settings;
        this.game = game;
    }

    public boolean isReactorActive() {
        return reactorActive;
    }

    public FixStation fixStationAt(Location location) {
        for (FixStation fix : settings.fixes()) {
            if (fix.isAt(location)) {
                return fix;
            }
        }
        return null;
    }

    public void openMenu(Player player) {
        if (!game.isState(GameState.RUNNING)) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || !gp.isImpostor() || gp.isGhost()) {
            ActionBar.send(player, ChatColor.RED + "Only living impostors can sabotage.");
            return;
        }
        int cooldown = cooldownRemaining();
        boolean ready = cooldownReady() && !reactorActive;
        boolean lightsReady = ready && !lightsActive && !settings.fixesOfType(FixType.LIGHTS).isEmpty();
        boolean reactorReady = ready && settings.fixesOfType(FixType.REACTOR).size() >= 2;
        player.openInventory(new SabotageMenu(lightsReady, reactorReady, cooldown).getInventory());
    }

    public void trigger(Player player, FixType type) {
        if (!game.isState(GameState.RUNNING)) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || !gp.isImpostor() || gp.isGhost()) {
            return;
        }
        player.closeInventory();
        if (reactorActive) {
            return;
        }
        if (!cooldownReady()) {
            ActionBar.send(player, ChatColor.RED + "Sabotage on cooldown: " + cooldownRemaining() + "s");
            return;
        }
        if (type == FixType.LIGHTS) {
            if (lightsActive || settings.fixesOfType(FixType.LIGHTS).isEmpty()) {
                return;
            }
            triggerLights();
            startCooldown();
        } else if (type == FixType.REACTOR) {
            if (settings.fixesOfType(FixType.REACTOR).size() < 2) {
                ActionBar.send(player, ChatColor.RED + "Need at least two reactor panels.");
                return;
            }
            triggerReactor();
            startCooldown();
        }
    }

    private boolean cooldownReady() {
        return System.currentTimeMillis() >= teamCooldownUntil;
    }

    private int cooldownRemaining() {
        long remaining = teamCooldownUntil - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
    }

    private void startCooldown() {
        teamCooldownUntil = System.currentTimeMillis() + settings.sabotageCooldown() * 1000L;
    }

    private void triggerLights() {
        lightsActive = true;
        game.broadcast(Msg.error("⚠ The lights have been sabotaged! Find a light panel to fix them."));
        game.broadcastSound(Sound.BLOCK_FIRE_EXTINGUISH, 0.8f);
        lightsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : game.aliveOnline()) {
                GamePlayer gp = game.player(player.getUniqueId());
                if (gp != null && !gp.isImpostor()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                }
            }
        }, 0L, 40L);
    }

    public void fixLights(Player player) {
        if (!lightsActive) {
            ActionBar.send(player, ChatColor.GRAY + "Nothing to fix here.");
            return;
        }
        clearLights();
        game.broadcast(Msg.success(player.getName() + " restored the lights!"));
        game.broadcastSound(Sound.BLOCK_BEACON_ACTIVATE, 1.2f);
    }

    private void clearLights() {
        lightsActive = false;
        if (lightsTask != null) {
            lightsTask.cancel();
            lightsTask = null;
        }
        for (Player player : game.onlineParticipants()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    private void triggerReactor() {
        reactorActive = true;
        reactorSecondsLeft = settings.reactorSeconds();
        for (Player player : game.onlineParticipants()) {
            reactorBar.addPlayer(player);
        }
        game.broadcast(Msg.error("⚠ REACTOR MELTDOWN! Two crew must cover the reactor panels!"));
        game.broadcastSound(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f);
        reactorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!game.isState(GameState.RUNNING)) {
                stopReactor();
                return;
            }
            if (reactorSecondsLeft <= 0) {
                stopReactor();
                game.end(Role.IMPOSTOR, "The reactor melted down!");
                return;
            }
            double progress = Math.max(0.0, Math.min(1.0, (double) reactorSecondsLeft / settings.reactorSeconds()));
            reactorBar.setProgress(progress);
            reactorBar.setTitle(ChatColor.RED + "⚠ Reactor meltdown — " + reactorSecondsLeft + "s");
            if (reactorCovered()) {
                stopReactor();
                game.broadcast(Msg.success("The reactor has been stabilised!"));
                game.broadcastSound(Sound.BLOCK_BEACON_ACTIVATE, 1.2f);
                return;
            }
            reactorSecondsLeft--;
        }, 0L, 20L);
    }

    private boolean reactorCovered() {
        List<FixStation> panels = settings.fixesOfType(FixType.REACTOR);
        int required = Math.min(2, panels.size());
        Set<UUID> used = new HashSet<>();
        int covered = 0;
        for (FixStation panel : panels) {
            for (Player player : game.aliveOnline()) {
                if (used.contains(player.getUniqueId())) {
                    continue;
                }
                Location panelLoc = panel.location();
                if (player.getWorld().equals(panelLoc.getWorld())
                        && player.getLocation().distanceSquared(panelLoc) <= REACTOR_RANGE_SQ) {
                    used.add(player.getUniqueId());
                    covered++;
                    break;
                }
            }
        }
        return covered >= required;
    }

    private void stopReactor() {
        reactorActive = false;
        if (reactorTask != null) {
            reactorTask.cancel();
            reactorTask = null;
        }
        reactorBar.removeAll();
    }

    public void clearAll() {
        clearLights();
        stopReactor();
    }

    public void reset() {
        clearAll();
        teamCooldownUntil = 0L;
    }
}
