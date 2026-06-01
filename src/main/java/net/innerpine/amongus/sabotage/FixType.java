package net.innerpine.amongus.sabotage;

/**
 * The kinds of sabotage / fix points.
 */
public enum FixType {

    /** Non-critical: blinds the crew until any of them reaches a fix point. */
    LIGHTS,
    /** Critical: a countdown that the crew must cover two fix points to stop. */
    REACTOR;

    public static FixType fromString(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
