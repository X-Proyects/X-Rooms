package com.fabian.xrooms.listeners;

import com.fabian.xrooms.menus.Menu;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof Menu) {
            Menu menu = (Menu) holder;
            if (menu.cancelClick()) e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            DebugLogger.debug("MenuListener", "Click in menu " + menu.getClass().getSimpleName() + " slot=" + e.getSlot() + " item=" + e.getCurrentItem().getType());
            menu.handleMenu(e);
        }
    }

    @EventHandler
    public void onMenuClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof Menu) {
            ((Menu) holder).handleClose(e);
        }
    }
}
