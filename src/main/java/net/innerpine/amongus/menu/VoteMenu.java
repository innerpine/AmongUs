package net.innerpine.amongus.menu;

import net.innerpine.amongus.util.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The clickable voting screen. One head per living candidate plus a "skip vote"
 * button. Identified at click-time via {@link InventoryHolder}.
 */
public final class VoteMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, UUID> candidates = new HashMap<>();
    private final int skipSlot;

    public VoteMenu(List<Player> alive) {
        int headRows = Math.max(1, (int) Math.ceil(alive.size() / 9.0));
        int rows = Math.min(6, headRows + 1);
        int size = rows * 9;

        this.inventory = Bukkit.createInventory(this, size,
                Component.text("Vote — who is the Impostor?", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        this.skipSlot = size - 5;

        int firstLastRowSlot = size - 9;
        int slot = 0;
        for (Player player : alive) {
            if (slot >= firstLastRowSlot) {
                break;
            }
            inventory.setItem(slot, ItemFactory.head(player,
                    Component.text(player.getName(), NamedTextColor.AQUA),
                    Component.text("Click to vote", NamedTextColor.GRAY)));
            candidates.put(slot, player.getUniqueId());
            slot++;
        }

        inventory.setItem(skipSlot, ItemFactory.icon(Material.BARRIER,
                Component.text("Skip Vote", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text("Click to skip without voting", NamedTextColor.GRAY)));
    }

    /** The candidate at the given slot, or {@code null} if the slot is not a head. */
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
