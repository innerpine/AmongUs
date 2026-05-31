package net.innerpine.amongus.listener;

import net.innerpine.amongus.meeting.MeetingManager;
import net.innerpine.amongus.menu.TaskMenu;
import net.innerpine.amongus.menu.VoteMenu;
import net.innerpine.amongus.task.TaskManager;
import net.innerpine.amongus.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Drives clicks in the {@link VoteMenu} and {@link TaskMenu}. Both menus are
 * read-only, so every interaction with them is cancelled.
 */
public final class MenuListener implements Listener {

    private final MeetingManager meetings;
    private final TaskManager tasks;

    public MenuListener(MeetingManager meetings, TaskManager tasks) {
        this.meetings = meetings;
        this.tasks = tasks;
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
            return;
        }

        if (holder instanceof TaskMenu menu) {
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
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof VoteMenu || holder instanceof TaskMenu) {
            event.setCancelled(true);
        }
    }

    private boolean isTopClick(InventoryClickEvent event) {
        return event.getWhoClicked() instanceof Player
                && event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory());
    }
}
