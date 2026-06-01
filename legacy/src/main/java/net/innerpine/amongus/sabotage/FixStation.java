package net.innerpine.amongus.sabotage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FixStation {

    private final Location location;
    private final FixType type;

    public FixStation(Location location, FixType type) {
        this.location = location;
        this.type = type;
    }

    public Location location() {
        return location;
    }

    public FixType type() {
        return type;
    }

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

    public static FixStation deserialize(Map<?, ?> map) {
        Object worldName = map.get("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(String.valueOf(worldName));
        if (world == null) {
            return null;
        }
        FixType type = FixType.fromString(String.valueOf(map.get("type")));
        if (type == null) {
            return null;
        }
        int x = asInt(map.get("x"));
        int y = asInt(map.get("y"));
        int z = asInt(map.get("z"));
        return new FixStation(new Location(world, x + 0.5, y, z + 0.5), type);
    }

    private static int asInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
