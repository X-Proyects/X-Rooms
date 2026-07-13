package com.fabian.xrooms.listeners;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.models.RoomState;
import com.fabian.xrooms.utils.DebugLogger;
import com.fabian.xrooms.utils.WorldEditUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoomListener implements Listener {

    private final XRooms plugin;
    private final Map<UUID, Room> currentRooms = new HashMap<>();
    
    // Internal Timers
    private final Map<Room, Integer> startTimers = new HashMap<>();
    private final Map<Room, Integer> roomTimers = new HashMap<>();
    
    // Title cooldowns: key = "UUID:roomName", value = timestamp
    private final Map<String, Long> titleCooldowns = new HashMap<>();

    public RoomListener(XRooms plugin) {
        this.plugin = plugin;
        startGlobalTask();
    }

    private void startGlobalTask() {
        plugin.getXScheduler().runTimer(() -> {
            for (Room room : plugin.getRoomManager().getAllRooms()) {
                if (room.getState() == RoomState.STARTING) {
                    int left = startTimers.getOrDefault(room, plugin.getConfigManager().getStartCountdown());
                    if (left <= 0) {
                        // Start PVP
                        room.setState(RoomState.ACTIVE);
                        DebugLogger.debug("RoomListener", "Room " + room.getName() + " state -> ACTIVE");
                        startTimers.remove(room);
                        roomTimers.put(room, room.getPvpDuration());
                        
                        for (UUID id : room.getAlivePlayers()) {
                            Player p = Bukkit.getPlayer(id);
                            if (p != null) {
                                p.sendTitle(plugin.getConfigManager().getMessageRaw("title-fight"), "", 5, 20, 5);
                                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                                
                                if (!plugin.getConfigManager().isUseOwnInventory()) {
                                    plugin.getInventoryManager().saveAndClearInventory(p);
                                    for (ItemStack item : room.getEquipment()) {
                                        if (item != null) p.getInventory().addItem(item);
                                    }
                                }
                                applyEffects(p, room);
                            }
                        }
                    } else {
                        // Countdown
                        for (UUID id : room.getAlivePlayers()) {
                            Player p = Bukkit.getPlayer(id);
                            if (p != null) {
                                p.sendTitle(plugin.getConfigManager().getMessageRaw("title-countdown").replace("{time}", String.valueOf(left)), "", 5, 20, 5);
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                            }
                        }
                        startTimers.put(room, left - 1);
                    }
                } else if (room.getState() == RoomState.ACTIVE && room.getPvpDuration() > 0) {
                    int left = roomTimers.getOrDefault(room, room.getPvpDuration());
                    if (left <= 0) {
                        endRoom(room);
                    } else {
                        String msg = plugin.getConfigManager().getMessage("actionbar-pvp-ends").replace("{time}", String.valueOf(left));
                        for (UUID id : room.getAlivePlayers()) {
                            Player p = Bukkit.getPlayer(id);
                            if (p != null) {
                                sendActionBar(p, msg);
                            }
                        }
                        roomTimers.put(room, left - 1);
                    }
                }
            }
        }, 20L, 20L);
    }

    private void sendActionBar(Player p, String msg) {
        String colored = ChatColor.translateAlternateColorCodes('&', msg);
        try {
            // Try Paper API first (modern servers)
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Object component = Class.forName("net.kyori.adventure.text.TextComponent")
                    .getMethod("text", String.class).invoke(null, colored);
            p.getClass().getMethod("sendActionBar", componentClass).invoke(p, component);
        } catch (Exception e) {
            try {
                // Spigot 1.16.1+ : ChatMessageType.ACTION_BAR
                Object chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType")
                        .getEnumConstants()[3]; // ACTION_BAR
                Object textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent")
                        .getConstructor(String.class).newInstance(colored);
                p.getClass().getMethod("sendActionBar", chatMessageType.getClass(), 
                        Class.forName("net.md_5.bungee.api.chat.BaseComponent[]"))
                        .invoke(p, chatMessageType, new Object[]{textComponent});
            } catch (Exception ignored) {
                // 1.13-1.15.2: action bar not available, skip
            }
        }
    }

    private void playSound(Player p, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName);
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            DebugLogger.debug("RoomListener", "Unknown sound: " + soundName);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
            e.getFrom().getBlockY() == e.getTo().getBlockY() &&
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        handleMovement(e.getPlayer(), e.getTo(), e);
    }

    @EventHandler
    public void onTeleport(org.bukkit.event.player.PlayerTeleportEvent e) {
        handleMovement(e.getPlayer(), e.getTo(), e);
    }

    private void handleMovement(Player p, org.bukkit.Location to, org.bukkit.event.Cancellable e) {
        Room room = plugin.getRoomManager().getRoomAt(to);
        Room previous = currentRooms.get(p.getUniqueId());

        if (room != null && previous == null) {
            // Trying to enter
            DebugLogger.debug("RoomListener", p.getName() + " entering room " + room.getName() + " (state=" + room.getState() + ")");
            if (room.getState() != RoomState.WAITING) {
                p.sendMessage(plugin.getConfigManager().getMessage("pvp-already-started"));
                e.setCancelled(true);
                return;
            }
            if (!plugin.getPermissionManager().canEnter(p, room)) {
                p.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                e.setCancelled(true);
                return;
            }
            if (room.getAlivePlayers().size() >= room.getMaxPlayers()) {
                p.sendMessage(plugin.getConfigManager().getMessage("room-full"));
                e.setCancelled(true);
                return;
            }
            enterRoomRegion(p, room);
        } else if (room == null && previous != null) {
            // Trying to leave
            DebugLogger.debug("RoomListener", p.getName() + " leaving room " + previous.getName() + " (state=" + previous.getState() + ")");
            if (previous.getState() != RoomState.WAITING) {
                p.sendMessage(plugin.getConfigManager().getMessage("cannot-escape"));
                e.setCancelled(true);
                return;
            }
            leaveRoomRegion(p, previous);
        } else if (room != null && !room.getName().equals(previous.getName())) {
            // Switched rooms directly (adjacent regions)
            if (previous.getState() != RoomState.WAITING || room.getState() != RoomState.WAITING) {
                e.setCancelled(true);
                return;
            }
            leaveRoomRegion(p, previous);
            enterRoomRegion(p, room);
        }
    }

    private void enterRoomRegion(Player p, Room room) {
        currentRooms.put(p.getUniqueId(), room);
        room.getAlivePlayers().add(p.getUniqueId());
        DebugLogger.debug("RoomListener", p.getName() + " entered room " + room.getName() + " (players=" + room.getAlivePlayers().size() + "/" + room.getMaxPlayers() + ")");
        // Title cooldown check
        int cooldown = plugin.getConfigManager().getEntryTitleCooldown();
        boolean showTitle = true;
        if (cooldown > 0) {
            String key = p.getUniqueId() + ":" + room.getName();
            long now = System.currentTimeMillis();
            Long lastShown = titleCooldowns.get(key);
            if (lastShown != null && (now - lastShown) < (cooldown * 1000L)) {
                showTitle = false;
            } else {
                titleCooldowns.put(key, now);
            }
        }
        
        if (showTitle) {
            String title = plugin.getConfigManager().color(room.getEntryTitle().replace("{name}", room.getDisplayName()));
            String subtitle = plugin.getConfigManager().color(room.getEntrySubtitle());
            p.sendTitle(title, subtitle, 10, 40, 10);
            playSound(p, room.getEntrySound());
        }
        
        // Start check
        if (room.getAlivePlayers().size() >= room.getMinPlayers()) {
            room.setState(RoomState.STARTING);
            startTimers.put(room, plugin.getConfigManager().getStartCountdown());
        }
    }

    private void leaveRoomRegion(Player p, Room room) {
        currentRooms.remove(p.getUniqueId());
        room.getAlivePlayers().remove(p.getUniqueId());
        DebugLogger.debug("RoomListener", p.getName() + " left room " + room.getName() + " (players=" + room.getAlivePlayers().size() + ")");
        removePotion(p, "INCREASE_DAMAGE", "STRENGTH");
        removePotion(p, "SPEED", "SPEED");
        removePotion(p, "DAMAGE_RESISTANCE", "RESISTANCE");
        removePotion(p, "FAST_DIGGING", "HASTE");
        removePotion(p, "REGENERATION", "REGENERATION");
        removePotion(p, "FIRE_RESISTANCE", "FIRE_RESISTANCE");
        removePotion(p, "JUMP", "JUMP");
        removePotion(p, "SATURATION", "SATURATION");
        removePotion(p, "ABSORPTION", "ABSORPTION");

        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }

    private void applyEffects(Player p, Room room) {
        Map<String, Integer> ab = room.getAbilities();
        if (ab.getOrDefault("strength", 0) > 0) applyPotion(p, "INCREASE_DAMAGE", "STRENGTH", ab.get("strength") - 1);
        if (ab.getOrDefault("speed", 0) > 0) applyPotion(p, "SPEED", "SPEED", ab.get("speed") - 1);
        if (ab.getOrDefault("resistance", 0) > 0) applyPotion(p, "DAMAGE_RESISTANCE", "RESISTANCE", ab.get("resistance") - 1);
        if (ab.getOrDefault("haste", 0) > 0) applyPotion(p, "FAST_DIGGING", "HASTE", ab.get("haste") - 1);
        if (ab.getOrDefault("regeneration", 0) > 0) applyPotion(p, "REGENERATION", "REGENERATION", ab.get("regeneration") - 1);
        if (ab.getOrDefault("fire_resistance", 0) > 0) applyPotion(p, "FIRE_RESISTANCE", "FIRE_RESISTANCE", ab.get("fire_resistance") - 1);
        if (ab.getOrDefault("jump", 0) > 0) applyPotion(p, "JUMP", "JUMP", ab.get("jump") - 1);
        if (ab.getOrDefault("saturation", 0) > 0) applyPotion(p, "SATURATION", "SATURATION", ab.get("saturation") - 1);
        if (ab.getOrDefault("absorption", 0) > 0) applyPotion(p, "ABSORPTION", "ABSORPTION", ab.get("absorption") - 1);
        if (ab.containsKey("fly") && ab.get("fly") > 0) p.setAllowFlight(true);
    }

    @SuppressWarnings("deprecation")
    private void applyPotion(Player p, String legacyName, String modernName, int amplifier) {
        org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(modernName);
        if (type == null) type = org.bukkit.potion.PotionEffectType.getByName(legacyName);
        if (type != null) p.addPotionEffect(new org.bukkit.potion.PotionEffect(type, Integer.MAX_VALUE, amplifier));
    }

    @SuppressWarnings("deprecation")
    private void removePotion(Player p, String legacyName, String modernName) {
        org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(modernName);
        if (type == null) type = org.bukkit.potion.PotionEffectType.getByName(legacyName);
        if (type != null) p.removePotionEffect(type);
    }

    private void endRoom(Room room) {
        room.setState(RoomState.ENDING);
        DebugLogger.debug("RoomListener", "Room " + room.getName() + " state -> ENDING (players alive=" + room.getAlivePlayers().size() + ")");
        roomTimers.remove(room);
        startTimers.remove(room);

        List<UUID> clone = new java.util.ArrayList<>(room.getAlivePlayers());
        boolean isWinner = clone.size() == 1;

        for (UUID id : clone) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.sendMessage(plugin.getConfigManager().getMessage("pvp-ended"));
            }
        }

        long delayTicks = plugin.getConfigManager().getEndDelay() * 20L;
        
        plugin.getXScheduler().runTaskLater(() -> {
            if (room.isSchematicEnabled()) {
                WorldEditUtils.pasteSchematic(plugin, room);
            }
            room.setState(RoomState.WAITING);
            DebugLogger.debug("RoomListener", "Room " + room.getName() + " state -> WAITING");
            room.getAlivePlayers().clear();
            
            for (UUID id : clone) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && currentRooms.get(p.getUniqueId()) == room) {
                    leaveRoomRegion(p, room);
                    if (!plugin.getConfigManager().isUseOwnInventory()) {
                        plugin.getInventoryManager().restoreInventory(p);
                    }
                    
                    // If there is only 1 person, it's the winner -> they stay OR go to winnerLocation.
                    // If there are more (tie/timeout), they go to spawn.
                    if (isWinner) {
                        if (room.getWinnerLocation() != null) {
                            plugin.getXScheduler().teleport(p, room.getWinnerLocation());
                        }
                    } else {
                        plugin.getXScheduler().teleport(p, p.getWorld().getSpawnLocation());
                    }
                }
            }
        }, delayTicks);
    }

    private void checkEndGame(Room room) {
        if (room.getState() == RoomState.ACTIVE && room.getAlivePlayers().size() <= 1) {
            endRoom(room);
        } else if (room.getState() == RoomState.STARTING && room.getAlivePlayers().size() < room.getMinPlayers()) {
            // If someone disconnected during starting countdown and we dipped below min-players
            room.setState(RoomState.WAITING);
            DebugLogger.debug("RoomListener", "Room " + room.getName() + " state -> WAITING (below min players during start)");
            startTimers.remove(room);
            List<UUID> clone = new java.util.ArrayList<>(room.getAlivePlayers());
            for (UUID id : clone) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.sendMessage(plugin.getConfigManager().getMessage("pvp-cancelled"));
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Room room = currentRooms.get(p.getUniqueId());
        if (room != null && room.getState() != RoomState.WAITING) {
            // Player reconnected during an active/ending match — they already left, restore inventory
            DebugLogger.debug("RoomListener", p.getName() + " rejoined while room " + room.getName() + " was in state " + room.getState() + " — cleaning up");
            currentRooms.remove(p.getUniqueId());
            if (!plugin.getConfigManager().isUseOwnInventory()) {
                plugin.getInventoryManager().restoreInventory(p);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Room room = currentRooms.get(p.getUniqueId());
        if (room != null) {
            DebugLogger.debug("RoomListener", p.getName() + " quit while in room " + room.getName() + " (state=" + room.getState() + ")");
            room.getAlivePlayers().remove(p.getUniqueId());
            if (room.getState() != RoomState.WAITING) {
                // Combat log
                if (!plugin.getConfigManager().isUseOwnInventory()) {
                    if (plugin.getConfigManager().isRestoreOnQuit()) {
                        plugin.getInventoryManager().restoreInventory(p);
                    } else {
                        plugin.getInventoryManager().deleteBackup(p);
                    }
                }
                checkEndGame(room);
            } else {
                leaveRoomRegion(p, room);
            }
            // Player is disconnecting, teleporting is pointless — just clean up the map
            currentRooms.remove(p.getUniqueId());
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Room room = currentRooms.get(victim.getUniqueId());
        if (room != null && room.getState() == RoomState.ACTIVE) {
            // Clear drops so room kits aren't duped
            if (!plugin.getConfigManager().isUseOwnInventory()) {
                e.getDrops().clear();
            }
            
            Player killer = victim.getKiller();
            if (killer != null) {
                playSound(killer, room.getKillSound());
            }
            
            room.getAlivePlayers().remove(victim.getUniqueId());
            checkEndGame(room);
        }
    }
    
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Room room = currentRooms.get(p.getUniqueId());
        if (room != null) {
            leaveRoomRegion(p, room); // cleans up effects and lists
            if (!plugin.getConfigManager().isUseOwnInventory()) {
                plugin.getInventoryManager().restoreInventory(p);
            }
            e.setRespawnLocation(p.getWorld().getSpawnLocation());
            currentRooms.remove(p.getUniqueId());
        }
    }

    @EventHandler
    public void onDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        Room room = currentRooms.get(p.getUniqueId());
        if (room != null && room.getState() == RoomState.ENDING) {
            e.setCancelled(true);
        }
    }
}