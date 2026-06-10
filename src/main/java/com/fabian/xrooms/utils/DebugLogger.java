package com.fabian.xrooms.utils;

import com.fabian.xrooms.XRooms;
import org.bukkit.Bukkit;

public final class DebugLogger {

    private static final String PREFIX = "&8[&bX-Rooms&8] &b[DEBUG] &7";

    private DebugLogger() {}

    private static boolean isDebugEnabled() {
        XRooms instance = XRooms.getInstance();
        if (instance == null) return false;
        try {
            if (instance.getConfigManager() == null) return false;
            if (instance.getConfigManager().getConfig() == null) return false;
            return instance.getConfigManager().getConfig().getBoolean("debug", false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void debug(String message) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(ColorUtils.translateColors(PREFIX + message));
        }
    }

    public static void debug(String category, String message) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(ColorUtils.translateColors(PREFIX + "&f[" + category + "&f] &7" + message));
        }
    }

    public static void debug(String category, String message, Throwable throwable) {
        if (isDebugEnabled()) {
            Bukkit.getConsoleSender().sendMessage(ColorUtils.translateColors(PREFIX + "&f[" + category + "&f] &7" + message));
            throwable.printStackTrace();
        }
    }
}