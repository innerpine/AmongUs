package net.innerpine.amongus.listener;

import net.innerpine.amongus.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Keeps the arena pristine: no building, no environmental damage, no hunger and
 * no dropping the gameplay items. Player-vs-player damage is left to the
 * {@link CombatListener} (which turns it into kills).
 */
public final class ProtectionListener implements Listener {

    private final Game game;

    public ProtectionListener(Game game) {
        this.game = game;
    }

    private boolean inGame(Player player) {
        return game.isParticipant(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (inGame(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (inGame(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return; // entity attacks are handled by CombatListener
        }
        if (event.getEntity() instanceof Player player && inGame(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && inGame(player)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (inGame(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
