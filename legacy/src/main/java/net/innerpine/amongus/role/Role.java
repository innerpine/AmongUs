package net.innerpine.amongus.role;

import org.bukkit.ChatColor;

public enum Role {

    CREWMATE("Crewmate", ChatColor.AQUA),
    IMPOSTOR("Impostor", ChatColor.RED);

    private final String displayName;
    private final ChatColor color;

    Role(String displayName, ChatColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor color() {
        return color;
    }

    public boolean isImpostor() {
        return this == IMPOSTOR;
    }
}
