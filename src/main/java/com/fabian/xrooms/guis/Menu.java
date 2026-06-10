package com.fabian.xrooms.guis;

import com.fabian.xrooms.XRooms;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public abstract class Menu implements InventoryHolder {

    protected Inventory inventory;
    protected XRooms plugin = XRooms.getInstance();

    public abstract String getMenuName();
    public abstract int getSlots();
    public abstract void handleMenu(InventoryClickEvent e);
    public abstract void setMenuItems();
    
    public boolean cancelClick() {
        return true;
    }
    
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent e) {}

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, getSlots(), getMenuName());
        this.setMenuItems();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected void fillBorders(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);

        for (int i = 0; i < getSlots(); i++) {
            if (i < 9 || i >= getSlots() - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inventory.setItem(i, item);
            }
        }
    }

    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().color(name));
        if (lore != null) {
            lore.replaceAll(s -> plugin.getConfigManager().color(s));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
