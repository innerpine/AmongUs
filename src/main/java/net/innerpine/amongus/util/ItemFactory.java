package net.innerpine.amongus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builders for every gameplay item and GUI icon. Gameplay items are tagged in
 * their {@link org.bukkit.persistence.PersistentDataContainer} so listeners can
 * recognise them regardless of display name.
 */
public final class ItemFactory {

    private ItemFactory() {
    }

    private static Component clean(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    /** Generic builder; pass {@code type == null} for a purely cosmetic icon. */
    public static ItemStack build(Material material, String type, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(clean(name));
            if (lore.length > 0) {
                List<Component> lines = new ArrayList<>(lore.length);
                for (Component line : lore) {
                    lines.add(clean(line));
                }
                meta.lore(lines);
            }
            if (type != null) {
                meta.getPersistentDataContainer().set(Keys.ITEM_TYPE, PersistentDataType.STRING, type);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Returns the gameplay tag of an item, or {@code null} if it is not tagged. */
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
                Component.text("Kill", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Right-click a nearby crewmate to eliminate them.", NamedTextColor.GRAY));
    }

    public static ItemStack emergencyButton() {
        return build(Material.BELL, "emergency",
                Component.text("Emergency Meeting", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Right-click to call everyone to a vote.", NamedTextColor.GRAY));
    }

    public static ItemStack sabotageItem() {
        return build(Material.COMPARATOR, "sabotage",
                Component.text("Sabotage", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                Component.text("Right-click to open the sabotage menu.", NamedTextColor.GRAY));
    }

    public static ItemStack head(OfflinePlayer owner, Component name, Component... lore) {
        ItemStack item = build(Material.PLAYER_HEAD, null, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(owner);
            item.setItemMeta(skull);
        }
        return item;
    }

    public static ItemStack icon(Material material, Component name, Component... lore) {
        return build(material, null, name, lore);
    }
}
