package net.innerpine.amongus.task;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A fixed location in the world where a {@link TaskType} can be performed.
 * Identified by the block it sits on.
 */
public final class TaskStation {

    private final int id;
    private final Location location;
    private final TaskType type;

    public TaskStation(int id, Location location, TaskType type) {
        this.id = id;
        this.location = location;
        this.type = type;
    }

    public int id() {
        return id;
    }

    public Location location() {
        return location;
    }

    public TaskType type() {
        return type;
    }

    /** True if {@code other} refers to the same block as this station. */
    public boolean isAt(Location other) {
        return other != null
                && other.getWorld() != null
                && location.getWorld() != null
                && other.getWorld().equals(location.getWorld())
                && other.getBlockX() == location.getBlockX()
                && other.getBlockY() == location.getBlockY()
                && other.getBlockZ() == location.getBlockZ();
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", location.getWorld() == null ? null : location.getWorld().getName());
        map.put("x", location.getBlockX());
        map.put("y", location.getBlockY());
        map.put("z", location.getBlockZ());
        map.put("type", type.name());
        return map;
    }

    public static TaskStation deserialize(int id, Map<?, ?> map) {
        Object worldName = map.get("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(String.valueOf(worldName));
        if (world == null) {
            return null;
        }
        TaskType type = TaskType.fromString(String.valueOf(map.get("type")));
        if (type == null) {
            return null;
        }
        int x = asInt(map.get("x"));
        int y = asInt(map.get("y"));
        int z = asInt(map.get("z"));
        Location loc = new Location(world, x + 0.5, y, z + 0.5);
        return new TaskStation(id, loc, type);
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
