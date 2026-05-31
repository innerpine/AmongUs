package net.innerpine.amongus.listener;

import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Translates an impostor hitting a crewmate into a kill, and otherwise blocks
 * all player-vs-player damage between participants.
 */
public final class CombatListener implements Listener {

    private final Game game;

    public CombatListener(Game game) {
        this.game = game;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        boolean victimInGame = game.isParticipant(victim.getUniqueId());

        if (attacker == null) {
            if (victimInGame) {
                event.setCancelled(true);
            }
            return;
        }

        if (!victimInGame && !game.isParticipant(attacker.getUniqueId())) {
            return; // unrelated to our game
        }

        // Never apply vanilla combat damage to participants.
        event.setCancelled(true);

        if (game.canKill(attacker, victim)) {
            game.kill(attacker, victim);
            return;
        }

        GamePlayer attackerGp = game.player(attacker.getUniqueId());
        if (attackerGp != null && attackerGp.isImpostor() && !attackerGp.isGhost()) {
            int remaining = attackerGp.killCooldownRemaining();
            if (remaining > 0) {
                attacker.sendActionBar(Component.text("Kill on cooldown: " + remaining + "s", NamedTextColor.RED));
            }
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
