package net.innerpine.amongus.menu;

import net.innerpine.amongus.sabotage.FixType;
import net.innerpine.amongus.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Impostor-only sabotage picker. */
public final class SabotageMenu implements InventoryHolder {

    private static final int LIGHTS_SLOT = 2;
    private static final int REACTOR_SLOT = 6;

    private final Inventory inventory;

    public SabotageMenu(boolean lightsReady, boolean reactorReady, int cooldownRemaining) {
        this.inventory = Bukkit.createInventory(this, 9, ChatColor.DARK_RED + "" + ChatColor.BOLD + "Sabotage");
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
        String status;
        if (ready) {
            status = ChatColor.GREEN + "Click to activate";
        } else if (cooldown > 0) {
            status = ChatColor.RED + "On cooldown: " + cooldown + "s";
        } else {
            status = ChatColor.RED + "Unavailable (no fix points set)";
        }
        return ItemFactory.icon(material,
                ChatColor.DARK_RED + "" + ChatColor.BOLD + name,
                ChatColor.GRAY + description,
                status);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
