package net.innerpine.amongus.listener;

import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.util.ActionBar;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Turns an impostor hitting a crewmate into a kill; blocks other PvP. */
public final class CombatListener implements Listener {

    private final Game game;

    public CombatListener(Game game) {
        this.game = game;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player attacker = resolveAttacker(event.getDamager());
        boolean victimInGame = game.isParticipant(victim.getUniqueId());

        if (attacker == null) {
            if (victimInGame) {
                event.setCancelled(true);
            }
            return;
        }

        if (!victimInGame && !game.isParticipant(attacker.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        if (game.canKill(attacker, victim)) {
            game.kill(attacker, victim);
            return;
        }

        GamePlayer attackerGp = game.player(attacker.getUniqueId());
        if (attackerGp != null && attackerGp.isImpostor() && !attackerGp.isGhost()) {
            int remaining = attackerGp.killCooldownRemaining();
            if (remaining > 0) {
                ActionBar.send(attacker, ChatColor.RED + "Kill on cooldown: " + remaining + "s");
            }
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            return (Player) ((Projectile) damager).getShooter();
        }
        return null;
    }
}
