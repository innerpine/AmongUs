package net.innerpine.amongus.command;

import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.sabotage.FixType;
import net.innerpine.amongus.task.TaskStation;
import net.innerpine.amongus.task.TaskType;
import net.innerpine.amongus.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implements {@code /amongus} and its sub-commands plus tab completion.
 */
public final class AmongUsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT = List.of("join", "leave", "start", "stop", "status", "admin");
    private static final List<String> ADMIN = List.of(
            "setlobby", "setmeeting", "addspawn", "clearspawns", "addtask", "cleartasks",
            "addfix", "clearfixes", "addvent", "clearvents", "reload", "info");

    private final Game game;

    public AmongUsCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "join" -> requirePlayer(sender, game::addToLobby);
            case "leave" -> requirePlayer(sender, game::leave);
            case "start" -> game.start(sender);
            case "stop" -> {
                if (!sender.hasPermission("amongus.admin")) {
                    sender.sendMessage(Messages.error("You don't have permission to do that."));
                } else if (!game.stop()) {
                    sender.sendMessage(Messages.error("No round is currently running."));
                }
            }
            case "status" -> sendStatus(sender);
            case "admin" -> handleAdmin(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amongus.admin")) {
            sender.sendMessage(Messages.error("You don't have permission to do that."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Messages.info("Admin: /amongus admin <" + String.join("|", ADMIN) + ">"));
            return;
        }
        GameSettings settings = game.settings();
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "setlobby" -> requirePlayer(sender, player -> {
                settings.setLobby(player.getLocation());
                player.sendMessage(Messages.success("Lobby spawn set."));
            });
            case "setmeeting" -> requirePlayer(sender, player -> {
                settings.setMeeting(player.getLocation());
                player.sendMessage(Messages.success("Meeting point set."));
            });
            case "addspawn" -> requirePlayer(sender, player -> {
                settings.addSpawn(player.getLocation());
                player.sendMessage(Messages.success("Game spawn added (" + settings.spawns().size() + " total)."));
            });
            case "clearspawns" -> {
                settings.clearSpawns();
                sender.sendMessage(Messages.success("All game spawns cleared."));
            }
            case "addtask" -> requirePlayer(sender, player -> addTask(player, args));
            case "cleartasks" -> {
                settings.clearTasks();
                sender.sendMessage(Messages.success("All task stations cleared."));
            }
            case "addfix" -> requirePlayer(sender, player -> addFix(player, args));
            case "clearfixes" -> {
                settings.clearFixes();
                sender.sendMessage(Messages.success("All sabotage fix points cleared."));
            }
            case "addvent" -> requirePlayer(sender, player -> {
                org.bukkit.block.Block target = player.getTargetBlockExact(6);
                if (target == null) {
                    player.sendMessage(Messages.error("Look directly at the block you want to use as a vent."));
                    return;
                }
                settings.addVent(target.getLocation());
                player.sendMessage(Messages.success("Vent added (" + settings.vents().size() + " total)."));
            });
            case "clearvents" -> {
                settings.clearVents();
                sender.sendMessage(Messages.success("All vents cleared."));
            }
            case "reload" -> {
                game.plugin().reloadConfig();
                settings.load();
                sender.sendMessage(Messages.success("Configuration reloaded."));
            }
            case "info" -> sendStatus(sender);
            default -> sender.sendMessage(Messages.error("Unknown admin sub-command."));
        }
    }

    private void addTask(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.error("Usage: /amongus admin addtask <" + typeList() + ">"));
            return;
        }
        TaskType type = TaskType.fromString(args[2]);
        if (type == null) {
            player.sendMessage(Messages.error("Unknown task type. Options: " + typeList()));
            return;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null) {
            player.sendMessage(Messages.error("Look directly at the block you want to use as a task station."));
            return;
        }
        TaskStation station = game.settings().addTask(target.getLocation(), type);
        player.sendMessage(Messages.success("Added " + type.title() + " station #" + station.id()
                + " at " + target.getX() + ", " + target.getY() + ", " + target.getZ() + "."));
    }

    private void addFix(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Messages.error("Usage: /amongus admin addfix <lights|reactor>"));
            return;
        }
        FixType type = FixType.fromString(args[2]);
        if (type == null) {
            player.sendMessage(Messages.error("Unknown fix type. Options: lights, reactor"));
            return;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null) {
            player.sendMessage(Messages.error("Look directly at the block you want to use as a fix point."));
            return;
        }
        game.settings().addFix(target.getLocation(), type);
        player.sendMessage(Messages.success("Added " + type.name().toLowerCase(Locale.ROOT) + " fix point at "
                + target.getX() + ", " + target.getY() + ", " + target.getZ() + "."));
    }

    private void sendStatus(CommandSender sender) {
        GameSettings settings = game.settings();
        sender.sendMessage(Messages.accent("AmongUs status"));
        sender.sendMessage(line("State", game.state().name()));
        sender.sendMessage(line("Players", game.onlineParticipants().size() + " (min " + settings.minPlayers() + ")"));
        sender.sendMessage(line("Lobby", settings.lobby() != null ? "set" : "MISSING"));
        sender.sendMessage(line("Meeting", settings.meeting() != null ? "set" : "(falls back to lobby)"));
        sender.sendMessage(line("Spawns", String.valueOf(settings.spawns().size())));
        sender.sendMessage(line("Task stations", String.valueOf(settings.tasks().size())));
        sender.sendMessage(line("Fix points", String.valueOf(settings.fixes().size())));
        sender.sendMessage(line("Vents", String.valueOf(settings.vents().size())));
    }

    private Component line(String key, String value) {
        return Component.text("  " + key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Messages.accent("AmongUs commands"));
        sender.sendMessage(help("/amongus join", "join the lobby"));
        sender.sendMessage(help("/amongus leave", "leave the game"));
        sender.sendMessage(help("/amongus start", "start the round"));
        sender.sendMessage(help("/amongus status", "show game status"));
        if (sender.hasPermission("amongus.admin")) {
            sender.sendMessage(help("/amongus stop", "force-stop the round"));
            sender.sendMessage(help("/amongus admin ...", "configure the arena"));
        }
    }

    private Component help(String command, String description) {
        return Component.text("  " + command + " ", NamedTextColor.YELLOW)
                .append(Component.text("- " + description, NamedTextColor.GRAY));
    }

    private void requirePlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (sender instanceof Player player) {
            action.accept(player);
        } else {
            sender.sendMessage(Messages.error("Only players can use that command."));
        }
    }

    private String typeList() {
        List<String> names = new ArrayList<>();
        for (TaskType type : TaskType.values()) {
            names.add(type.name().toLowerCase(Locale.ROOT));
        }
        return String.join("|", names);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(ROOT, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return filter(ADMIN, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("addtask")) {
            List<String> types = new ArrayList<>();
            for (TaskType type : TaskType.values()) {
                types.add(type.name().toLowerCase(Locale.ROOT));
            }
            return filter(types, args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("addfix")) {
            return filter(List.of("lights", "reactor"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
