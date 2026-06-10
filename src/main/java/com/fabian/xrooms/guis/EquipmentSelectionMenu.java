package com.fabian.xrooms.guis;

import com.cryptomorin.xseries.XMaterial;
import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;

public class EquipmentSelectionMenu extends Menu {

    private final Room room;

    public EquipmentSelectionMenu(Room room) {
        this.room = room;
    }

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessageRaw("gui-equipment-selection");
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        
        switch (e.getSlot()) {
            case 11: // View
                new EquipmentViewMenu(room, false).open(p);
                break;
            case 15: // Edit
                new StorageMenu(room, "equipment").open(p);
                break;
            case 22: // Back
                new RoomEditMenu(room).open(p);
                break;
        }
    }

    @Override
    public void setMenuItems() {
        DebugLogger.debug("EquipmentSelectionMenu", "Opening equipment selection for room " + room.getName());
        fillBorders(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial());

        inventory.setItem(11, createItem(XMaterial.BOOK.parseMaterial(), 
                plugin.getConfigManager().getMessageRaw("gui-view-equipment"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-view-equipment-lore"))));
        
        inventory.setItem(15, createItem(XMaterial.ANVIL.parseMaterial(), 
                plugin.getConfigManager().getMessageRaw("gui-edit-equipment"), 
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-edit-equipment-lore"))));

        inventory.setItem(22, createItem(XMaterial.ARROW.parseMaterial(), 
                plugin.getConfigManager().getMessageRaw("gui-back"), null));
    }
}
