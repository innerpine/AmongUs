package net.innerpine.amongus.task;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.menu.TaskMenu;
import net.innerpine.amongus.util.ActionBar;
import net.innerpine.amongus.util.Msg;
import net.innerpine.amongus.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Assigns tasks, runs the mini-games and tracks overall task progress. */
public final class TaskManager {

    private final AmongUsPlugin plugin;
    private final GameSettings settings;
    private Game game;

    private final BossBar taskBar = Bukkit.createBossBar("Tasks", BarColor.GREEN, BarStyle.SOLID);
    private final Map<UUID, BukkitTask> activeDownloads = new HashMap<>();

    public TaskManager(AmongUsPlugin plugin, GameSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void setGame(Game game) {
        this.game = game;
    }

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

    public TaskStation stationAt(Location location) {
        for (TaskStation station : settings.tasks()) {
            if (station.isAt(location)) {
                return station;
            }
        }
        return null;
    }

    public void onInteract(Player player, TaskStation station) {
        if (!game.isState(GameState.RUNNING)) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null) {
            return;
        }
        if (gp.isImpostor()) {
            ActionBar.send(player, ChatColor.RED + "Impostors don't have real tasks…");
            return;
        }
        if (!gp.hasTask(station.id())) {
            ActionBar.send(player, ChatColor.GRAY + "This isn't one of your tasks.");
            return;
        }
        if (gp.isTaskDone(station.id())) {
            ActionBar.send(player, ChatColor.GREEN + "You already finished this task.");
            return;
        }
        switch (station.type()) {
            case CALIBRATE:
                player.openInventory(new TaskMenu(station.id(), station.type().title()).getInventory());
                break;
            case DOWNLOAD:
                startDownload(player, station);
                break;
            default:
                break;
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
                ActionBar.send(online, progressBar(label, (float) step / steps));
                if (step >= steps) {
                    cancel();
                    activeDownloads.remove(uuid);
                    completeTask(online, stationId);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
        activeDownloads.put(uuid, task);
    }

    private String progressBar(String label, float fraction) {
        int total = 20;
        int filled = Math.max(0, Math.min(total, Math.round(fraction * total)));
        int percent = Math.round(fraction * 100);
        return ChatColor.GRAY + label + " "
                + ChatColor.GREEN + repeat(filled)
                + ChatColor.DARK_GRAY + repeat(total - filled)
                + ChatColor.YELLOW + " " + percent + "%";
    }

    private String repeat(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('|');
        }
        return sb.toString();
    }

    public void completeTask(Player player, int stationId) {
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || !gp.completeTask(stationId)) {
            return;
        }
        player.sendMessage(Msg.success("Task complete! ("
                + gp.completedTaskCount() + "/" + gp.assignedTaskCount() + ")"));
        SoundUtil.play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.4f);
        updateBossBar();
        game.checkWin();
    }

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

    public void showBossBar() {
        for (Player player : game.onlineParticipants()) {
            taskBar.addPlayer(player);
        }
    }

    public void hideBossBar() {
        taskBar.removeAll();
    }

    public void updateBossBar() {
        float progress = progress();
        taskBar.setProgress(Math.max(0f, Math.min(1f, progress)));
        taskBar.setTitle(ChatColor.GREEN + "Tasks: " + Math.round(progress * 100) + "%");
    }

    public void reset() {
        for (BukkitTask task : activeDownloads.values()) {
            task.cancel();
        }
        activeDownloads.clear();
        hideBossBar();
    }
}
