package net.innerpine.amongus.listener;

import net.innerpine.amongus.meeting.MeetingManager;
import net.innerpine.amongus.menu.SabotageMenu;
import net.innerpine.amongus.menu.TaskMenu;
import net.innerpine.amongus.menu.VentMenu;
import net.innerpine.amongus.menu.VoteMenu;
import net.innerpine.amongus.sabotage.FixType;
import net.innerpine.amongus.sabotage.SabotageManager;
import net.innerpine.amongus.task.TaskManager;
import net.innerpine.amongus.util.SoundUtil;
import net.innerpine.amongus.vent.VentManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Drives clicks in all of the plugin's GUIs (vote, task, sabotage, vent). Every
 * menu is read-only, so interactions are cancelled.
 */
public final class MenuListener implements Listener {

    private final MeetingManager meetings;
    private final TaskManager tasks;
    private final SabotageManager sabotage;
    private final VentManager vent;

    public MenuListener(MeetingManager meetings, TaskManager tasks, SabotageManager sabotage, VentManager vent) {
        this.meetings = meetings;
        this.tasks = tasks;
        this.sabotage = sabotage;
        this.vent = vent;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof VoteMenu menu) {
            event.setCancelled(true);
            if (!isTopClick(event)) {
                return;
            }
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            UUID candidate = menu.candidateAt(slot);
            if (candidate != null) {
                meetings.castVote(player, candidate);
            } else if (menu.isSkip(slot)) {
                meetings.castVote(player, null);
            }
        } else if (holder instanceof TaskMenu menu) {
            event.setCancelled(true);
            if (!isTopClick(event)) {
                return;
            }
            Player player = (Player) event.getWhoClicked();
            TaskMenu.Result result = menu.click(event.getRawSlot());
            if (result == TaskMenu.Result.PROGRESS) {
                SoundUtil.play(player, "block.note_block.hat", 1.6f);
            } else if (result == TaskMenu.Result.COMPLETE) {
                player.closeInventory();
                tasks.completeTask(player, menu.stationId());
            }
        } else if (holder instanceof SabotageMenu menu) {
            event.setCancelled(true);
            if (!isTopClick(event)) {
                return;
            }
            Player player = (Player) event.getWhoClicked();
            FixType type = menu.fixTypeAt(event.getRawSlot());
            if (type != null) {
                sabotage.trigger(player, type);
            }
        } else if (holder instanceof VentMenu menu) {
            event.setCancelled(true);
            if (!isTopClick(event)) {
                return;
            }
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();
            if (menu.isExit(slot)) {
                player.closeInventory();
            } else {
                int index = menu.ventIndexAt(slot);
                if (index >= 0) {
                    vent.travel(player, index);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof VentMenu
                && event.getPlayer() instanceof Player player) {
            vent.exit(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof VoteMenu || holder instanceof TaskMenu
                || holder instanceof SabotageMenu || holder instanceof VentMenu) {
            event.setCancelled(true);
        }
    }

    private boolean isTopClick(InventoryClickEvent event) {
        return event.getWhoClicked() instanceof Player
                && event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory());
    }
}
