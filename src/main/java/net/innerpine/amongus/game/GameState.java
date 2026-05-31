package net.innerpine.amongus.game;

/**
 * High level state machine for a round.
 *
 * <pre>
 * WAITING ---start---&gt; COUNTDOWN ---&gt; RUNNING &lt;--&gt; MEETING
 *                                        |
 *                                      ENDING ---&gt; WAITING
 * </pre>
 */
public enum GameState {

    /** Lobby: players may join, nothing is happening yet. */
    WAITING,
    /** Roles assigned, players teleported, countdown ticking. */
    COUNTDOWN,
    /** Active play: tasks, kills, reports. */
    RUNNING,
    /** Discussion + voting screen is open. */
    MEETING,
    /** A team won; showing the end screen before resetting. */
    ENDING
}
