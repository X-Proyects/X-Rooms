package com.fabian.xrooms.menus;

import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RoomsMenu extends Menu {

    private final Map<Integer, Room> roomSlots = new HashMap<>();

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessageRaw("rooms-gui-title");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        
        if (e.getSlot() == 49) {
            p.closeInventory();
            return;
        }

        if (e.getCurrentItem().getType() == Material.CHEST) {
            Room room = roomSlots.get(e.getSlot());
            if (room != null) {
                new RoomEditMenu(room).open(p);
            }
        }
    }

    @Override
    public void setMenuItems() {
        DebugLogger.debug("RoomsMenu", "Opening rooms menu with " + plugin.getRoomManager().getAllRooms().size() + " room(s)");
        fillBorders(Material.GRAY_STAINED_GLASS_PANE);

        Collection<Room> rooms = plugin.getRoomManager().getAllRooms();
        int slot = 10;
        for (Room room : rooms) {
            if (slot == 17 || slot == 26 || slot == 35 || slot == 44) slot += 2;
            if (slot > 43) break;

            inventory.setItem(slot, createItem(Material.CHEST, 
                    "&b" + room.getDisplayName(), 
                    Arrays.asList(
                        plugin.getConfigManager().getMessageRaw("gui-room-id").replace("{id}", room.getName()),
                        "",
                        plugin.getConfigManager().getMessageRaw("gui-item-lore")
                    )));
            roomSlots.put(slot, room);
            slot++;
        }

        inventory.setItem(49, createItem(Material.BARRIER, 
                plugin.getConfigManager().getMessageRaw("gui-close"), null));
    }
}