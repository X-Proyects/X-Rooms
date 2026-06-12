package com.fabian.xrooms.menus;

import com.cryptomorin.xseries.XMaterial;
import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EquipmentViewMenu extends Menu {

    private final Room room;
    private final boolean fromCommand;

    public EquipmentViewMenu(Room room, boolean fromCommand) {
        this.room = room;
        this.fromCommand = fromCommand;
    }

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessageRaw("edit-gui-title").replace("{name}", room.getDisplayName());
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getSlot() == 49) {
            if (fromCommand) {
                ((org.bukkit.entity.Player) e.getWhoClicked()).closeInventory();
            } else {
                new EquipmentSelectionMenu(room).open((org.bukkit.entity.Player) e.getWhoClicked());
            }
        }
    }

    @Override
    public void setMenuItems() {
        DebugLogger.debug("EquipmentViewMenu", "Opening equipment view for room " + room.getName());
        // Decoration: Black glass at the bottom only as requested
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, createItem(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial(), " ", null));
        }

        List<ItemStack> equipment = room.getEquipment();
        if (equipment != null) {
            for (int i = 0; i < Math.min(equipment.size(), 45); i++) {
                inventory.setItem(i, equipment.get(i));
            }
        }

        inventory.setItem(49, createItem(XMaterial.BARRIER.parseMaterial(), 
                plugin.getConfigManager().getMessageRaw("gui-close"), null));
    }
}
