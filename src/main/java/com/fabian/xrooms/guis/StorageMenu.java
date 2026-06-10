package com.fabian.xrooms.guis;

import com.cryptomorin.xseries.XMaterial;
import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
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

    public StorageMenu(Room room, String type) {
        this.room = room;
        this.type = type;
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
            new EquipmentSelectionMenu(room).open(p);
        } else if (e.getSlot() == 53) { // Save
            save(e.getInventory());
            p.closeInventory();
            p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
            new EquipmentSelectionMenu(room).open(p);
        }
    }

    @Override
    public void setMenuItems() {
        DebugLogger.debug("StorageMenu", "Opening storage menu (type=" + type + ") for room " + room.getName());
        // Bottom decoration
        for (int i = 46; i < 53; i++) {
            inventory.setItem(i, createItem(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial(), " ", null));
        }

        List<ItemStack> items = type.equalsIgnoreCase("rewards") ? room.getRewards() : room.getEquipment();
        if (items != null) {
            for (int i = 0; i < Math.min(items.size(), 45); i++) {
                inventory.setItem(i, items.get(i));
            }
        }

        inventory.setItem(45, createItem(XMaterial.RED_WOOL.parseMaterial(), 
                plugin.getConfigManager().getMessageRaw("gui-button-cancel"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-button-cancel-lore"))));
                
        inventory.setItem(53, createItem(XMaterial.LIME_WOOL.parseMaterial(), 
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
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                items.add(item);
            }
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
        // We handle saving via the SAVE button now to avoid accidental saves
    }
}
