package net.innerpine.amongus.game;

import net.innerpine.amongus.role.Role;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Per-round state for a single participant. */
public final class GamePlayer {

    private final UUID uuid;
    private Role role = Role.CREWMATE;
    private boolean alive = true;

    private final Set<Integer> assignedTasks = new LinkedHashSet<>();
    private final Set<Integer> completedTasks = new LinkedHashSet<>();

    private int emergencyMeetingsLeft;
    private long killCooldownUntil;

    public GamePlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public Role role() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isImpostor() {
        return role.isImpostor();
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isGhost() {
        return !alive;
    }

    public void assignTasks(Collection<Integer> stationIds) {
        assignedTasks.clear();
        completedTasks.clear();
        assignedTasks.addAll(stationIds);
    }

    public boolean hasTask(int stationId) {
        return assignedTasks.contains(stationId);
    }

    public boolean isTaskDone(int stationId) {
        return completedTasks.contains(stationId);
    }

    public boolean completeTask(int stationId) {
        if (!assignedTasks.contains(stationId)) {
            return false;
        }
        return completedTasks.add(stationId);
    }

    public int assignedTaskCount() {
        return assignedTasks.size();
    }

    public int completedTaskCount() {
        return completedTasks.size();
    }

    public int emergencyMeetingsLeft() {
        return emergencyMeetingsLeft;
    }

    public void setEmergencyMeetingsLeft(int amount) {
        this.emergencyMeetingsLeft = amount;
    }

    public boolean useEmergencyMeeting() {
        if (emergencyMeetingsLeft <= 0) {
            return false;
        }
        emergencyMeetingsLeft--;
        return true;
    }

    public boolean canKill() {
        return System.currentTimeMillis() >= killCooldownUntil;
    }

    public void startKillCooldown(int seconds) {
        killCooldownUntil = System.currentTimeMillis() + seconds * 1000L;
    }

    public void clearKillCooldown() {
        killCooldownUntil = 0L;
    }

    public int killCooldownRemaining() {
        long remainingMillis = killCooldownUntil - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return 0;
        }
        return (int) Math.ceil(remainingMillis / 1000.0);
    }
}
