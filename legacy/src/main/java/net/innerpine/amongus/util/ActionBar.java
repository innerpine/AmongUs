package net.innerpine.amongus.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/** Sends action-bar messages via the Spigot/BungeeCord chat API (stable on 1.16.5). */
public final class ActionBar {

    private ActionBar() {
    }

    public static void send(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
