package net.innerpine.amongus.meeting;

import net.innerpine.amongus.AmongUsPlugin;
import net.innerpine.amongus.config.GameSettings;
import net.innerpine.amongus.game.Game;
import net.innerpine.amongus.game.GamePlayer;
import net.innerpine.amongus.game.GameState;
import net.innerpine.amongus.menu.VoteMenu;
import net.innerpine.amongus.util.Messages;
import net.innerpine.amongus.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs the discussion + voting flow: opens the clickable {@link VoteMenu},
 * collects one vote per living player and resolves the ejection.
 */
public final class MeetingManager {

    private enum Phase {
        NONE,
        DISCUSSION,
        VOTING
    }

    /** Sentinel target used to represent a "skip" vote. */
    private static final UUID SKIP = new UUID(0L, 0L);

    private final AmongUsPlugin plugin;
    private final Game game;
    private final GameSettings settings;

    private Phase phase = Phase.NONE;
    private int secondsLeft;
    private BukkitTask ticker;
    private final Map<UUID, UUID> votes = new HashMap<>();
    private final Set<UUID> menuOpen = new HashSet<>();

    public MeetingManager(AmongUsPlugin plugin, Game game, GameSettings settings) {
        this.plugin = plugin;
        this.game = game;
        this.settings = settings;
    }

    public boolean isActive() {
        return phase != Phase.NONE;
    }

    /** Begins a meeting; {@code reason} is the announcement line (already coloured). */
    public boolean start(Player initiator, Component reason) {
        if (!game.isState(GameState.RUNNING)) {
            return false;
        }
        game.beginMeeting();
        game.broadcast(Messages.prefixed(reason));
        game.broadcastSound("block.note_block.pling", 1.0f);

        votes.clear();
        menuOpen.clear();
        phase = Phase.DISCUSSION;
        secondsLeft = settings.discussionSeconds();
        startTicker();
        return true;
    }

    private void startTicker() {
        stopTicker();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
    }

    private void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private void tick() {
        if (!game.isState(GameState.MEETING)) {
            stopTicker();
            return;
        }
        switch (phase) {
            case DISCUSSION -> {
                if (secondsLeft <= 0) {
                    beginVoting();
                    return;
                }
                actionBarAll(Component.text("Discussion — " + secondsLeft + "s", NamedTextColor.YELLOW));
                secondsLeft--;
            }
            case VOTING -> {
                int alive = game.aliveOnline().size();
                if (votes.size() >= alive || secondsLeft <= 0) {
                    tally();
                    return;
                }
                actionBarAll(Component.text(
                        "Voting — " + secondsLeft + "s  (" + votes.size() + "/" + alive + ")",
                        NamedTextColor.GOLD));
                secondsLeft--;
            }
            default -> stopTicker();
        }
    }

    private void beginVoting() {
        phase = Phase.VOTING;
        secondsLeft = settings.votingSeconds();
        votes.clear();
        menuOpen.clear();

        List<Player> alive = game.aliveOnline();
        VoteMenu menu = new VoteMenu(alive);
        for (Player player : alive) {
            player.closeInventory();
            player.openInventory(menu.getInventory());
            menuOpen.add(player.getUniqueId());
        }
        for (Player player : game.onlineParticipants()) {
            GamePlayer gp = game.player(player.getUniqueId());
            if (gp != null && gp.isGhost()) {
                player.sendMessage(Messages.info("Ghosts cannot vote — watch the outcome."));
            }
        }
        game.broadcast(Messages.accent("Voting has started! Click a player, or skip."));
        game.broadcastSound("ui.button.click", 1.0f);
    }

    /** Records a vote. {@code target == null} means a skip. */
    public void castVote(Player voter, UUID target) {
        if (phase != Phase.VOTING) {
            return;
        }
        GamePlayer gp = game.player(voter.getUniqueId());
        if (gp == null || gp.isGhost()) {
            return;
        }
        if (votes.containsKey(voter.getUniqueId())) {
            return;
        }
        votes.put(voter.getUniqueId(), target == null ? SKIP : target);
        menuOpen.remove(voter.getUniqueId());
        voter.closeInventory();
        voter.sendMessage(target == null
                ? Messages.info("You skipped the vote.")
                : Messages.success("Your vote has been cast."));
        SoundUtil.play(voter, "ui.button.click", 1.2f);

        int alive = game.aliveOnline().size();
        game.broadcast(Messages.info(votes.size() + "/" + alive + " players have voted."));
        if (votes.size() >= alive) {
            tally();
        }
    }

    private void tally() {
        if (phase != Phase.VOTING) {
            return;
        }
        stopTicker();
        phase = Phase.NONE;
        for (UUID id : new ArrayList<>(menuOpen)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.closeInventory();
            }
        }
        menuOpen.clear();

        Map<UUID, Integer> counts = new HashMap<>();
        for (UUID target : votes.values()) {
            counts.merge(target, 1, Integer::sum);
        }
        int skipVotes = counts.getOrDefault(SKIP, 0);

        UUID top = null;
        int topVotes = 0;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            if (entry.getKey().equals(SKIP)) {
                continue;
            }
            if (entry.getValue() > topVotes) {
                top = entry.getKey();
                topVotes = entry.getValue();
                tie = false;
            } else if (entry.getValue() == topVotes) {
                tie = true;
            }
        }

        UUID ejected = (top == null || topVotes == 0 || tie || skipVotes >= topVotes) ? null : top;
        game.finishMeeting(ejected);
    }

    /** Aborts the meeting without resolving (used when the game ends/stops). */
    public void cancel() {
        stopTicker();
        phase = Phase.NONE;
        for (UUID id : new ArrayList<>(menuOpen)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.closeInventory();
            }
        }
        menuOpen.clear();
        votes.clear();
    }

    private void actionBarAll(Component component) {
        for (Player player : game.onlineParticipants()) {
            player.sendActionBar(component);
        }
    }
}
