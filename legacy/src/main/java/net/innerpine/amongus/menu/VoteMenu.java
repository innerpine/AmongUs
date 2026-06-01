package net.innerpine.amongus.menu;

import net.innerpine.amongus.util.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Clickable voting screen: one head per living candidate plus a skip button. */
public final class VoteMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, UUID> candidates = new HashMap<>();
    private final int skipSlot;

    public VoteMenu(List<Player> alive) {
        int headRows = Math.max(1, (int) Math.ceil(alive.size() / 9.0));
        int rows = Math.min(6, headRows + 1);
        int size = rows * 9;

        this.inventory = Bukkit.createInventory(this, size,
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "Vote — who is the Impostor?");
        this.skipSlot = size - 5;

        int firstLastRowSlot = size - 9;
        int slot = 0;
        for (Player player : alive) {
            if (slot >= firstLastRowSlot) {
                break;
            }
            inventory.setItem(slot, ItemFactory.head(player,
                    ChatColor.AQUA + player.getName(),
                    ChatColor.GRAY + "Click to vote"));
            candidates.put(slot, player.getUniqueId());
            slot++;
        }

        inventory.setItem(skipSlot, ItemFactory.icon(Material.BARRIER,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Skip Vote",
                ChatColor.GRAY + "Click to skip without voting"));
    }

    public UUID candidateAt(int slot) {
        return candidates.get(slot);
    }

    public boolean isSkip(int slot) {
        return slot == skipSlot;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
