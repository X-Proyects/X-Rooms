package com.fabian.xrooms.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.function.Consumer;

public class WorldEditUtils {

    public static class RegionSelection {
        public Location min;
        public Location max;
        public String world;

        public RegionSelection(Location min, Location max, String world) {
            this.min = min;
            this.max = max;
            this.world = world;
        }
    }

    public static RegionSelection getSelection(Player player) {
        Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (wePlugin == null) return null;

        boolean isModern = false;
        try {
            Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            isModern = true;
        } catch (ClassNotFoundException ignored) {}

        try {
            if (isModern) {
                // Modern WorldEdit 7+ or FAWE 2+
                return getModernSelection(player);
            } else {
                // Legacy WorldEdit 6 (1.8.8)
                return getLegacySelection(player, wePlugin);
            }
        } catch (Exception e) {
            Throwable cause = e;
            if (e instanceof java.lang.reflect.InvocationTargetException) {
                cause = e.getCause();
            }
            
            if (cause != null && cause.getClass().getSimpleName().equals("IncompleteRegionException")) {
                // Selection is not finished yet, just return null
                return null;
            }
            
            e.printStackTrace();
            return null;
        }
    }

    private static RegionSelection getModernSelection(Player player) throws Exception {
        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        
        // Adapt Player
        Method adaptPlayerMethod = bukkitAdapterClass.getMethod("adapt", Player.class);
        Object localPlayer = adaptPlayerMethod.invoke(null, player);

        // Get Session
        Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
        Object worldEditInstance = worldEditClass.getMethod("getInstance").invoke(null);
        Object sessionManager = worldEditInstance.getClass().getMethod("getSessionManager").invoke(worldEditInstance);
        
        Method getSessionMethod = null;
        for (Method m : sessionManager.getClass().getMethods()) {
            if (m.getName().equals("get") && m.getParameterCount() == 1) {
                if (m.getParameterTypes()[0].isAssignableFrom(localPlayer.getClass())) {
                    getSessionMethod = m;
                    break;
                }
            }
        }
        if (getSessionMethod == null) throw new NoSuchMethodException("Could not find SessionManager.get()");
        Object session = getSessionMethod.invoke(sessionManager, localPlayer);

        // Get World
        Method adaptWorldMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class);
        Object world = adaptWorldMethod.invoke(null, player.getWorld());

        // Get Selection
        Method getSelectionMethod = session.getClass().getMethod("getSelection", Class.forName("com.sk89q.worldedit.world.World"));
        Object region = getSelectionMethod.invoke(session, world);
        
        if (region == null) return null;
        
        Object min = region.getClass().getMethod("getMinimumPoint").invoke(region);
        Object max = region.getClass().getMethod("getMaximumPoint").invoke(region);
        
        double minX = ((Number) min.getClass().getMethod("getX").invoke(min)).doubleValue();
        double minY = ((Number) min.getClass().getMethod("getY").invoke(min)).doubleValue();
        double minZ = ((Number) min.getClass().getMethod("getZ").invoke(min)).doubleValue();
        
        double maxX = ((Number) max.getClass().getMethod("getX").invoke(max)).doubleValue();
        double maxY = ((Number) max.getClass().getMethod("getY").invoke(max)).doubleValue();
        double maxZ = ((Number) max.getClass().getMethod("getZ").invoke(max)).doubleValue();
        
