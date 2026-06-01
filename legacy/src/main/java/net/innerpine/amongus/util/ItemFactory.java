package net.innerpine.amongus.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/** Builders for gameplay items and GUI icons (legacy String names/lore). */
public final class ItemFactory {

    private ItemFactory() {
    }

    public static ItemStack build(Material material, String type, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            if (type != null) {
                meta.getPersistentDataContainer().set(Keys.ITEM_TYPE, PersistentDataType.STRING, type);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String typeOf(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(Keys.ITEM_TYPE, PersistentDataType.STRING);
    }

    public static ItemStack killKnife() {
        return build(Material.IRON_SWORD, "kill",
                ChatColor.RED + "" + ChatColor.BOLD + "Kill",
                ChatColor.GRAY + "Right-click a nearby crewmate to eliminate them.");
    }

    public static ItemStack emergencyButton() {
        return build(Material.BELL, "emergency",
                ChatColor.GOLD + "" + ChatColor.BOLD + "Emergency Meeting",
                ChatColor.GRAY + "Right-click to call everyone to a vote.");
    }

    public static ItemStack sabotageItem() {
        return build(Material.COMPARATOR, "sabotage",
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "Sabotage",
                ChatColor.GRAY + "Right-click to open the sabotage menu.");
    }

    public static ItemStack head(OfflinePlayer owner, String name, String... lore) {
        ItemStack item = build(Material.PLAYER_HEAD, null, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skull = (SkullMeta) meta;
            skull.setOwningPlayer(owner);
            item.setItemMeta(skull);
        }
        return item;
    }

    public static ItemStack icon(Material material, String name, String... lore) {
        return build(material, null, name, lore);
    }
}
