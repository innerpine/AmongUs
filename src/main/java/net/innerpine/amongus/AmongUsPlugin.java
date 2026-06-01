package net.innerpine.amongus;

import net.innerpine.amongus.command.AmongUsCommand;
import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.corpse.CorpseManager;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.listener.ChatListener;
import net.innerpine.amongus.listener.CombatListener;
import net.innerpine.amongus.listener.ConnectionListener;
import net.innerpine.amongus.listener.InteractListener;
import net.innerpine.amongus.listener.MenuListener;
import net.innerpine.amongus.listener.ProtectionListener;
import net.innerpine.amongus.meeting.MeetingManager;
import net.innerpine.amongus.sabotage.SabotageManager;
import net.innerpine.amongus.task.TaskManager;
import net.innerpine.amongus.util.Keys;
import net.innerpine.amongus.vent.VentManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point. Wires up the managers, listeners and the {@code /amongus}
 * command.
 */
public final class AmongUsPlugin extends JavaPlugin {

    private Game game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);

        GameSettings settings = new GameSettings(this);
        settings.load();

        CorpseManager corpses = new CorpseManager(this);
        TaskManager tasks = new TaskManager(this, settings);
        this.game = new Game(this, settings, tasks, corpses);
        MeetingManager meetings = new MeetingManager(this, game, settings);
        SabotageManager sabotage = new SabotageManager(this, settings, game);
        VentManager vent = new VentManager(game, settings);
        game.setMeetingManager(meetings);
        game.setSabotageManager(sabotage);
        game.setVentManager(vent);
        tasks.setGame(game);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ConnectionListener(game), this);
        pm.registerEvents(new ProtectionListener(game), this);
        pm.registerEvents(new CombatListener(game), this);
        pm.registerEvents(new InteractListener(game, meetings, tasks, corpses, sabotage, vent), this);
        pm.registerEvents(new MenuListener(meetings, tasks, sabotage, vent), this);
        pm.registerEvents(new ChatListener(this, game), this);

        AmongUsCommand command = new AmongUsCommand(game);
        PluginCommand pluginCommand = getCommand("amongus");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("AmongUs enabled — set up your arena with /amongus admin.");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            if (!game.isState(GameState.WAITING)) {
                game.stop();
            }
            game.corpses().removeAll();
        }
    }

    public Game game() {
        return game;
    }
}