        return new RegionSelection(
            new Location(player.getWorld(), minX, minY, minZ),
            new Location(player.getWorld(), maxX, maxY, maxZ),
            player.getWorld().getName()
        );
    }

    private static RegionSelection getLegacySelection(Player player, Plugin wePlugin) throws Exception {
        Method getSelectionMethod = wePlugin.getClass().getMethod("getSelection", Player.class);
        Object selection = getSelectionMethod.invoke(wePlugin, player);
        
        if (selection == null) return null;
        
        Location min = (Location) selection.getClass().getMethod("getMinimumPoint").invoke(selection);
        Location max = (Location) selection.getClass().getMethod("getMaximumPoint").invoke(selection);
        
        return new RegionSelection(min, max, player.getWorld().getName());
    }
    public static void saveSchematic(org.bukkit.plugin.Plugin plugin, com.fabian.xrooms.models.Room room, Player player) {
        saveSchematic(plugin, room, player, null);
    }

    public static void saveSchematic(org.bukkit.plugin.Plugin plugin, com.fabian.xrooms.models.Room room, Player player, Consumer<Boolean> callback) {
        File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) schematicsFolder.mkdirs();
        File file = new File(schematicsFolder, room.getName() + ".schem");

        try {
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            
            // Get Selection
            Object localPlayer = bukkitAdapterClass.getMethod("adapt", Player.class).invoke(null, player);
            Object worldEdit = worldEditClass.getMethod("getInstance").invoke(null);
            Object sessionManager = worldEdit.getClass().getMethod("getSessionManager").invoke(worldEdit);
            
            Method getSessionMethod = null;
            for (Method m : sessionManager.getClass().getMethods()) {
                if (m.getName().equals("get") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(localPlayer.getClass())) {
                    getSessionMethod = m;
                    break;
                }
            }
            if (getSessionMethod == null) {
                if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                return;
            }
            Object session = getSessionMethod.invoke(sessionManager, localPlayer);
            Object world = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, player.getWorld());
            Object region;
            try {
                region = session.getClass().getMethod("getSelection", Class.forName("com.sk89q.worldedit.world.World")).invoke(session, world);
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() != null && e.getCause().getClass().getSimpleName().equals("IncompleteRegionException")) {
                    if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                    return;
                }
                throw e;
            }

            if (region == null) {
                if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                return;
            }

            // Copy to Clipboard
            Class<?> blockArrayClipboardClass = Class.forName("com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard");
            Object clipboard = blockArrayClipboardClass.getConstructor(Class.forName("com.sk89q.worldedit.regions.Region")).newInstance(region);
            clipboard.getClass().getMethod("setOrigin", Class.forName("com.sk89q.worldedit.math.BlockVector3")).invoke(clipboard, region.getClass().getMethod("getMinimumPoint").invoke(region));

            Class<?> forwardExtentCopyClass = Class.forName("com.sk89q.worldedit.function.operation.ForwardExtentCopy");
            Object copyOperation = forwardExtentCopyClass.getConstructor(
                Class.forName("com.sk89q.worldedit.extent.Extent"), 
                Class.forName("com.sk89q.worldedit.regions.Region"), 
                Class.forName("com.sk89q.worldedit.extent.Extent"), 
                Class.forName("com.sk89q.worldedit.math.BlockVector3")
            ).newInstance(world, region, clipboard, region.getClass().getMethod("getMinimumPoint").invoke(region));

            // Run the blocking copy + file save asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Class<?> operationsClass = Class.forName("com.sk89q.worldedit.function.operation.Operations");
                    operationsClass.getMethod("complete", Class.forName("com.sk89q.worldedit.function.operation.Operation")).invoke(null, copyOperation);

                    // Save to File
                    Class<?> builtInClipboardFormatClass = Class.forName("com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat");
                    Object format = builtInClipboardFormatClass.getField("SPONGE_SCHEMATIC").get(null);
                    
                    try (OutputStream os = new FileOutputStream(file)) {
                        Object writer = format.getClass().getMethod("getWriter", OutputStream.class).invoke(format, os);
                        writer.getClass().getMethod("write", Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard")).invoke(writer, clipboard);
                        writer.getClass().getMethod("close").invoke(writer);
                    }

                    if (callback != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (callback != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
        }
    }

    public static void pasteSchematic(org.bukkit.plugin.Plugin plugin, com.fabian.xrooms.models.Room room) {
        pasteSchematic(plugin, room, null);
    }

    public static void pasteSchematic(org.bukkit.plugin.Plugin plugin, com.fabian.xrooms.models.Room room, Consumer<Boolean> callback) {
        File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        File file = new File(schematicsFolder, room.getName() + ".schem");
        DebugLogger.debug("WorldEditUtils", "Pasting schematic for room " + room.getName() + " (file=" + file.getName() + ")");
        if (!file.exists()) {
            if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            return;
        }

        try {
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> clipboardFormatsClass = Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats");
            Object format = clipboardFormatsClass.getMethod("findByFile", File.class).invoke(null, file);
            if (format == null) {
                if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                return;
            }

            Object clipboard;
            try (InputStream is = new FileInputStream(file)) {
                Object reader = format.getClass().getMethod("getReader", InputStream.class).invoke(format, is);
                clipboard = reader.getClass().getMethod("read").invoke(reader);
                reader.getClass().getMethod("close").invoke(reader);
            }

            if (clipboard == null) {
                if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                return;
            }

            org.bukkit.World bukkitWorld = Bukkit.getWorld(room.getWorldName());
            if (bukkitWorld == null) {
                if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                return;
            }
            Object world = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, bukkitWorld);

            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            Object worldEdit = worldEditClass.getMethod("getInstance").invoke(null);
            
            // EditSession
            Object editSession = worldEdit.getClass().getMethod("newEditSession", Class.forName("com.sk89q.worldedit.world.World")).invoke(worldEdit, world);
            
            Class<?> clipboardHolderClass = Class.forName("com.sk89q.worldedit.session.ClipboardHolder");
            Object holder = clipboardHolderClass.getConstructor(Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard")).newInstance(clipboard);

            Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Object pastePos = blockVector3Class.getMethod("at", double.class, double.class, double.class).invoke(null, room.getMinX(), room.getMinY(), room.getMinZ());

            Object pasteOperation = holder.getClass().getMethod("createPaste", Class.forName("com.sk89q.worldedit.extent.Extent")).invoke(holder, editSession);
            pasteOperation.getClass().getMethod("to", blockVector3Class).invoke(pasteOperation, pastePos);
            pasteOperation.getClass().getMethod("ignoreAirBlocks", boolean.class).invoke(pasteOperation, false);

            // Run the blocking paste operation asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    DebugLogger.debug("WorldEditUtils", "Starting async paste for room " + room.getName());
                    Class<?> operationsClass = Class.forName("com.sk89q.worldedit.function.operation.Operations");
                    operationsClass.getMethod("complete", Class.forName("com.sk89q.worldedit.function.operation.Operation")).invoke(null, pasteOperation);
                    
                    editSession.getClass().getMethod("close").invoke(editSession);
                    DebugLogger.debug("WorldEditUtils", "Paste completed for room " + room.getName());

                    if (callback != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (callback != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
        }
    }
}