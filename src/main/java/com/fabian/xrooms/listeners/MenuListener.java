package com.fabian.xrooms.listeners;

import com.fabian.xrooms.guis.Menu;
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
