package net.innerpine.amongus.util;

import net.innerpine.amongus.AmongUsPlugin;
import org.bukkit.NamespacedKey;

/**
 * Central registry of {@link NamespacedKey}s used to tag items and entities
 * with persistent data so we can recognise them later.
 */
public final class Keys {

    /** Marks a gameplay item (kill knife, emergency button, ...). */
    public static NamespacedKey ITEM_TYPE;

    private Keys() {
    }

    public static void init(AmongUsPlugin plugin) {
        ITEM_TYPE = new NamespacedKey(plugin, "item_type");
    }
}
