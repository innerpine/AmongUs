package net.innerpine.amongus.sabotage;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.menu.SabotageMenu;
import net.innerpine.amongus.role.Role;
import net.innerpine.amongus.util.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Impostor sabotages: a non-critical "lights" blackout fixed at a panel, and a
 * critical "reactor" meltdown the crew must cover two panels to stop before the
 * timer runs out (otherwise the impostors win).
 */
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
    private final BossBar reactorBar = BossBar.bossBar(
            Component.text("Reactor"), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);

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

    // --- opening the menu ---------------------------------------------------

    public void openMenu(Player player) {
        if (!game.isState(GameState.RUNNING)) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || !gp.isImpostor() || gp.isGhost()) {
            player.sendActionBar(Component.text("Only living impostors can sabotage.", NamedTextColor.RED));
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
            player.sendActionBar(Component.text("Sabotage on cooldown: " + cooldownRemaining() + "s",
                    NamedTextColor.RED));
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
                player.sendActionBar(Component.text("Need at least two reactor panels.", NamedTextColor.RED));
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

    // --- lights -------------------------------------------------------------

    private void triggerLights() {
        lightsActive = true;
        game.broadcast(Messages.error("⚠ The lights have been sabotaged! Find a light panel to fix them."));
        game.broadcastSound("block.fire.extinguish", 0.8f);
        lightsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : game.aliveOnline()) {
                GamePlayer gp = game.player(player.getUniqueId());
                if (gp != null && !gp.isImpostor()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                }
            }
        }, 0L, 40L);
    }

    public void fixLights(Player player) {
        if (!lightsActive) {
            player.sendActionBar(Component.text("Nothing to fix here.", NamedTextColor.GRAY));
            return;
        }
        clearLights();
        game.broadcast(Messages.success(player.getName() + " restored the lights!"));
        game.broadcastSound("block.beacon.activate", 1.2f);
    }

    private void clearLights() {
        lightsActive = false;
        if (lightsTask != null) {
            lightsTask.cancel();
            lightsTask = null;
        }
        for (Player player : game.onlineParticipants()) {
            player.removePotionEffect(PotionEffectType.DARKNESS);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    // --- reactor ------------------------------------------------------------

    private void triggerReactor() {
        reactorActive = true;
        reactorSecondsLeft = settings.reactorSeconds();
        for (Player player : game.onlineParticipants()) {
            player.showBossBar(reactorBar);
        }
        game.broadcast(Messages.error("⚠ REACTOR MELTDOWN! Two crew must cover the reactor panels!"));
        game.broadcastSound("block.respawn_anchor.deplete", 0.7f);
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
            float progress = Math.max(0f, Math.min(1f, (float) reactorSecondsLeft / settings.reactorSeconds()));
            reactorBar.progress(progress);
            reactorBar.name(Component.text("⚠ Reactor meltdown — " + reactorSecondsLeft + "s", NamedTextColor.RED));
            if (reactorCovered()) {
                stopReactor();
                game.broadcast(Messages.success("The reactor has been stabilised!"));
                game.broadcastSound("block.beacon.activate", 1.2f);
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
        for (Player player : game.onlineParticipants()) {
            player.hideBossBar(reactorBar);
        }
    }

    // --- lifecycle ----------------------------------------------------------

    /** Clears active sabotages (e.g. when a meeting begins). */
    public void clearAll() {
        clearLights();
        stopReactor();
    }

    /** Full reset between rounds. */
    public void reset() {
        clearAll();
        teamCooldownUntil = 0L;
    }
}
