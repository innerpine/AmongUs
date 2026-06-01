package net.innerpine.amongus.menu;

import net.innerpine.amongus.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/** The CALIBRATE task GUI: click the moving lime pane a number of times. */
public final class TaskMenu implements InventoryHolder {

    public enum Result {
        MISS,
        PROGRESS,
        COMPLETE
    }

    private static final int SIZE = 27;
    private static final int REQUIRED = 5;

    private final int stationId;
    private final Inventory inventory;
    private final Random random = new Random();

    private int done;
    private int targetSlot;

    public TaskMenu(int stationId, String title) {
        this.stationId = stationId;
        this.inventory = Bukkit.createInventory(this, SIZE, ChatColor.DARK_AQUA + title);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler());
        }
        targetSlot = random.nextInt(SIZE);
        inventory.setItem(targetSlot, target());
    }

    public Result click(int slot) {
        if (slot != targetSlot || slot < 0 || slot >= SIZE) {
            return Result.MISS;
        }
        inventory.setItem(targetSlot, filler());
        done++;
        if (done >= REQUIRED) {
            return Result.COMPLETE;
        }
        int next;
        do {
            next = random.nextInt(SIZE);
        } while (next == targetSlot);
        targetSlot = next;
        inventory.setItem(targetSlot, target());
        return Result.PROGRESS;
    }

    public int stationId() {
        return stationId;
    }

    private ItemStack filler() {
        return ItemFactory.icon(Material.RED_STAINED_GLASS_PANE, " ");
    }

    private ItemStack target() {
        return ItemFactory.icon(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Calibrate!");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
