package net.innerpine.amongus.corpse;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.UUID;

/** A dead body represented by an (invisible) armor stand wearing the player's head. */
public final class Corpse {

    private final UUID deadId;
    private final String deadName;
    private final Location location;
    private final ArmorStand stand;

    public Corpse(UUID deadId, String deadName, Location location, ArmorStand stand) {
        this.deadId = deadId;
        this.deadName = deadName;
        this.location = location;
        this.stand = stand;
    }

    public UUID deadId() {
        return deadId;
    }

    public String deadName() {
        return deadName;
    }

    public Location location() {
        return location;
    }

    public UUID standId() {
        return stand.getUniqueId();
    }

    public void remove() {
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }
}
