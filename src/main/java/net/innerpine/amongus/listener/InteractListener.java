package net.innerpine.amongus.listener;

import net.innerpine.amongus.corpse.Corpse;
import net.innerpine.amongus.corpse.CorpseManager;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.meeting.MeetingManager;
import net.innerpine.amongus.task.TaskManager;
import net.innerpine.amongus.task.TaskStation;
import net.innerpine.amongus.util.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles every "right-click" interaction: starting tasks, calling emergency
 * meetings, killing by right-click and reporting bodies.
 */
public final class InteractListener implements Listener {

    private final Game game;
    private final MeetingManager meetings;
    private final TaskManager tasks;
    private final CorpseManager corpses;

    public InteractListener(Game game, MeetingManager meetings, TaskManager tasks, CorpseManager corpses) {
        this.game = game;
        this.meetings = meetings;
        this.tasks = tasks;
        this.corpses = corpses;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!game.isParticipant(player.getUniqueId())) {
            return;
        }
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            TaskStation station = tasks.stationAt(block.getLocation());
            if (station != null) {
                event.setCancelled(true);
                tasks.onInteract(player, station);
                return;
            }
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if ("emergency".equals(ItemFactory.typeOf(item))) {
                event.setCancelled(true);
                handleEmergency(player);
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!game.isParticipant(player.getUniqueId())) {
            return;
        }
        Entity clicked = event.getRightClicked();

        Corpse corpse = corpses.byInteraction(clicked.getUniqueId());
        if (corpse != null) {
            event.setCancelled(true);
            handleReport(player, corpse);
            return;
        }

        if (clicked instanceof Player victim && game.canKill(player, victim)) {
            event.setCancelled(true);
            game.kill(player, victim);
        }
    }

    private void handleEmergency(Player player) {
        if (!game.isState(GameState.RUNNING)) {
            player.sendActionBar(Component.text("You can only call a meeting during the round.", NamedTextColor.RED));
            return;
        }
        if (meetings.isActive()) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || gp.isGhost()) {
            player.sendActionBar(Component.text("Ghosts cannot call meetings.", NamedTextColor.RED));
            return;
        }
        if (!gp.useEmergencyMeeting()) {
            player.sendActionBar(Component.text("You have no emergency meetings left.", NamedTextColor.RED));
            return;
        }
        boolean started = meetings.start(player,
                Component.text(player.getName() + " called an emergency meeting!", NamedTextColor.GOLD));
        if (!started) {
            gp.setEmergencyMeetingsLeft(gp.emergencyMeetingsLeft() + 1); // refund on failure
        }
    }

    private void handleReport(Player player, Corpse corpse) {
        if (!game.isState(GameState.RUNNING) || meetings.isActive()) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || gp.isGhost()) {
            player.sendActionBar(Component.text("Ghosts cannot report bodies.", NamedTextColor.RED));
            return;
        }
        meetings.start(player, Component.text(
                player.getName() + " reported " + corpse.deadName() + "'s body!", NamedTextColor.RED));
    }
}
