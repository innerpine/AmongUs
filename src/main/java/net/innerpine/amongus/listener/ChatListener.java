package net.innerpine.amongus.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Splits chat into two channels: the living (and spectators) see normal chat,
 * while dead players have a private ghost channel only other ghosts can read.
 */
public final class ChatListener implements Listener {

    private final AmongUsPlugin plugin;
    private final Game game;

    public ChatListener(AmongUsPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        GamePlayer gp = game.player(sender.getUniqueId());
        if (gp == null) {
            return; // not in a game — leave chat untouched
        }
        event.setCancelled(true);

        boolean ghost = gp.isGhost();
        Component message = ghost
                ? Component.text("[Ghost] " + sender.getName() + ": ", NamedTextColor.GRAY).append(event.message())
                : Component.text(sender.getName() + ": ", NamedTextColor.WHITE).append(event.message());

        // Deliver on the main thread (chat events fire asynchronously).
        Bukkit.getScheduler().runTask(plugin, () -> deliver(ghost, message));
    }

    private void deliver(boolean ghostChannel, Component message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            GamePlayer gp = game.player(online.getUniqueId());
            if (gp == null) {
                // Spectators / non-participants only see the living channel.
                if (!ghostChannel) {
                    online.sendMessage(message);
                }
            } else if (ghostChannel) {
                if (gp.isGhost()) {
                    online.sendMessage(message);
                }
            } else {
                online.sendMessage(message);
            }
        }
    }
}
