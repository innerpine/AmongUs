package net.innerpine.amongus.listener;

import net.innerpine.amongus.corpse.Corpse;
import net.innerpine.amongus.corpse.CorpseManager;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.meeting.MeetingManager;
import net.innerpine.amongus.sabotage.FixStation;
import net.innerpine.amongus.sabotage.FixType;
import net.innerpine.amongus.sabotage.SabotageManager;
import net.innerpine.amongus.task.TaskManager;
import net.innerpine.amongus.task.TaskStation;
import net.innerpine.amongus.util.ActionBar;
import net.innerpine.amongus.util.ItemFactory;
import net.innerpine.amongus.vent.VentManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Right-click interactions: tasks, sabotage, kills, reporting, vents. */
public final class InteractListener implements Listener {

    private final Game game;
    private final MeetingManager meetings;
    private final TaskManager tasks;
    private final CorpseManager corpses;
    private final SabotageManager sabotage;
    private final VentManager vent;

    public InteractListener(Game game, MeetingManager meetings, TaskManager tasks, CorpseManager corpses,
                            SabotageManager sabotage, VentManager vent) {
        this.game = game;
        this.meetings = meetings;
        this.tasks = tasks;
        this.corpses = corpses;
        this.sabotage = sabotage;
        this.vent = vent;
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
            FixStation fix = sabotage.fixStationAt(block.getLocation());
            if (fix != null) {
                event.setCancelled(true);
                handleFix(player, fix);
                return;
            }
            int ventIndex = vent.ventAt(block.getLocation());
            if (ventIndex >= 0) {
                event.setCancelled(true);
                vent.enter(player, ventIndex);
                return;
            }
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            String type = ItemFactory.typeOf(event.getItem());
            if ("emergency".equals(type)) {
                event.setCancelled(true);
                handleEmergency(player);
            } else if ("sabotage".equals(type)) {
                event.setCancelled(true);
                sabotage.openMenu(player);
            }
        }
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!game.isParticipant(player.getUniqueId())) {
            return;
        }
        Entity clicked = event.getRightClicked();

        Corpse corpse = corpses.byStand(clicked.getUniqueId());
        if (corpse != null) {
            event.setCancelled(true);
            handleReport(player, corpse);
            return;
        }

        if (clicked instanceof Player && game.canKill(player, (Player) clicked)) {
            event.setCancelled(true);
            game.kill(player, (Player) clicked);
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Corpse corpse = corpses.byStand(event.getRightClicked().getUniqueId());
        if (corpse == null) {
            return;
        }
        event.setCancelled(true); // protect the head and treat the click as a report
        if (game.isParticipant(event.getPlayer().getUniqueId())) {
            handleReport(event.getPlayer(), corpse);
        }
    }

    private void handleEmergency(Player player) {
        if (!game.isState(GameState.RUNNING)) {
            ActionBar.send(player, ChatColor.RED + "You can only call a meeting during the round.");
            return;
        }
        if (sabotage.isReactorActive()) {
            ActionBar.send(player, ChatColor.RED + "You can't call a meeting during a reactor meltdown!");
            return;
        }
        if (meetings.isActive()) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || gp.isGhost()) {
            ActionBar.send(player, ChatColor.RED + "Ghosts cannot call meetings.");
            return;
        }
        if (!gp.useEmergencyMeeting()) {
            ActionBar.send(player, ChatColor.RED + "You have no emergency meetings left.");
            return;
        }
        boolean started = meetings.start(player,
                ChatColor.GOLD + player.getName() + " called an emergency meeting!");
        if (!started) {
            gp.setEmergencyMeetingsLeft(gp.emergencyMeetingsLeft() + 1);
        }
    }

    private void handleReport(Player player, Corpse corpse) {
        if (!game.isState(GameState.RUNNING) || meetings.isActive() || sabotage.isReactorActive()) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || gp.isGhost()) {
            ActionBar.send(player, ChatColor.RED + "Ghosts cannot report bodies.");
            return;
        }
        meetings.start(player, ChatColor.RED + player.getName() + " reported " + corpse.deadName() + "'s body!");
    }

    private void handleFix(Player player, FixStation fix) {
        if (!game.isState(GameState.RUNNING)) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || gp.isGhost()) {
            return;
        }
        if (fix.type() == FixType.LIGHTS) {
            sabotage.fixLights(player);
        } else {
            ActionBar.send(player, ChatColor.YELLOW + "Stay next to the reactor panel to stabilise it.");
        }
    }
}
