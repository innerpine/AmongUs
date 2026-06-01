package net.innerpine.amongus.menu;

import net.innerpine.amongus.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Vent travel screen: one icon per vent plus an exit button. */
public final class VentMenu implements InventoryHolder {

    private final Inventory inventory;
    private final int contentLimit;
    private final int ventCount;
    private final int exitSlot;

    public VentMenu(int ventCount) {
        this.ventCount = ventCount;
        int rows = Math.min(6, Math.max(2, (int) Math.ceil(ventCount / 9.0) + 1));
        int size = rows * 9;
        this.contentLimit = size - 9;
        this.exitSlot = size - 5;

        this.inventory = Bukkit.createInventory(this, size, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Vents");
        for (int i = 0; i < ventCount && i < contentLimit; i++) {
            inventory.setItem(i, ItemFactory.icon(Material.IRON_TRAPDOOR,
                    ChatColor.AQUA + "Vent #" + (i + 1),
                    ChatColor.GRAY + "Click to travel here"));
        }
        inventory.setItem(exitSlot, ItemFactory.icon(Material.BARRIER,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Exit Vent"));
    }

    public int ventIndexAt(int slot) {
        return (slot >= 0 && slot < ventCount && slot < contentLimit) ? slot : -1;
    }

    public boolean isExit(int slot) {
        return slot == exitSlot;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
