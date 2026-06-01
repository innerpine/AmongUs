package net.innerpine.amongus.util;

import org.bukkit.ChatColor;

/** Legacy (§-coloured String) message helpers with a shared prefix. */
public final class Msg {

    public static final String PREFIX =
            ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "AmongUs" + ChatColor.DARK_GRAY + "] ";

    private Msg() {
    }

    public static String info(String text) {
        return PREFIX + ChatColor.GRAY + text;
    }

    public static String success(String text) {
        return PREFIX + ChatColor.GREEN + text;
    }

    public static String error(String text) {
        return PREFIX + ChatColor.RED + text;
    }

    public static String accent(String text) {
        return PREFIX + ChatColor.YELLOW + text;
    }
}
