package com.fabian.xrooms.menus;

import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;

public class EntryTitleMenu extends Menu {

    private final Room room;

    public EntryTitleMenu(Room room) {
        this.room = room;
    }

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessageRaw("gui-entry-title-menu-title").replace("{name}", room.getDisplayName());
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        DebugLogger.debug("EntryTitleMenu", p.getName() + " clicked slot " + e.getSlot() + " for room " + room.getName());

        switch (e.getSlot()) {
            case 11: // Edit Title
                plugin.getChatInputManager().requestInput(p, plugin.getConfigManager().getMessageRaw("chat-entry-title-prompt"), input -> {
                    room.setEntryTitle(input);
                    plugin.getRoomManager().saveRoom(room);
                    p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                    new EntryTitleMenu(room).open(p);
                }, () -> new EntryTitleMenu(room).open(p));
                break;
            case 13: // Edit Subtitle
                plugin.getChatInputManager().requestInput(p, plugin.getConfigManager().getMessageRaw("chat-entry-subtitle-prompt"), input -> {
                    room.setEntrySubtitle(input);
                    plugin.getRoomManager().saveRoom(room);
                    p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                    new EntryTitleMenu(room).open(p);
                }, () -> new EntryTitleMenu(room).open(p));
                break;
            case 15: // Reset to defaults
                room.setEntryTitle("&b&l{name}");
                room.setEntrySubtitle("");
                plugin.getRoomManager().saveRoom(room);
                p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                new EntryTitleMenu(room).open(p);
                break;
            case 22: // Back
                new RoomEditMenu(room).open(p);
                break;
        }
    }

    @Override
    public void setMenuItems() {
        DebugLogger.debug("EntryTitleMenu", "Opening entry title menu for room " + room.getName());
        fillBorders(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        inventory.setItem(11, createItem(Material.OAK_SIGN,
                plugin.getConfigManager().getMessageRaw("gui-entry-title"),
                Arrays.asList(
                        plugin.getConfigManager().getMessageRaw("gui-current-value").replace("{value}",
                                room.getEntryTitle().replace("{name}", room.getDisplayName())),
                        "",
                        plugin.getConfigManager().getMessageRaw("gui-click-to-edit")
                )));

        inventory.setItem(13, createItem(Material.BIRCH_SIGN,
                plugin.getConfigManager().getMessageRaw("gui-entry-subtitle"),
                Arrays.asList(
                        plugin.getConfigManager().getMessageRaw("gui-current-value").replace("{value}",
                                room.getEntrySubtitle().isEmpty() ? "&7(None)" : room.getEntrySubtitle()),
                        "",
                        plugin.getConfigManager().getMessageRaw("gui-click-to-edit")
                )));

        inventory.setItem(15, createItem(Material.BARRIER,
                plugin.getConfigManager().getMessageRaw("gui-entry-reset"),
                Arrays.asList(plugin.getConfigManager().getMessageRaw("gui-entry-reset-lore"))));

        inventory.setItem(22, createItem(Material.ARROW,
                plugin.getConfigManager().getMessageRaw("gui-back"), null));
    }
}