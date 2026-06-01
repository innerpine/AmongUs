package net.innerpine.amongus.menu;

import net.innerpine.amongus.sabotage.FixType;
import net.innerpine.amongus.util.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * The impostor-only sabotage picker.
 */
public final class SabotageMenu implements InventoryHolder {

    private static final int LIGHTS_SLOT = 2;
    private static final int REACTOR_SLOT = 6;

    private final Inventory inventory;

    public SabotageMenu(boolean lightsReady, boolean reactorReady, int cooldownRemaining) {
        this.inventory = Bukkit.createInventory(this, 9,
                Component.text("Sabotage", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        inventory.setItem(LIGHTS_SLOT, icon(Material.GLOWSTONE, "Sabotage Lights", lightsReady,
                cooldownRemaining, "Blinds the crew until someone fixes the lights."));
        inventory.setItem(REACTOR_SLOT, icon(Material.TNT, "Reactor Meltdown", reactorReady,
                cooldownRemaining, "The crew must cover two reactor panels or lose."));
    }

    public FixType fixTypeAt(int slot) {
        if (slot == LIGHTS_SLOT) {
            return FixType.LIGHTS;
        }
        if (slot == REACTOR_SLOT) {
            return FixType.REACTOR;
        }
        return null;
    }

    private ItemStack icon(Material material, String name, boolean ready, int cooldown, String description) {
        Component status;
        if (ready) {
            status = Component.text("Click to activate", NamedTextColor.GREEN);
        } else if (cooldown > 0) {
            status = Component.text("On cooldown: " + cooldown + "s", NamedTextColor.RED);
        } else {
            status = Component.text("Unavailable (no fix points set)", NamedTextColor.RED);
        }
        return ItemFactory.icon(material,
                Component.text(name, NamedTextColor.DARK_RED, TextDecoration.BOLD),
                Component.text(description, NamedTextColor.GRAY),
                status);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
