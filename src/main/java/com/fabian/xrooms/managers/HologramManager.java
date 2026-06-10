package com.fabian.xrooms.managers;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;
import java.util.stream.Collectors;

public class HologramManager {

    private final XRooms plugin;
    private final boolean dhEnabled;
    private final boolean hdEnabled;

    public HologramManager(XRooms plugin) {
        this.plugin = plugin;
        this.dhEnabled = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
        this.hdEnabled = Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays");
        DebugLogger.debug("HologramManager", "Provider detection: DH=" + dhEnabled + " HD=" + hdEnabled);

        if (!dhEnabled && !hdEnabled) {
            plugin.getLogger().warning("No compatible hologram plugin found (DecentHolograms or HolographicDisplays). Holograms will be disabled.");
        }
    }

    public String getProviderName() {
        if (dhEnabled) return "DecentHolograms";
        if (hdEnabled) return "HolographicDisplays";
        return "None";
    }

    public void updateHolograms() {
        DebugLogger.debug("HologramManager", "Updating holograms for " + plugin.getRoomManager().getAllRooms().size() + " room(s)");
        for (Room room : plugin.getRoomManager().getAllRooms()) {
            if (!room.isHologramEnabled()) {
                removeHologram(room.getName());
                continue;
            }

            Location loc = room.getHologramLocation();
            if (loc == null) {
                loc = getCenter(room);
                if (loc != null) loc.add(0, 3, 0);
            }
            
            if (loc == null || loc.getWorld() == null) continue;

            List<String> lines = room.getHologramLines();
            if (lines == null || lines.isEmpty()) {
                lines = plugin.getConfig().getStringList("holograms.default-lines");
            }

            List<String> coloredLines = lines.stream()
                    .map(line -> plugin.getConfigManager().color(line
                            .replace("{name}", room.getDisplayName())
                            .replace("{players}", String.valueOf(plugin.getRoomManager().countPlayersInRoom(room)))
                            .replace("{max}", String.valueOf(room.getMaxPlayers()))
                            .replace("{time}", room.getPvpDuration() == -1 ? "∞" : room.getPvpDuration() + "s")))
                    .collect(Collectors.toList());

            if (dhEnabled) updateDecentHologram(room.getName(), loc, coloredLines);
            if (hdEnabled) updateHDHologram(room.getName(), loc, coloredLines);
        }
    }

    private void removeHologram(String name) {
        String id = "xrooms_" + name;
        if (dhEnabled) {
            try {
                eu.decentsoftware.holograms.api.DHAPI.removeHologram(id);
            } catch (NoClassDefFoundError | Exception ignored) {}
        }
        
        if (hdEnabled) {
            try {
                com.gmail.filoghost.holographicdisplays.api.HologramsAPI.getHolograms(plugin).stream()
                    .filter(h -> h.getLocation().getWorld() != null && h.getLocation().getWorld().getName().contains(name))
                    .forEach(com.gmail.filoghost.holographicdisplays.api.Hologram::delete);
            } catch (NoClassDefFoundError | Exception ignored) {}
        }
    }

    private void updateDecentHologram(String name, Location loc, List<String> lines) {
        try {
            String id = "xrooms_" + name;
            eu.decentsoftware.holograms.api.holograms.Hologram holo = eu.decentsoftware.holograms.api.DHAPI.getHologram(id);
            if (holo == null) {
                eu.decentsoftware.holograms.api.DHAPI.createHologram(id, loc, lines);
            } else {
                eu.decentsoftware.holograms.api.DHAPI.setHologramLines(holo, lines);
                eu.decentsoftware.holograms.api.DHAPI.moveHologram(holo, loc);
            }
        } catch (NoClassDefFoundError | Exception e) {
            // DecentHolograms not actually present or API changed
        }
    }

    private void updateHDHologram(String name, Location loc, List<String> lines) {
        try {
            com.gmail.filoghost.holographicdisplays.api.HologramsAPI.getHolograms(plugin).forEach(h -> {
                 if (h.getLocation().distanceSquared(loc) < 1) h.delete();
            });
            
            com.gmail.filoghost.holographicdisplays.api.Hologram holo = 
                    com.gmail.filoghost.holographicdisplays.api.HologramsAPI.createHologram(plugin, loc);
            for (String line : lines) {
                holo.appendTextLine(line);
            }
        } catch (NoClassDefFoundError | Exception e) {
            // HolographicDisplays not actually present
        }
    }

    private Location getCenter(Room room) {
        org.bukkit.World world = Bukkit.getWorld(room.getWorldName());
        if (world == null) return null;
        return new Location(world, 
                (room.getMinX() + room.getMaxX()) / 2, 
                room.getMinY(), 
                (room.getMinZ() + room.getMaxZ()) / 2);
    }
}
