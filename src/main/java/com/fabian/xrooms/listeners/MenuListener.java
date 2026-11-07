package com.fabian.xrooms.listeners;

import com.fabian.xrooms.menus.Menu;
import com.fabian.xrooms.menus.StorageMenu;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof Menu)) {
            return;
        }
        Menu menu = (Menu) holder;

        // C6: Always guard against a null clicked inventory (clicks "outside" the
        // inventory view, e.g. in the empty area below the player inventory) — these
        // would otherwise NPE downstream when we try to read the clicked slot/item.
        if (e.getClickedInventory() == null) {
            // Still honour cancelClick() so players can't take border items via the
            // keyboard (number keys / hotbar swap) when the click target was "outside".
            if (menu.cancelClick()) {
                e.setCancelled(true);
            }
            return;
        }

        // C6: Only treat clicks in the top (menu) inventory as menu clicks. If the
        // clicked inventory is the player's own (bottom) inventory we ignore the
        // click for menu handling — otherwise `e.getSlot()` would return a slot index
        // relative to the player inventory that could collide with menu slot indices
        // (e.g. clicking hotbar slot 10 would wrongly trigger the menu's slot-10
        // action). We still cancel these clicks when `cancelClick()` is true so that
        // shift-clicks from the bottom inventory cannot push items INTO the menu
        // (effectively stealing border/decoration slots or overwriting SAVE buttons).
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) {
            if (menu.cancelClick()) {
                e.setCancelled(true);
            }
            return;
        }

        // From here on, the click happened inside the menu's own inventory.
        if (menu.cancelClick()) {
            e.setCancelled(true);
        }
        if (e.getCurrentItem() == null) {
            return;
        }
        DebugLogger.debug("MenuListener", "Click in menu " + menu.getClass().getSimpleName()
                + " slot=" + e.getSlot() + " item=" + e.getCurrentItem().getType());
        menu.handleMenu(e);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof Menu)) {
            return;
        }
        Menu menu = (Menu) holder;

        // C7: Drags bypass `cancelClick()` and can be used to steal menu border /
        // decoration items, or to drop items onto the StorageMenu SAVE/CANCEL
        // buttons. When the menu wants clicks cancelled, cancel drags outright.
        if (menu.cancelClick()) {
            e.setCancelled(true);
            return;
        }

        // For editable menus (StorageMenu), still forbid dragging onto the bottom-row
        // button slots (45..53) so the SAVE / CANCEL / decoration slots stay intact.
        if (menu instanceof StorageMenu) {
            for (int slot : e.getRawSlots()) {
                if (slot >= 45 && slot < 54) {
                    e.setCancelled(true);
                    return;
                }
            }
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
