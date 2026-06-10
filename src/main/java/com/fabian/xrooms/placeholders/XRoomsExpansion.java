package com.fabian.xrooms.placeholders;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class XRoomsExpansion extends PlaceholderExpansion {

    private final XRooms plugin;

    public XRoomsExpansion(XRooms plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Fabian";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "xroom";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        DebugLogger.debug("XRoomsExpansion", "Placeholder request: %xroom_" + params + (player != null ? " (player=" + player.getName() + ")" : ""));
        // %xroom_maxplayers_<room>%
        // %xroom_minplayers_<room>%
        
        if (params.startsWith("maxplayers_")) {
            String roomName = params.substring(11);
            Room room = plugin.getRoomManager().getRoom(roomName);
            String result = room != null ? String.valueOf(room.getMaxPlayers()) : "0";
            DebugLogger.debug("XRoomsExpansion", "  -> maxplayers_" + roomName + " = " + result);
            return result;
        }

        if (params.startsWith("minplayers_")) {
            String roomName = params.substring(11);
            Room room = plugin.getRoomManager().getRoom(roomName);
            return room != null ? String.valueOf(room.getMinPlayers()) : "0";
        }
        
        if (params.startsWith("players_")) {
            String roomName = params.substring(8);
            Room room = plugin.getRoomManager().getRoom(roomName);
            return room != null ? String.valueOf(plugin.getRoomManager().countPlayersInRoom(room)) : "0";
        }

        if (params.startsWith("time_")) {
            String roomName = params.substring(5);
            Room room = plugin.getRoomManager().getRoom(roomName);
            if (room == null) return "0";
            if (room.getPvpDuration() == -1) return "∞";
            // Logic for active countdown could go here
            return room.getPvpDuration() + "s";
        }

        return null;
    }
}
