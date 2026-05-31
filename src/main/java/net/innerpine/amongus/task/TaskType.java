package net.innerpine.amongus.task;

import org.bukkit.Material;

/**
 * The mini-games a task station can present.
 */
public enum TaskType {

    /** A clickable GUI: hit the moving lime pane a number of times. */
    CALIBRATE("Calibrate Distributor", Material.LIME_STAINED_GLASS_PANE),
    /** A short timed channel: stand still while a progress bar fills. */
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
