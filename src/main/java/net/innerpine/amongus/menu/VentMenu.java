package net.innerpine.amongus.menu;

import net.innerpine.amongus.util.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * The vent travel screen shown while an impostor is inside a vent: one icon per
 * vent plus an exit button. Selecting a vent teleports without closing the menu.
 */
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

        this.inventory = Bukkit.createInventory(this, size,
                Component.text("Vents", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
        for (int i = 0; i < ventCount && i < contentLimit; i++) {
            inventory.setItem(i, ItemFactory.icon(Material.IRON_TRAPDOOR,
                    Component.text("Vent #" + (i + 1), NamedTextColor.AQUA),
                    Component.text("Click to travel here", NamedTextColor.GRAY)));
        }
        inventory.setItem(exitSlot, ItemFactory.icon(Material.BARRIER,
                Component.text("Exit Vent", NamedTextColor.YELLOW, TextDecoration.BOLD)));
    }

    /** Returns the vent index for a slot, or {@code -1} if it isn't a vent icon. */
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
