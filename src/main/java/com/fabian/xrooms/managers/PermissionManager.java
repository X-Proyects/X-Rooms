package com.fabian.xrooms.managers;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.entity.Player;

public class PermissionManager {

    public PermissionManager(XRooms plugin) {
    }

    public boolean hasAdmin(Player player) {
        return player.hasPermission("xrooms.admin.*") || player.isOp();
    }

    public boolean canCreate(Player player) {
        return player.hasPermission("xrooms.admin.create") || hasAdmin(player);
    }

    public boolean canDelete(Player player) {
        return player.hasPermission("xrooms.admin.delete") || hasAdmin(player);
    }

    public boolean canEdit(Player player) {
        return player.hasPermission("xrooms.admin.edit") || hasAdmin(player);
    }

    public boolean canOpenGui(Player player) {
        return player.hasPermission("xrooms.admin.gui") || hasAdmin(player);
    }

    public boolean canTp(Player player) {
        return player.hasPermission("xrooms.admin.tp") || hasAdmin(player);
    }

    public boolean canEnter(Player player, com.fabian.xrooms.models.Room room) {
        if (!room.isRequirePermission()) return true;
        if (hasAdmin(player)) return true;

        // Check custom permission if set
        if (room.getPermission() != null && !room.getPermission().isEmpty()) {
            boolean has = player.hasPermission(room.getPermission());
            DebugLogger.debug("PermissionManager", "canEnter: player=" + player.getName() + " room=" + room.getName() + " customPerm=" + has);
            if (has) return true;
        }

        // Default permission
        boolean has = player.hasPermission("xrooms.player." + room.getName().toLowerCase());
        DebugLogger.debug("PermissionManager", "canEnter: player=" + player.getName() + " room=" + room.getName() + " defaultPerm=" + has);
        return has;
    }
}
