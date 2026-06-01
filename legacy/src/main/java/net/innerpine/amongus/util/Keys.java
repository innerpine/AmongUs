package net.innerpine.amongus.util;

import net.innerpine.amongus.AmongUsPlugin;
import org.bukkit.NamespacedKey;

/** Namespaced keys for tagging gameplay items. */
public final class Keys {

    public static NamespacedKey ITEM_TYPE;

    private Keys() {
    }

    public static void init(AmongUsPlugin plugin) {
        ITEM_TYPE = new NamespacedKey(plugin, "item_type");
    }
}
