package com.fabian.xrooms.managers;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.models.Room;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

public class RoomManager {

    private final File roomsFolder;
    private final Map<String, Room> rooms = new HashMap<>();

    public RoomManager(XRooms plugin) {
        this.roomsFolder = new File(plugin.getDataFolder(), "rooms");
        if (!roomsFolder.exists()) {
            roomsFolder.mkdirs();
            plugin.saveResource("rooms/example.yml", false);
        }
        loadAll();
    }

    public void createRoom(Room room) {
        rooms.put(room.getName().toLowerCase(), room);
        saveRoom(room);
    }

    public void deleteRoom(String name) {
        rooms.remove(name.toLowerCase());
        File file = new File(roomsFolder, name.toLowerCase() + ".yml");
        if (file.exists()) file.delete();
    }

    public Room getRoom(String name) {
        return rooms.get(name.toLowerCase());
    }

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public Room getRoomAt(org.bukkit.Location loc) {
        for (Room room : rooms.values()) {
            if (room.isInside(loc)) return room;
        }
        return null;
    }

    public int countPlayersInRoom(Room room) {
        int count = 0;
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (room.isInside(player.getLocation())) count++;
        }
        return count;
    }

    public void loadAll() {
        rooms.clear();
        File[] files = roomsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = file.getName().replace(".yml", "");
            Room room = new Room(name);
            
            room.setDisplayName(config.getString("displayName", name));
            room.setPermission(config.getString("permission", ""));
            room.setRequirePermission(config.getBoolean("require-permission", false));
            
            int minP = config.getInt("min-players", 2);
            room.setMinPlayers(Math.max(2, minP));
            room.setMaxPlayers(config.getInt("max-players", 10));
            room.setPvpDuration(config.getInt("pvp-duration", -1));
            
            room.setHologramEnabled(config.getBoolean("hologram.enabled", true));
            room.setHologramLines(config.getStringList("hologram.lines"));
            if (config.contains("hologram.location.world")) {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(config.getString("hologram.location.world", "world"));
                double hx = config.getDouble("hologram.location.x", 0);
                double hy = config.getDouble("hologram.location.y", 0);
                double hz = config.getDouble("hologram.location.z", 0);
                float hpitch = (float) config.getDouble("hologram.location.pitch", 0);
                float hyaw = (float) config.getDouble("hologram.location.yaw", 0);
                room.setHologramLocation(new org.bukkit.Location(w, hx, hy, hz, hyaw, hpitch));
            }

            if (config.contains("winner-location.world")) {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(config.getString("winner-location.world", "world"));
                double wx = config.getDouble("winner-location.x", 0);
                double wy = config.getDouble("winner-location.y", 0);
                double wz = config.getDouble("winner-location.z", 0);
                float wpitch = (float) config.getDouble("winner-location.pitch", 0);
                float wyaw = (float) config.getDouble("winner-location.yaw", 0);
                room.setWinnerLocation(new org.bukkit.Location(w, wx, wy, wz, wyaw, wpitch));
            }
            
            room.setWorldName(config.getString("region.world", "world"));
            room.setMinX(config.getDouble("region.minX"));
            room.setMinY(config.getDouble("region.minY"));
            room.setMinZ(config.getDouble("region.minZ"));
            room.setMaxX(config.getDouble("region.maxX"));
            room.setMaxY(config.getDouble("region.maxY"));
            room.setMaxZ(config.getDouble("region.maxZ"));
            
            room.setSchematicEnabled(config.getBoolean("region.schematic-enabled", false));
            
            room.setEntrySound(config.getString("sounds.entry", "ENTITY_PLAYER_LEVELUP"));
            room.setKillSound(config.getString("sounds.kill", "ENTITY_EXPERIENCE_ORB_PICKUP"));
            room.setEntryTitle(config.getString("visuals.title", "&b&l{name}"));
            room.setEntrySubtitle(config.getString("visuals.subtitle", "&fWelcome to the room!"));
            
            room.setRewards(deserializeItemStackList(config, "rewards"));
            room.setEquipment(deserializeItemStackList(config, "equipment"));
            
            if (config.contains("abilities")) {
                for (String key : config.getConfigurationSection("abilities").getKeys(false)) {
                    room.getAbilities().put(key.toLowerCase(), config.getInt("abilities." + key));
                }
            }
            
            rooms.put(name.toLowerCase(), room);
        }
    }

    public void saveRoom(Room room) {
        File file = new File(roomsFolder, room.getName().toLowerCase() + ".yml");
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        config.set("displayName", room.getDisplayName());
        config.set("permission", room.getPermission());
        config.set("require-permission", room.isRequirePermission());
        config.set("min-players", Math.max(2, room.getMinPlayers()));
        config.set("max-players", room.getMaxPlayers());
        config.set("pvp-duration", room.getPvpDuration());
        
        config.set("hologram.enabled", room.isHologramEnabled());
        config.set("hologram.lines", room.getHologramLines());
        if (room.getHologramLocation() != null) {
            config.set("hologram.location.world", room.getHologramLocation().getWorld() != null ? room.getHologramLocation().getWorld().getName() : "world");
            config.set("hologram.location.x", room.getHologramLocation().getX());
            config.set("hologram.location.y", room.getHologramLocation().getY());
            config.set("hologram.location.z", room.getHologramLocation().getZ());
            config.set("hologram.location.pitch", room.getHologramLocation().getPitch());
            config.set("hologram.location.yaw", room.getHologramLocation().getYaw());
        }

        if (room.getWinnerLocation() != null) {
            config.set("winner-location.world", room.getWinnerLocation().getWorld() != null ? room.getWinnerLocation().getWorld().getName() : "world");
            config.set("winner-location.x", room.getWinnerLocation().getX());
            config.set("winner-location.y", room.getWinnerLocation().getY());
            config.set("winner-location.z", room.getWinnerLocation().getZ());
            config.set("winner-location.pitch", room.getWinnerLocation().getPitch());
            config.set("winner-location.yaw", room.getWinnerLocation().getYaw());
        }

        config.set("region.world", room.getWorldName());
        config.set("region.minX", room.getMinX());
        config.set("region.minY", room.getMinY());
        config.set("region.minZ", room.getMinZ());
        config.set("region.maxX", room.getMaxX());
        config.set("region.maxY", room.getMaxY());
        config.set("region.maxZ", room.getMaxZ());
        config.set("region.schematic-enabled", room.isSchematicEnabled());
        
        config.set("sounds.entry", room.getEntrySound());
        config.set("sounds.kill", room.getKillSound());
        config.set("visuals.title", room.getEntryTitle());
        config.set("visuals.subtitle", room.getEntrySubtitle());
        
        config.set("rewards", room.getRewards());
        config.set("equipment", room.getEquipment());
        
        if (!room.getAbilities().isEmpty()) {
            for (Map.Entry<String, Integer> entry : room.getAbilities().entrySet()) {
                config.set("abilities." + entry.getKey(), entry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        rooms.values().forEach(this::saveRoom);
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> deserializeItemStackList(org.bukkit.configuration.ConfigurationSection config, String path) {
        List<ItemStack> result = new ArrayList<>();
        if (!config.contains(path)) return result;
        List<?> raw = config.getList(path);
        if (raw == null) return result;
        for (Object entry : raw) {
            if (entry instanceof ItemStack) {
                result.add((ItemStack) entry);
            } else if (entry instanceof Map) {
                try {
                    Object deserialized = ConfigurationSerialization.deserializeObject((Map<String, Object>) entry);
                    if (deserialized instanceof ItemStack) {
                        result.add((ItemStack) deserialized);
                    }
                } catch (Exception ignored) {}
            }
        }
        return result;
    }
}
