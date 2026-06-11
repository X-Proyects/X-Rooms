package com.fabian.xrooms.managers;

import com.fabian.xrooms.XRooms;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.fabian.xrooms.utils.ColorUtils;
import com.fabian.xrooms.utils.ConfigUpdater;
import com.fabian.xrooms.utils.DebugLogger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ConfigManager {

    private final XRooms plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    
    private String prefix;
    private String entrySound;
    private String killSound;
    
    private boolean useOwnInventory;
    private int startCountdown;
    private String barrierMaterial;
    private boolean restoreOnQuit;
    private int entryTitleCooldown;
    private int endDelay;
    private int autoResetInterval;
    
    private final Map<String, Integer> abilitiesLimits = new HashMap<>();
    private final Map<String, String> messageCache = new HashMap<>();

    public ConfigManager(XRooms plugin) {
        this.plugin = plugin;
        
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        DebugLogger.debug("ConfigManager", "Loading config.yml");
        if (configFile.exists()) {
            FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
            int currentVersion = currentConfig.getInt("code", 0);
            
            // Get internal version from resource
            FileConfiguration internalConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(plugin.getResource("config.yml")));
            int internalVersion = internalConfig.getInt("code", 0);
            
            if (currentVersion < internalVersion) {
                DebugLogger.debug("ConfigManager", "Config outdated: v" + currentVersion + " -> v" + internalVersion);
                plugin.logInfo("Updating config.yml from version " + currentVersion + " to " + internalVersion);
                
                // 1. Create backup (Copy instead of Rename to keep the file accessible for ConfigUpdater)
                File backupFile = new File(plugin.getDataFolder(), "config_old.yml");
                try {
                    com.google.common.io.Files.copy(configFile, backupFile);
                } catch (java.io.IOException e) {
                    plugin.logWarning("Could not create backup of config.yml");
                }
                
                // 2. The ConfigUpdater below will handle merging new keys and keeping old values.
                // We don't need to saveResource(true) because that would wipe custom values.
            }
        } else {
            plugin.saveDefaultConfig();
        }
        
        try {
            // This tool merges new keys from the JAR into the file on disk while preserving values.
            ConfigUpdater.update(plugin, "config.yml", configFile);
            DebugLogger.debug("ConfigManager", "ConfigUpdater merged config.yml");
        } catch (Exception e) {
            DebugLogger.debug("ConfigManager", "Failed to auto-update config.yml", e);
            plugin.logWarning("Failed to auto-update config.yml");
            e.printStackTrace();
        }
        
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Ensure the code on disk is updated to the latest version
        int internalVer = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(plugin.getResource("config.yml"))).getInt("code", 0);
        if (this.config.getInt("code", 0) < internalVer) {
            this.config.set("code", internalVer);
            try {
                this.config.save(configFile);
            } catch (java.io.IOException ignored) {}
        }

        setupConfig();
        setupMessages();
        DebugLogger.debug("ConfigManager", "ConfigManager fully initialized (language=" + config.getString("language", "es") + ")");
    }

    public void setupConfig() {
        this.prefix = color(config.getString("prefix", "&8[&bX-Rooms&8] &r"));
        this.entrySound = config.getString("entry-sound", "ENTITY_PLAYER_LEVELUP");
        this.killSound = config.getString("kill-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        
        this.useOwnInventory = config.getBoolean("room-settings.use-own-inventory", false);
        this.startCountdown = config.getInt("room-settings.start-countdown", 3);
        this.barrierMaterial = config.getString("room-settings.barrier-material", "AIR");
        this.restoreOnQuit = config.getBoolean("room-settings.restore-on-quit", true);
        this.entryTitleCooldown = config.getInt("room-settings.entry-title-cooldown", -1);
        this.endDelay = config.getInt("room-settings.end-delay", 3);
        this.autoResetInterval = config.getInt("schematics.auto-reset-interval", 30);
        
        abilitiesLimits.clear();
        if (config.contains("room-settings.abilities-level-limit")) {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("room-settings.abilities-level-limit");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    abilitiesLimits.put(key.toLowerCase(), section.getInt(key));
                }
            }
        }
    }

    public void setupMessages() {
        String lang = config.getString("language", "es");
        File messagesFile = new File(plugin.getDataFolder(), "messages/" + lang + ".yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages/es.yml", false);
            plugin.saveResource("messages/en.yml", false);
            plugin.saveResource("messages/fr.yml", false);
        } else {
            // Update the current language file
            try {
                ConfigUpdater.update(plugin, "messages/" + lang + ".yml", messagesFile);
            } catch (Exception e) {
                plugin.logWarning("Failed to auto-update messages/" + lang + ".yml");
                e.printStackTrace();
            }
        }
        
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
        messageCache.clear();
    }

    public String getMessage(String path) {
        return color(prefix + messages.getString(path, "&cMissing message: " + path));
    }

    public String getMessageRaw(String path) {
        return color(messages.getString(path, "&cMissing message: " + path));
    }

    public void sendMessage(org.bukkit.command.CommandSender sender, String path) {
        sender.sendMessage(getMessage(path));
    }

    public void sendMessageRaw(org.bukkit.command.CommandSender sender, String text) {
        sender.sendMessage(color(prefix + text));
    }

    public void reload() {
        DebugLogger.debug("ConfigManager", "Reloading configuration...");
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        setupConfig();
        setupMessages();
        DebugLogger.debug("ConfigManager", "Configuration reloaded");
    }

    public String color(String text) {
        return ColorUtils.translateColors(text);
    }

    public String setPlaceholders(org.bukkit.entity.Player player, String text) {
        if (text == null) return "";
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }
}
