package com.fabian.xrooms;

import com.fabian.xrooms.managers.ConfigManager;
import com.fabian.xrooms.commands.CommandManager;
import com.fabian.xrooms.managers.DependencyManager;
import com.fabian.xrooms.managers.PermissionManager;
import com.fabian.xrooms.managers.RoomManager;
import com.fabian.xrooms.managers.HologramManager;
import com.fabian.xrooms.managers.ChatInputManager;
import com.fabian.xrooms.managers.InventoryManager;
import com.fabian.xrooms.utils.DebugLogger;
import com.fabian.xrooms.utils.SchedulerUtil;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class XRooms extends JavaPlugin {

    private static XRooms instance;

    public static XRooms getInstance() {
        return instance;
    }

    private ConfigManager configManager;
    private RoomManager roomManager;
    private CommandManager commandManager;
    private PermissionManager permissionManager;
    private HologramManager hologramManager;
    private ChatInputManager chatInputManager;
    private InventoryManager inventoryManager;
    private SchedulerUtil xScheduler;

    private final String CONSOLE_PREFIX = "&8[&bX-Rooms&8]&r ";

    @Override
    public void onEnable() {
        instance = this;

        DebugLogger.debug("Enable", "Plugin enable sequence started (v" + getDescription().getVersion() + ")");

        // Load libraries before anything else
        new DependencyManager(this).loadDependencies();
        DebugLogger.debug("Enable", "Dependencies loaded");

        this.configManager = new ConfigManager(this);
        DebugLogger.debug("Enable", "ConfigManager initialized");
        this.xScheduler = new SchedulerUtil(this);
        this.chatInputManager = new ChatInputManager(this);
        
        org.bukkit.Bukkit.getConsoleSender().sendMessage(configManager.color(CONSOLE_PREFIX + "&aSuccessfully enabled!"));

        // Initialize Managers
        this.permissionManager = new PermissionManager(this);
        DebugLogger.debug("Enable", "PermissionManager initialized");
        this.roomManager = new RoomManager(this);
        DebugLogger.debug("Enable", "RoomManager initialized");
        this.commandManager = new CommandManager(this);
        DebugLogger.debug("Enable", "CommandManager initialized");
        this.hologramManager = new HologramManager(this);
        DebugLogger.debug("Enable", "HologramManager initialized (provider: " + hologramManager.getProviderName() + ")");
        this.inventoryManager = new InventoryManager(this);
        DebugLogger.debug("Enable", "InventoryManager initialized");

        // Fancy Startup Logs
        sendConsole("&bInitializing room configurations...");
        sendConsole("&bLoading registered rooms from disk...");
        sendConsole("&a" + roomManager.getAllRooms().size() + " rooms loaded and ready to use.");
        
        sendConsole("&b&l----------------------------------------------");
        sendConsole("  &aEnabled v" + getDescription().getVersion() + "! &fEnjoy rooms!");
        sendConsole("  &bHolograms: &f" + hologramManager.getProviderName() + " &8| &bLanguage: &f" + configManager.getConfig().getString("language", "en").toUpperCase());
        sendConsole("&b&l----------------------------------------------");

        // Register Listeners
        getServer().getPluginManager().registerEvents(new com.fabian.xrooms.listeners.MenuListener(), this);
        getServer().getPluginManager().registerEvents(new com.fabian.xrooms.listeners.RoomListener(this), this);
        DebugLogger.debug("Enable", "Listeners registered (MenuListener, RoomListener)");
        
        // Hide namespaced commands (1.13+)
        try {
            Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
            getServer().getPluginManager().registerEvents(new com.fabian.xrooms.listeners.CommandFilterListener(), this);
            DebugLogger.debug("Enable", "CommandFilterListener registered");
        } catch (ClassNotFoundException ignored) {}

        // Initialize Metrics (bStats)
        DebugLogger.debug("Enable", "Metrics: " + (configManager.getConfig().getBoolean("metrics", true) ? "enabled" : "disabled"));
        if (configManager.getConfig().getBoolean("metrics", true)) {
            com.fabian.xrooms.metrics.Metrics metrics = new com.fabian.xrooms.metrics.Metrics(this, 30894);
            metrics.addCustomChart(new com.fabian.xrooms.metrics.Metrics.SimplePie("total_rooms", () -> 
                    String.valueOf(roomManager.getAllRooms().size())));
            metrics.addCustomChart(new com.fabian.xrooms.metrics.Metrics.SimplePie("language", () -> 
                    getConfigManager().getConfig().getString("language", "es")));
        }

        // Update Checker
        new com.fabian.xrooms.utils.UpdateChecker(this).checkForUpdates();
        DebugLogger.debug("Enable", "Update checker started");

        // Placeholders
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.fabian.xrooms.placeholders.XRoomsExpansion(this).register();
            DebugLogger.debug("Enable", "PlaceholderAPI expansion registered");
        }

        // Hologram Task
        this.xScheduler.runTimer(() -> hologramManager.updateHolograms(), 20L, 100L);
        DebugLogger.debug("Enable", "Hologram update task scheduled (20L initial, 100L period)");

        // Schematic Task
        startSchematicTask();
        DebugLogger.debug("Enable", "Plugin enable sequence completed");
    }

    private Object schematicTask;

    public void startSchematicTask() {
        if (schematicTask != null) {
            try {
                schematicTask.getClass().getMethod("cancel").invoke(schematicTask);
            } catch (Exception ignored) {}
            schematicTask = null;
        }

        int interval = configManager.getAutoResetInterval();
        if (interval > 0) {
            long ticks = interval * 60 * 20L;
            DebugLogger.debug("SchematicTask", "Scheduled auto-reset task: interval=" + interval + "min, ticks=" + ticks);
            this.xScheduler.runTimer(() -> {
                for (com.fabian.xrooms.models.Room room : roomManager.getAllRooms()) {
                    if (room.isSchematicEnabled() && room.getState() == com.fabian.xrooms.models.RoomState.WAITING) {
                        com.fabian.xrooms.utils.WorldEditUtils.pasteSchematic(this, room);
                    }
                }
            }, ticks, ticks);
        }
    }

    public void logInfo(String message) {
        getLogger().info(message);
    }

    public void logWarning(String message) {
        getLogger().warning(message);
    }

    public void logError(String message) {
        getLogger().severe(message);
    }

    private void sendConsole(String message) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(configManager.color(CONSOLE_PREFIX + message));
    }

    @Override
    public void onDisable() {
        DebugLogger.debug("Disable", "Plugin disable sequence started");
        if (roomManager != null) {
            roomManager.saveAll();
            DebugLogger.debug("Disable", "All rooms saved");
        }
        org.bukkit.Bukkit.getConsoleSender().sendMessage(configManager.color(CONSOLE_PREFIX + "&cSuccessfully disabled!"));
    }
}
