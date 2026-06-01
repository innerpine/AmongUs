package net.innerpine.amongus.sabotage;

public enum FixType {

    LIGHTS,
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
