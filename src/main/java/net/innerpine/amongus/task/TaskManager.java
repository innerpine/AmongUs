package net.innerpine.amongus.task;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.menu.TaskMenu;
import net.innerpine.amongus.util.Messages;
import net.innerpine.amongus.util.SoundUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Assigns tasks to crewmates, runs the per-task mini-games and tracks overall
 * task completion (shown on a boss bar and used as a crew win condition).
 */
public final class TaskManager {

    private final AmongUsPlugin plugin;
    private final GameSettings settings;
    private Game game;

    private final BossBar taskBar = BossBar.bossBar(
            Component.text("Tasks", NamedTextColor.GREEN), 0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private final Map<UUID, BukkitTask> activeDownloads = new HashMap<>();

    public TaskManager(AmongUsPlugin plugin, GameSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    /** Gives each crewmate a random subset of the configured stations. */
    public void assignTasks() {
        List<TaskStation> stations = settings.tasks();
        int perPlayer = Math.min(settings.tasksPerPlayer(), stations.size());
        for (GamePlayer gp : game.players()) {
            if (gp.isImpostor()) {
                gp.assignTasks(Collections.emptyList());
                continue;
            }
            List<Integer> ids = new ArrayList<>();
            for (TaskStation station : stations) {
                ids.add(station.id());
            }
            Collections.shuffle(ids);
            gp.assignTasks(ids.subList(0, perPlayer));
        }
    }

    public TaskStation stationAt(org.bukkit.Location location) {
        for (TaskStation station : settings.tasks()) {
            if (station.isAt(location)) {
                return station;
            }
        }
        return null;
    }

    /** Called when a participant right-clicks a task station block. */
    public void onInteract(Player player, TaskStation station) {
        if (!game.isState(GameState.RUNNING)) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null) {
            return;
        }
        if (gp.isImpostor()) {
            player.sendActionBar(Component.text("Impostors don't have real tasks…", NamedTextColor.RED));
            return;
        }
        if (!gp.hasTask(station.id())) {
            player.sendActionBar(Component.text("This isn't one of your tasks.", NamedTextColor.GRAY));
            return;
        }
        if (gp.isTaskDone(station.id())) {
            player.sendActionBar(Component.text("You already finished this task.", NamedTextColor.GREEN));
            return;
        }
        switch (station.type()) {
            case CALIBRATE -> player.openInventory(new TaskMenu(station.id(), station.type().title()).getInventory());
            case DOWNLOAD -> startDownload(player, station);
        }
    }

    private void startDownload(Player player, TaskStation station) {
        UUID uuid = player.getUniqueId();
        if (activeDownloads.containsKey(uuid)) {
            return;
        }
        int stationId = station.id();
        String label = station.type().title();
        BukkitTask task = new BukkitRunnable() {
            int step = 0;
            final int steps = 20;

            @Override
            public void run() {
                Player online = Bukkit.getPlayer(uuid);
                GamePlayer gp = game.player(uuid);
                if (online == null || gp == null || !game.isState(GameState.RUNNING) || gp.isTaskDone(stationId)) {
                    cancel();
                    activeDownloads.remove(uuid);
                    return;
                }
                step++;
                online.sendActionBar(progressBar(label, (float) step / steps));
                if (step >= steps) {
                    cancel();
                    activeDownloads.remove(uuid);
                    completeTask(online, stationId);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
        activeDownloads.put(uuid, task);
    }

    private Component progressBar(String label, float fraction) {
        int total = 20;
        int filled = Math.max(0, Math.min(total, Math.round(fraction * total)));
        int percent = Math.round(fraction * 100);
        return Component.text(label + " ", NamedTextColor.GRAY)
                .append(Component.text("|".repeat(filled), NamedTextColor.GREEN))
                .append(Component.text("|".repeat(total - filled), NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + percent + "%", NamedTextColor.YELLOW));
    }

    /** Marks a station complete for the player and fires the relevant updates. */
    public void completeTask(Player player, int stationId) {
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || !gp.completeTask(stationId)) {
            return;
        }
        player.sendMessage(Messages.success("Task complete! ("
                + gp.completedTaskCount() + "/" + gp.assignedTaskCount() + ")"));
        SoundUtil.play(player, "entity.experience_orb.pickup", 1.4f);
        updateBossBar();
        game.checkWin();
    }

    // --- progress -----------------------------------------------------------

    public float progress() {
        int total = 0;
        int done = 0;
        for (GamePlayer gp : game.players()) {
            if (gp.isImpostor()) {
                continue;
            }
            total += gp.assignedTaskCount();
            done += gp.completedTaskCount();
        }
        return total == 0 ? 0f : (float) done / total;
    }

    public boolean allComplete() {
        int total = 0;
        int done = 0;
        for (GamePlayer gp : game.players()) {
            if (gp.isImpostor()) {
                continue;
            }
            total += gp.assignedTaskCount();
            done += gp.completedTaskCount();
        }
        return total > 0 && done >= total;
    }

    // --- boss bar -----------------------------------------------------------

    public void showBossBar() {
        for (Player player : game.onlineParticipants()) {
            player.showBossBar(taskBar);
        }
    }

    public void hideBossBar() {
        for (Player player : game.onlineParticipants()) {
            player.hideBossBar(taskBar);
        }
    }

    public void updateBossBar() {
        float progress = progress();
        taskBar.progress(Math.max(0f, Math.min(1f, progress)));
        taskBar.name(Component.text("Tasks: " + Math.round(progress * 100) + "%", NamedTextColor.GREEN));
    }

    /** Cancels any running mini-games and clears the boss bar. */
    public void reset() {
        for (BukkitTask task : activeDownloads.values()) {
            task.cancel();
        }
        activeDownloads.clear();
        hideBossBar();
    }
}
