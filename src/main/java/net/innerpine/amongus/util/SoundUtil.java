package net.innerpine.amongus.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * Thin wrapper around the Adventure sound API so we never touch the
 * {@code org.bukkit.Sound} registry (which changed shape across 1.21.x).
 */
public final class SoundUtil {

    private SoundUtil() {
    }

    public static void play(Audience audience, String key, float pitch) {
        audience.playSound(Sound.sound(Key.key(key), Sound.Source.MASTER, 1.0f, pitch));
    }

    public static void play(Audience audience, String key) {
        play(audience, key, 1.0f);
    }
}
