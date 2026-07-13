package com.fabian.xrooms.menus;

import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;

public class RoomEditMenu extends Menu {

    private final Room room;

    public RoomEditMenu(Room room) {
        this.room = room;
    }

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessageRaw("edit-gui-title").replace("{name}", room.getDisplayName());
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        DebugLogger.debug("RoomEditMenu", p.getName() + " clicked slot " + e.getSlot() + " in edit menu for room " + room.getName());
        
        switch (e.getSlot()) {
            case 10: // Rename
                plugin.getChatInputManager().requestInput(p, plugin.getConfigManager().getMessageRaw("chat-rename-prompt"), input -> {
                    room.setDisplayName(input);
                    plugin.getRoomManager().saveRoom(room);
                    p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                    this.open(p);
                }, () -> this.open(p));
                break;
            case 12: // Permission
                p.sendMessage(plugin.getConfigManager().getMessage("gui-perm-info")
                        .replace("{perm}", "xrooms.player." + room.getName().toLowerCase()));
                break;
            case 14: // Rewards
                new StorageMenu(room, "rewards", true).open(p);
                break;
            case 16: // Equipment Selection (New)
                new EquipmentSelectionMenu(room).open(p);
                break;
            case 20: // Entry Sound
                plugin.getChatInputManager().requestInput(p, plugin.getConfigManager().getMessageRaw("chat-entry-sound-prompt"), input -> {
                    room.setEntrySound(input.toUpperCase());
                    plugin.getRoomManager().saveRoom(room);
                    p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                    this.open(p);
                }, () -> this.open(p));
                break;
            case 22: // Kill Sound
                plugin.getChatInputManager().requestInput(p, plugin.getConfigManager().getMessageRaw("chat-kill-sound-prompt"), input -> {
                    room.setKillSound(input.toUpperCase());
                    plugin.getRoomManager().saveRoom(room);
                    p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                    this.open(p);
                }, () -> this.open(p));
                break;
            case 24: // Update Region (Actual Logic)
                com.fabian.xrooms.utils.WorldEditUtils.RegionSelection sel = com.fabian.xrooms.utils.WorldEditUtils.getSelection(p);
                if (sel == null) {
                    p.sendMessage(plugin.getConfigManager().getMessage("no-selection"));
                } else {
                    room.setWorldName(sel.world);
                    room.setMinX(Math.min(sel.min.getX(), sel.max.getX()));
                    room.setMinY(Math.min(sel.min.getY(), sel.max.getY()));
                    room.setMinZ(Math.min(sel.min.getZ(), sel.max.getZ()));
                    room.setMaxX(Math.max(sel.min.getX(), sel.max.getX()));
                    room.setMaxY(Math.max(sel.min.getY(), sel.max.getY()));
                    room.setMaxZ(Math.max(sel.min.getZ(), sel.max.getZ()));
                    plugin.getRoomManager().saveRoom(room);
                    p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                }
                break;
            case 28: // PVP Duration
                p.sendMessage(plugin.getConfigManager().getMessage("gui-current-value").replace("{value}", 
                        room.getPvpDuration() == -1 ? "∞" : room.getPvpDuration() + "s"));
                p.sendMessage(plugin.getConfigManager().getMessageRaw("pvp-duration-info").replace("{room}", room.getName()));
                break;
            case 30: // Abilities
                new AbilitiesMenu(room).open(p);
                break;
            case 32: // Winner Location
                room.setWinnerLocation(p.getLocation());
                plugin.getRoomManager().saveRoom(room);
                p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                new RoomEditMenu(room).open(p); // Refresh
                break;
            case 34: // Entry Title & Subtitle Menu
                new EntryTitleMenu(room).open(p);
                break;
            case 40: // Back
                new RoomsMenu().open(p);
                break;
        }
    }

    @Override
    public void setMenuItems() {
        fillBorders(Material.CYAN_STAINED_GLASS_PANE);

        inventory.setItem(10, createItem(Material.NAME_TAG, 
                plugin.getConfigManager().getMessageRaw("gui-rename"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-current-value").replace("{value}", room.getDisplayName()))));
        
        inventory.setItem(12, createItem(Material.PAPER, 
                plugin.getConfigManager().getMessageRaw("gui-permission"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-perm-info").replace("{perm}", "xrooms.player." + room.getName().toLowerCase()))));
        
        inventory.setItem(14, createItem(Material.GOLD_INGOT, 
                plugin.getConfigManager().getMessageRaw("gui-rewards"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-click-to-edit"))));
        
        inventory.setItem(16, createItem(Material.DIAMOND_SWORD, 
                plugin.getConfigManager().getMessageRaw("gui-equipment"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-click-to-edit"))));
        
        inventory.setItem(20, createItem(Material.NOTE_BLOCK, 
                plugin.getConfigManager().getMessageRaw("gui-sounds-entry"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-current-value").replace("{value}", room.getEntrySound()))));
        
        inventory.setItem(22, createItem(Material.SKELETON_SKULL, 
                plugin.getConfigManager().getMessageRaw("gui-sounds-kill"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-current-value").replace("{value}", room.getKillSound()))));
        
        inventory.setItem(24, createItem(Material.COMPASS, 
                plugin.getConfigManager().getMessageRaw("gui-update-region"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-click-to-edit"))));

        inventory.setItem(28, createItem(Material.CLOCK, 
                plugin.getConfigManager().getMessageRaw("gui-pvp-duration"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-current-value").replace("{value}", 
                        room.getPvpDuration() == -1 ? "∞" : room.getPvpDuration() + "s"))));

        // Abilities summary
        int activeCount = (int) room.getAbilities().values().stream().filter(v -> v > 0).count();
        inventory.setItem(30, createItem(Material.BREWING_STAND, 
                plugin.getConfigManager().getMessageRaw("gui-abilities"), 
                Arrays.asList(
                    plugin.getConfigManager().getMessageRaw("gui-abilities-active").replace("{count}", String.valueOf(activeCount)),
                    "",
                    plugin.getConfigManager().getMessageRaw("gui-click-to-edit")
                )));

        inventory.setItem(32, createItem(Material.IRON_DOOR, 
                plugin.getConfigManager().getMessageRaw("gui-winner-location"), 
                Arrays.asList(
                    plugin.getConfigManager().getMessageRaw("gui-winner-location-lore"),
                    "",
                    room.getWinnerLocation() != null ? 
                        plugin.getConfigManager().getMessageRaw("gui-winner-set") : 
                        plugin.getConfigManager().getMessageRaw("gui-winner-not-set")
                )));
        
        inventory.setItem(34, createItem(Material.OAK_SIGN, 
                plugin.getConfigManager().getMessageRaw("gui-entry-titles-editor"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-click-to-edit"))));

        inventory.setItem(40, createItem(Material.ARROW, 
                plugin.getConfigManager().getMessageRaw("gui-back"), null));
    }
}