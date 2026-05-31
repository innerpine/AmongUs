package net.innerpine.amongus.corpse;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.UUID;

/**
 * A dead body in the world, made up of several Display entities plus an
 * {@link org.bukkit.entity.Interaction} hitbox used for reporting.
 */
public final class Corpse {

    private final UUID deadId;
    private final String deadName;
    private final Location location;
    private final List<Entity> entities;
    private final UUID interactionId;

    public Corpse(UUID deadId, String deadName, Location location, List<Entity> entities, UUID interactionId) {
        this.deadId = deadId;
        this.deadName = deadName;
        this.location = location;
        this.entities = entities;
        this.interactionId = interactionId;
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

    public UUID interactionId() {
        return interactionId;
    }

    /** Despawns every entity that makes up this corpse. */
    public void remove() {
        for (Entity entity : entities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
    }
}
