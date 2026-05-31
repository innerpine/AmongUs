package net.innerpine.amongus.role;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * The two teams a player can belong to during a round.
 */
public enum Role {

    CREWMATE("Crewmate", NamedTextColor.AQUA),
    IMPOSTOR("Impostor", NamedTextColor.RED);

    private final String displayName;
    private final NamedTextColor color;

    Role(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public NamedTextColor color() {
        return color;
    }

    public boolean isImpostor() {
        return this == IMPOSTOR;
    }
}
