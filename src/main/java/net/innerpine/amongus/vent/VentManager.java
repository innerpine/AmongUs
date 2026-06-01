package net.innerpine.amongus.vent;

import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.menu.VentMenu;
import net.innerpine.amongus.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Lets living impostors hide in vents and travel between them. While vented a
 * player is hidden from everyone else (handled by {@link Game#refreshVisibility()}).
 */
public final class VentManager {

    private final Game game;
    private final GameSettings settings;

    public VentManager(Game game, GameSettings settings) {
        this.game = game;
        this.settings = settings;
    }

    /** Returns the index of the vent at the given block, or {@code -1}. */
    public int ventAt(Location location) {
        List<Location> vents = settings.vents();
        for (int i = 0; i < vents.size(); i++) {
            Location vent = vents.get(i);
            if (location.getWorld() != null && vent.getWorld() != null
                    && location.getWorld().equals(vent.getWorld())
                    && location.getBlockX() == vent.getBlockX()
                    && location.getBlockY() == vent.getBlockY()
                    && location.getBlockZ() == vent.getBlockZ()) {
                return i;
            }
        }
        return -1;
    }

    public void enter(Player player, int index) {
        if (!game.isState(GameState.RUNNING)) {
            return;
        }
        GamePlayer gp = game.player(player.getUniqueId());
        if (gp == null || !gp.isImpostor() || gp.isGhost()) {
            return;
        }
        if (game.meetings() != null && game.meetings().isActive()) {
            return;
        }
        List<Location> vents = settings.vents();
        if (index < 0 || index >= vents.size() || game.isVented(player.getUniqueId())) {
            return;
        }
        game.markVented(player.getUniqueId());
        player.teleport(vents.get(index));
        player.openInventory(new VentMenu(vents.size()).getInventory());
        SoundUtil.play(player, "block.piston.contract", 1.2f);
    }

    public void travel(Player player, int index) {
        if (!game.isVented(player.getUniqueId())) {
            return;
        }
        List<Location> vents = settings.vents();
        if (index < 0 || index >= vents.size()) {
            return;
        }
        player.teleport(vents.get(index));
        SoundUtil.play(player, "block.piston.extend", 1.4f);
    }

    public void exit(Player player) {
        if (!game.isVented(player.getUniqueId())) {
            return;
        }
        game.unmarkVented(player.getUniqueId());
        SoundUtil.play(player, "block.piston.contract", 0.8f);
    }
}
