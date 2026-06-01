package net.innerpine.amongus.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Plays a sound at the player's own location. */
public final class SoundUtil {

    private SoundUtil() {
    }

    public static void play(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 1.0f, pitch);
    }
}
