package com.fabian.xrooms;

import com.fabian.xrooms.managers.ConfigManager;
import com.fabian.xrooms.managers.CommandManager;
import com.fabian.xrooms.managers.DependencyManager;
import com.fabian.xrooms.managers.PermissionManager;
import com.fabian.xrooms.managers.RoomManager;
import com.fabian.xrooms.managers.HologramManager;
import com.fabian.xrooms.managers.ChatInputManager;
import com.fabian.xrooms.managers.InventoryManager;
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

        // Load libraries before anything else
        new DependencyManager(this).loadDependencies();

        this.configManager = new ConfigManager(this);
        this.xScheduler = new SchedulerUtil(this);
        this.chatInputManager = new ChatInputManager(this);
        
        org.bukkit.Bukkit.getConsoleSender().sendMessage(configManager.color(CONSOLE_PREFIX + "&aSuccessfully enabled!"));

        // Initialize Managers
        this.permissionManager = new PermissionManager(this);
        this.roomManager = new RoomManager(this);
        this.commandManager = new CommandManager(this);
        this.hologramManager = new HologramManager(this);
        this.inventoryManager = new InventoryManager(this);

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
        
        // Hide namespaced commands (1.13+)
        try {
            Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
            getServer().getPluginManager().registerEvents(new com.fabian.xrooms.listeners.CommandFilterListener(), this);
        } catch (ClassNotFoundException ignored) {}

        // Initialize Metrics (bStats)
        if (configManager.getConfig().getBoolean("metrics", true)) {
            com.fabian.xrooms.metrics.Metrics metrics = new com.fabian.xrooms.metrics.Metrics(this, 30894);
            metrics.addCustomChart(new com.fabian.xrooms.metrics.Metrics.SimplePie("total_rooms", () -> 
                    String.valueOf(roomManager.getAllRooms().size())));
            metrics.addCustomChart(new com.fabian.xrooms.metrics.Metrics.SimplePie("language", () -> 
                    getConfigManager().getConfig().getString("language", "es")));
        }

        // Update Checker
        new com.fabian.xrooms.utils.UpdateChecker(this).checkForUpdates();

        // Placeholders
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.fabian.xrooms.placeholders.XRoomsExpansion(this).register();
        }

        // Hologram Task
        this.xScheduler.runTimer(() -> hologramManager.updateHolograms(), 20L, 100L);

        // Schematic Task
        startSchematicTask();
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
            this.xScheduler.runTimer(() -> {
                for (com.fabian.xrooms.models.Room room : roomManager.getAllRooms()) {
                    if (room.isSchematicEnabled() && room.getState() == com.fabian.xrooms.models.RoomState.WAITING) {
                        com.fabian.xrooms.utils.WorldEditUtils.pasteSchematic(this, room);
                    }
                }
            }, ticks, ticks);
        }
    }

    private void sendConsole(String message) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(configManager.color(CONSOLE_PREFIX + message));
    }

    @Override
    public void onDisable() {
        if (roomManager != null) {
            roomManager.saveAll();
        }
        org.bukkit.Bukkit.getConsoleSender().sendMessage(configManager.color(CONSOLE_PREFIX + "&cSuccessfully disabled!"));
    }
}
