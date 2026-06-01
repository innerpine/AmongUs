package net.innerpine.amongus.config;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.sabotage.FixStation;
import net.innerpine.amongus.sabotage.FixType;
import net.innerpine.amongus.task.TaskStation;
import net.innerpine.amongus.task.TaskType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads, exposes and persists everything from {@code config.yml}. */
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
    private int sabotageCooldown;
    private int reactorSeconds;

    private Location lobby;
    private Location meeting;
    private final List<Location> spawns = new ArrayList<>();
    private final List<TaskStation> tasks = new ArrayList<>();
    private final List<FixStation> fixes = new ArrayList<>();
    private final List<Location> vents = new ArrayList<>();

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
        sabotageCooldown = Math.max(0, config.getInt("settings.sabotage-cooldown", 25));
        reactorSeconds = Math.max(10, config.getInt("settings.reactor-seconds", 40));

        lobby = config.getLocation("arena.lobby");
        meeting = config.getLocation("arena.meeting");

        spawns.clear();
        for (Object raw : config.getList("arena.spawns", Collections.emptyList())) {
            if (raw instanceof Location) {
                spawns.add((Location) raw);
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

        fixes.clear();
        for (Map<?, ?> raw : config.getMapList("arena.fixes")) {
            FixStation fix = FixStation.deserialize(raw);
            if (fix != null) {
                fixes.add(fix);
            }
        }

        vents.clear();
        for (Map<?, ?> raw : config.getMapList("arena.vents")) {
            Location vent = ventFromMap(raw);
            if (vent != null) {
                vents.add(vent);
            }
        }
    }

    private static Location ventFromMap(Map<?, ?> map) {
        Object worldName = map.get("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(String.valueOf(worldName));
        if (world == null) {
            return null;
        }
        int x = map.get("x") instanceof Number ? ((Number) map.get("x")).intValue() : 0;
        int y = map.get("y") instanceof Number ? ((Number) map.get("y")).intValue() : 0;
        int z = map.get("z") instanceof Number ? ((Number) map.get("z")).intValue() : 0;
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private static Map<String, Object> ventToMap(Location location) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", location.getWorld() == null ? null : location.getWorld().getName());
        map.put("x", location.getBlockX());
        map.put("y", location.getBlockY());
        map.put("z", location.getBlockZ());
        return map;
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

        List<Map<String, Object>> serializedFixes = new ArrayList<>();
        for (FixStation fix : fixes) {
            serializedFixes.add(fix.serialize());
        }
        config.set("arena.fixes", serializedFixes);

        List<Map<String, Object>> serializedVents = new ArrayList<>();
        for (Location vent : vents) {
            serializedVents.add(ventToMap(vent));
        }
        config.set("arena.vents", serializedVents);
        plugin.saveConfig();
    }

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

    public int sabotageCooldown() {
        return sabotageCooldown;
    }

    public int reactorSeconds() {
        return reactorSeconds;
    }

    public Location lobby() {
        return lobby;
    }

    public Location meeting() {
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

    public List<FixStation> fixes() {
        return Collections.unmodifiableList(fixes);
    }

    public List<FixStation> fixesOfType(FixType type) {
        List<FixStation> result = new ArrayList<>();
        for (FixStation fix : fixes) {
            if (fix.type() == type) {
                result.add(fix);
            }
        }
        return result;
    }

    public FixStation addFix(Location location, FixType type) {
        FixStation fix = new FixStation(location, type);
        fixes.add(fix);
        persist();
        return fix;
    }

    public void clearFixes() {
        fixes.clear();
        persist();
    }

    public List<Location> vents() {
        return Collections.unmodifiableList(vents);
    }

    public void addVent(Location location) {
        vents.add(location);
        persist();
    }

    public void clearVents() {
        vents.clear();
        persist();
    }

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
