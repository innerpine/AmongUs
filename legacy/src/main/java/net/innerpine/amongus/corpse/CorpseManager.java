package net.innerpine.amongus.corpse;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.util.ItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Spawns and tracks armor-stand corpses. */
public final class CorpseManager {

    private final AmongUsPlugin plugin;
    private final List<Corpse> corpses = new ArrayList<>();
    private final Map<UUID, Corpse> byStand = new HashMap<>();

    public CorpseManager(AmongUsPlugin plugin) {
        this.plugin = plugin;
    }

    public Corpse spawn(Player dead) {
        World world = dead.getWorld();
        Location loc = dead.getLocation();

        ArmorStand stand = world.spawn(loc, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setMarker(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setInvulnerable(true);
        stand.setCustomName(ChatColor.RED + "☠ " + dead.getName());
        stand.setCustomNameVisible(true);

        EntityEquipment equipment = stand.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(ItemFactory.head(dead, dead.getName()));
        }

        Corpse corpse = new Corpse(dead.getUniqueId(), dead.getName(), loc, stand);
        corpses.add(corpse);
        byStand.put(stand.getUniqueId(), corpse);
        return corpse;
    }

    public Corpse byStand(UUID standId) {
        return byStand.get(standId);
    }

    public int count() {
        return corpses.size();
    }

    public void removeAll() {
        for (Corpse corpse : corpses) {
            corpse.remove();
        }
        corpses.clear();
        byStand.clear();
    }
}
