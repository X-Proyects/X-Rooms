package com.fabian.xrooms.menus;

import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageMenu extends Menu {

    private final Room room;
    private final String type; // "rewards" or "equipment"
    private final boolean fromRewards;

    public StorageMenu(Room room, String type) {
        this(room, type, false);
    }

    public StorageMenu(Room room, String type, boolean fromRewards) {
        this.room = room;
        this.type = type;
        this.fromRewards = fromRewards;
    }

    @Override
    public String getMenuName() {
        String titleKey = type.equalsIgnoreCase("rewards") ? "gui-edit-rewards-title" : "gui-edit-equipment-title";
        return plugin.getConfigManager().getMessageRaw(titleKey).replace("{name}", room.getName());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getSlot() == 45) { // Cancel
            p.closeInventory();
            if (fromRewards) {
                new RoomEditMenu(room).open(p);
            } else {
                new EquipmentSelectionMenu(room).open(p);
            }
        } else if (e.getSlot() == 53) { // Save
            save(e.getInventory());
            p.closeInventory();
            p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
            if (fromRewards) {
                new RoomEditMenu(room).open(p);
            } else {
                new EquipmentSelectionMenu(room).open(p);
            }
        }
    }

    @Override
    public void setMenuItems() {
        DebugLogger.debug("StorageMenu", "Opening storage menu (type=" + type + ") for room " + room.getName());
        // Bottom decoration
        for (int i = 46; i < 53; i++) {
            inventory.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null));
        }

        List<ItemStack> items = type.equalsIgnoreCase("rewards") ? room.getRewards() : room.getEquipment();
        if (items != null) {
            for (int i = 0; i < Math.min(items.size(), 45); i++) {
                inventory.setItem(i, items.get(i));
            }
        }

        inventory.setItem(45, createItem(Material.RED_WOOL, 
                plugin.getConfigManager().getMessageRaw("gui-button-cancel"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-button-cancel-lore"))));
                
        inventory.setItem(53, createItem(Material.LIME_WOOL, 
                plugin.getConfigManager().getMessageRaw("gui-button-save"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-button-save-lore"))));
    }

    @Override
    public boolean cancelClick() {
        return false;
    }

    private void save(org.bukkit.inventory.Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
            }
            // Clear the editable slot so that the subsequent InventoryCloseEvent ->
            // handleClose() handler does not re-duplicate these items back into
            // the player's inventory (they have just been persisted to the room).
            inv.setItem(i, null);
        }

        if (type.equalsIgnoreCase("rewards")) {
            room.setRewards(items);
        } else {
            room.setEquipment(items);
        }

        plugin.getRoomManager().saveRoom(room);
    }

    @Override
    public void handleClose(InventoryCloseEvent e) {
        // C8: When the player closes the menu WITHOUT clicking SAVE, the items they
        // placed into the editable slots (0..44) would otherwise be silently lost
        // (the orphan Inventory is GC'd along with its contents). Return every
        // non-AIR item to the player's inventory; drop overflow at the player's
        // location so nothing is destroyed. The SAVE button still calls `save(...)`
        // explicitly before close, which clears those slots — so this is a no-op
        // when the player saves, and a safety net when they cancel/ESC.
        if (!(e.getPlayer() instanceof org.bukkit.entity.Player)) {
            return;
        }
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getPlayer();
        org.bukkit.inventory.Inventory inv = e.getInventory();
        for (int i = 0; i < 45; i++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(i);
            if (item == null) continue;
            if (item.getType() == org.bukkit.Material.AIR) continue;
            java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = p.getInventory().addItem(item);
            for (org.bukkit.inventory.ItemStack leftover : overflow.values()) {
                if (leftover == null || leftover.getType() == org.bukkit.Material.AIR) continue;
                p.getWorld().dropItemNaturally(p.getLocation(), leftover);
            }
            inv.setItem(i, null);
        }
    }
}