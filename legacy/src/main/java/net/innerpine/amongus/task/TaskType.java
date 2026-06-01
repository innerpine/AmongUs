package net.innerpine.amongus.task;

import org.bukkit.Material;

public enum TaskType {

    CALIBRATE("Calibrate Distributor", Material.LIME_STAINED_GLASS_PANE),
    DOWNLOAD("Download Data", Material.OBSERVER);

    private final String title;
    private final Material icon;

    TaskType(String title, Material icon) {
        this.title = title;
        this.icon = icon;
    }

    public String title() {
        return title;
    }

    public Material icon() {
        return icon;
    }

    public static TaskType fromString(String raw) {
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
