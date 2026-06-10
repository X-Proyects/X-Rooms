package com.fabian.xrooms.models;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.UUID;

@Data
public class Room {
    private final String name;
    private String displayName;
    private String permission;
    private boolean requirePermission = false;
    private int minPlayers = 2;
    private int maxPlayers = 10;
    private int pvpDuration = -1; // -1 = Unlimited
    
    // State
    private RoomState state = RoomState.WAITING;
    private List<UUID> alivePlayers = new ArrayList<>();
    
    // Region
    private String worldName;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;

    // Hologram
    private org.bukkit.Location hologramLocation;
    private List<String> hologramLines = new java.util.ArrayList<>();
    private boolean hologramEnabled = true;
    
    // Schematics
    private boolean schematicEnabled = false;

    // Sounds (XSeries sound names)
    private String entrySound = "ENTITY_PLAYER_LEVELUP";
    private String killSound = "ENTITY_EXPERIENCE_ORB_PICKUP";
    
    // Visuals
    private String entryTitle = "&b&l{name}";
    private String entrySubtitle = "&fWelcome to the room!";
    
    // Teleport Locations
    private org.bukkit.Location winnerLocation;
    
    // Data
    private List<ItemStack> rewards = new ArrayList<>();
    private List<ItemStack> equipment = new ArrayList<>();
    private Map<String, Integer> abilities = new HashMap<>(); // name -> level (0 = off)

    public Room(String name) {
        this.name = name;
        this.displayName = name;
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) return false;
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}
