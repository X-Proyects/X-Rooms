package com.fabian.xrooms.managers;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class InventoryManager {

    private final XRooms plugin;
    private final File playersFolder;

    public InventoryManager(XRooms plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "players");
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
    }

    public void saveAndClearInventory(Player p) {
        DebugLogger.debug("InventoryManager", "Saving and clearing inventory for " + p.getName());
        File file = new File(playersFolder, p.getUniqueId().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        // Save inventory as a List (proper YAML serialization)
        config.set("inventory", Arrays.asList(p.getInventory().getContents()));
        config.set("armor", Arrays.asList(p.getInventory().getArmorContents()));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.logError("Could not save inventory for player " + p.getName());
            e.printStackTrace();
        }

        // Clear everything
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);
        p.updateInventory();
    }

    @SuppressWarnings("unchecked")
    public void restoreInventory(Player p) {
        File file = new File(playersFolder, p.getUniqueId().toString() + ".yml");
        if (!file.exists()) return;
        DebugLogger.debug("InventoryManager", "Restoring inventory for " + p.getName());

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Restore inventory
        if (config.contains("inventory")) {
            List<ItemStack> inv = (List<ItemStack>) config.get("inventory");
            if (inv != null) p.getInventory().setContents(inv.toArray(new ItemStack[0]));
        }

        if (config.contains("armor")) {
            List<ItemStack> armor = (List<ItemStack>) config.get("armor");
            if (armor != null) p.getInventory().setArmorContents(armor.toArray(new ItemStack[0]));
        }

        p.updateInventory();

        // Delete the file after restoring
        file.delete();
    }

    public void deleteBackup(Player p) {
        File file = new File(playersFolder, p.getUniqueId().toString() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }
}

