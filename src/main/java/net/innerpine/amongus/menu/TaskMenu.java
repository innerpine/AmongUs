package net.innerpine.amongus.menu;

import net.innerpine.amongus.util.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * The {@code CALIBRATE} task GUI: a grid of red panes with a single moving lime
 * pane. Clicking the lime pane the required number of times completes the task.
 */
public final class TaskMenu implements InventoryHolder {

    public enum Result {
        /** Clicked a non-target slot; nothing happens. */
        MISS,
        /** Hit the target; more clicks still required. */
        PROGRESS,
        /** Final required click; the task is finished. */
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
        this.inventory = Bukkit.createInventory(this, SIZE,
                Component.text(title, NamedTextColor.DARK_AQUA));
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

    public int remaining() {
        return Math.max(0, REQUIRED - done);
    }

    private ItemStack filler() {
        return ItemFactory.icon(Material.RED_STAINED_GLASS_PANE, Component.text(" "));
    }

    private ItemStack target() {
        return ItemFactory.icon(Material.LIME_STAINED_GLASS_PANE,
                Component.text("Calibrate!", NamedTextColor.GREEN));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
