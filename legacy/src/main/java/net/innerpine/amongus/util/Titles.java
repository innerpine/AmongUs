package net.innerpine.amongus.util;

import org.bukkit.entity.Player;

/** Thin wrapper over the legacy {@code Player#sendTitle} API. */
public final class Titles {

    private Titles() {
    }

    public static void send(Player player, String title, String subtitle, int stayTicks) {
        player.sendTitle(title, subtitle, 6, stayTicks, 8);
    }
}
