package com.fabian.xrooms.utils;

import com.fabian.xrooms.XRooms;
import org.bukkit.Bukkit;
import java.lang.reflect.Method;

public class SchedulerUtil {

    private final XRooms plugin;
    private static boolean isFolia = false;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            DebugLogger.debug("SchedulerUtil", "Folia detected - using region schedulers");
        } catch (ClassNotFoundException ignored) {
            DebugLogger.debug("SchedulerUtil", "Using standard Bukkit scheduler");
        }
    }

    public SchedulerUtil(XRooms plugin) {
        this.plugin = plugin;
    }

    public void runTimer(Runnable runnable, long delay, long period) {
        if (isFolia) {
            try {
                Object server = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = server.getClass().getMethod("runAtFixedRate", 
                        org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class, long.class);
                
                // Folia's consumer receives a scheduled task, we just wrap our runnable
                runMethod.invoke(server, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), delay, period);
            } catch (Exception e) {
                // Fallback to Bukkit if reflection fails
                Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        }
    }

    public void runTaskLater(Runnable runnable, long delay) {
        if (isFolia) {
            try {
                Object server = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = server.getClass().getMethod("runDelayed", 
                        org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class);
                
                runMethod.invoke(server, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), delay);
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    public void runTask(Runnable runnable) {
        if (isFolia) {
            try {
                Object server = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = server.getClass().getMethod("execute", 
                        org.bukkit.plugin.Plugin.class, Runnable.class);
                runMethod.invoke(server, plugin, runnable);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runAsync(Runnable runnable) {
        if (isFolia) {
            try {
                Object server = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runMethod = server.getClass().getMethod("runNow", 
                        org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
                runMethod.invoke(server, plugin, (java.util.function.Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                // Last fallback: Plain Java Thread
                new Thread(runnable).start();
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public void teleport(org.bukkit.entity.Player player, org.bukkit.Location location) {
        if (isFolia) {
            try {
                // Folia requires teleportAsync
                Method teleportAsync = player.getClass().getMethod("teleportAsync", org.bukkit.Location.class);
                teleportAsync.invoke(player, location);
            } catch (Exception e) {
                player.teleport(location);
            }
        } else {
            player.teleport(location);
        }
    }
}
