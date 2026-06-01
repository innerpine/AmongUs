package net.innerpine.amongus.listener;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** Splits chat: a private ghost channel and a normal living channel. */
public final class ChatListener implements Listener {

    private final AmongUsPlugin plugin;
    private final Game game;

    public ChatListener(AmongUsPlugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        GamePlayer gp = game.player(sender.getUniqueId());
        if (gp == null) {
            return;
        }
        event.setCancelled(true);

        boolean ghost = gp.isGhost();
        String message = ghost
                ? ChatColor.GRAY + "[Ghost] " + sender.getName() + ": " + event.getMessage()
                : ChatColor.WHITE + sender.getName() + ": " + event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> deliver(ghost, message));
    }

    private void deliver(boolean ghostChannel, String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            GamePlayer gp = game.player(online.getUniqueId());
            if (gp == null) {
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
