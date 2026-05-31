package net.innerpine.amongus.corpse;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.util.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spawns and tracks {@link Corpse}s. Bodies are entirely client-visible Display
 * entities plus one {@link Interaction} hitbox players right-click to report.
 */
public final class CorpseManager {

    private final AmongUsPlugin plugin;
    private final List<Corpse> corpses = new ArrayList<>();
    private final Map<UUID, Corpse> byInteraction = new HashMap<>();

    public CorpseManager(AmongUsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Drops a body at the given player's feet.
     */
    public Corpse spawn(Player dead) {
        World world = dead.getWorld();
        Location base = dead.getLocation();
        List<Entity> parts = new ArrayList<>(4);

        // Flat "blood pool" under the body.
        BlockDisplay blood = world.spawn(base.clone(), BlockDisplay.class);
        blood.setBlock(Material.REDSTONE_BLOCK.createBlockData());
        blood.setTransformation(new Transformation(
                new Vector3f(-0.7f, 0.02f, -0.7f),
                new Quaternionf(),
                new Vector3f(1.4f, 0.08f, 1.4f),
                new Quaternionf()));
        blood.setBrightness(new Display.Brightness(15, 15));
        parts.add(blood);

        // The dead player's head, laid on the ground and glowing red so it can be found.
        ItemDisplay head = world.spawn(base.clone().add(0.0, 0.10, 0.0), ItemDisplay.class);
        head.setItemStack(ItemFactory.head(dead, Component.text(dead.getName())));
        head.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        head.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Quaternionf(),
                new Vector3f(1.1f, 1.1f, 1.1f),
                new Quaternionf()));
        head.setBrightness(new Display.Brightness(15, 15));
        head.setGlowing(true);
        head.setGlowColorOverride(Color.RED);
        parts.add(head);

        // Floating "☠ Name" label.
        TextDisplay label = world.spawn(base.clone().add(0.0, 1.15, 0.0), TextDisplay.class);
        label.text(Component.text("☠ ", NamedTextColor.DARK_RED)
                .append(Component.text(dead.getName(), NamedTextColor.RED)));
        label.setBillboard(Display.Billboard.CENTER);
        label.setSeeThrough(true);
        label.setBrightness(new Display.Brightness(15, 15));
        parts.add(label);

        // Clickable hitbox used for reporting.
        Interaction hitbox = world.spawn(base.clone(), Interaction.class);
        hitbox.setInteractionWidth(1.0f);
        hitbox.setInteractionHeight(0.8f);
        hitbox.setResponsive(true);
        parts.add(hitbox);

        Corpse corpse = new Corpse(dead.getUniqueId(), dead.getName(), base, parts, hitbox.getUniqueId());
        corpses.add(corpse);
        byInteraction.put(hitbox.getUniqueId(), corpse);
        return corpse;
    }

    public Corpse byInteraction(UUID interactionId) {
        return byInteraction.get(interactionId);
    }

    public int count() {
        return corpses.size();
    }

    /** Removes every corpse from the world (called when a meeting starts or the game resets). */
    public void removeAll() {
        for (Corpse corpse : corpses) {
            corpse.remove();
        }
        corpses.clear();
        byInteraction.clear();
    }
}
