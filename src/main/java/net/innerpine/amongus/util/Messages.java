package net.innerpine.amongus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Helpers for building the chat components used throughout the plugin so every
 * message shares the same prefix and colour scheme.
 */
public final class Messages {

    public static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Component.text("AmongUs", NamedTextColor.AQUA))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY));

    private Messages() {
    }

    public static Component prefixed(Component body) {
        return PREFIX.append(body);
    }

    public static Component info(String text) {
        return prefixed(Component.text(text, NamedTextColor.GRAY));
    }

    public static Component success(String text) {
        return prefixed(Component.text(text, NamedTextColor.GREEN));
    }

    public static Component error(String text) {
        return prefixed(Component.text(text, NamedTextColor.RED));
    }

    public static Component accent(String text) {
        return prefixed(Component.text(text, NamedTextColor.YELLOW));
    }
}
