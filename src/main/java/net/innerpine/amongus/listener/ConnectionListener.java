package net.innerpine.amongus.listener;

import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.util.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Greets joining players and cleans up after anyone who disconnects.
 */
public final class ConnectionListener implements Listener {

    private final Game game;

    public ConnectionListener(Game game) {
        this.game = game;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Messages.info("Welcome! Use /amongus join to play."));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        game.handleQuit(event.getPlayer());
    }
}
