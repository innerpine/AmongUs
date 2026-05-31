package net.innerpine.amongus.config;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.task.TaskStation;
import net.innerpine.amongus.task.TaskType;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loads, exposes and persists everything from {@code config.yml}: numeric
 * tuning under {@code settings:} and the arena layout under {@code arena:}.
 */
public final class GameSettings {

    private final AmongUsPlugin plugin;

    private int minPlayers;
    private int impostorCount;
    private int startCountdown;
    private int killCooldown;
    private double killRange;
    private int tasksPerPlayer;
    private int emergencyMeetings;
    private int discussionSeconds;
    private int votingSeconds;
    private int endScreenSeconds;
    private boolean confirmEjects;

    private Location lobby;
    private Location meeting;
    private final List<Location> spawns = new ArrayList<>();
    private final List<TaskStation> tasks = new ArrayList<>();

    public GameSettings(AmongUsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();

        minPlayers = Math.max(2, config.getInt("settings.min-players", 4));
        impostorCount = Math.max(1, config.getInt("settings.impostor-count", 1));
        startCountdown = Math.max(0, config.getInt("settings.start-countdown", 10));
        killCooldown = Math.max(0, config.getInt("settings.kill-cooldown", 30));
        killRange = Math.max(1.0, config.getDouble("settings.kill-range", 3.0));
        tasksPerPlayer = Math.max(1, config.getInt("settings.tasks-per-player", 4));
        emergencyMeetings = Math.max(0, config.getInt("settings.emergency-meetings", 1));
        discussionSeconds = Math.max(0, config.getInt("settings.discussion-seconds", 20));
        votingSeconds = Math.max(5, config.getInt("settings.voting-seconds", 30));
        endScreenSeconds = Math.max(1, config.getInt("settings.end-screen-seconds", 8));
        confirmEjects = config.getBoolean("settings.confirm-ejects", true);

        lobby = config.getLocation("arena.lobby");
        meeting = config.getLocation("arena.meeting");

        spawns.clear();
        for (Object raw : config.getList("arena.spawns", Collections.emptyList())) {
            if (raw instanceof Location location) {
                spawns.add(location);
            }
        }

        tasks.clear();
        List<Map<?, ?>> rawTasks = config.getMapList("arena.tasks");
        for (int i = 0; i < rawTasks.size(); i++) {
            TaskStation station = TaskStation.deserialize(i, rawTasks.get(i));
            if (station != null) {
                tasks.add(station);
            }
        }
    }

    private void persist() {
        FileConfiguration config = plugin.getConfig();
        config.set("arena.lobby", lobby);
        config.set("arena.meeting", meeting);
        config.set("arena.spawns", new ArrayList<>(spawns));

        List<Map<String, Object>> serialized = new ArrayList<>();
        for (TaskStation station : tasks) {
            serialized.add(station.serialize());
        }
        config.set("arena.tasks", serialized);
        plugin.saveConfig();
    }

    // --- numeric settings ---------------------------------------------------

    public int minPlayers() {
        return minPlayers;
    }

    public int impostorCount() {
        return impostorCount;
    }

    public int startCountdown() {
        return startCountdown;
    }

    public int killCooldown() {
        return killCooldown;
    }

    public double killRange() {
        return killRange;
    }

    public int tasksPerPlayer() {
        return tasksPerPlayer;
    }

    public int emergencyMeetings() {
        return emergencyMeetings;
    }

    public int discussionSeconds() {
        return discussionSeconds;
    }

    public int votingSeconds() {
        return votingSeconds;
    }

    public int endScreenSeconds() {
        return endScreenSeconds;
    }

    public boolean confirmEjects() {
        return confirmEjects;
    }

    // --- arena --------------------------------------------------------------

    public Location lobby() {
        return lobby;
    }

    public Location meeting() {
        // Fall back to the lobby if no dedicated meeting point is set.
        return meeting != null ? meeting : lobby;
    }

    public List<Location> spawns() {
        return Collections.unmodifiableList(spawns);
    }

    public List<TaskStation> tasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void setLobby(Location location) {
        this.lobby = location;
        persist();
    }

    public void setMeeting(Location location) {
        this.meeting = location;
        persist();
    }

    public void addSpawn(Location location) {
        spawns.add(location);
        persist();
    }

    public void clearSpawns() {
        spawns.clear();
        persist();
    }

    public TaskStation addTask(Location location, TaskType type) {
        TaskStation station = new TaskStation(tasks.size(), location, type);
        tasks.add(station);
        persist();
        return station;
    }

    public void clearTasks() {
        tasks.clear();
        persist();
    }

    /** Returns a human readable list of everything still missing before a game can start. */
    public List<String> validateArena() {
        List<String> problems = new ArrayList<>();
        if (lobby == null) {
            problems.add("lobby spawn (/amongus admin setlobby)");
        }
        if (spawns.isEmpty()) {
            problems.add("at least one game spawn (/amongus admin addspawn)");
        }
        if (tasks.isEmpty()) {
            problems.add("at least one task station (/amongus admin addtask <type>)");
        }
        return problems;
    }
}
