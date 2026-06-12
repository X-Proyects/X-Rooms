package com.fabian.xrooms;

import com.fabian.xrooms.managers.ConfigManager;
import com.fabian.xrooms.managers.CommandManager;
import com.fabian.xrooms.managers.DependencyManager;
import com.fabian.xrooms.managers.PermissionManager;
import com.fabian.xrooms.managers.RoomManager;
import com.fabian.xrooms.managers.HologramManager;
import com.fabian.xrooms.managers.ChatInputManager;
import com.fabian.xrooms.managers.InventoryManager;
import com.fabian.xrooms.utils.DebugLogger;
import com.fabian.xrooms.utils.SchedulerUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
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
    private com.fabian.xrooms.utils.UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // Initialize config managers first
            this.configManager = new ConfigManager(this);
            DebugLogger.debug("Config", "ConfigManager initialized");
        } catch (Exception e) {
            DebugLogger.debug("Config", "Failed to initialize config managers", e);
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Load libraries before anything else
        DebugLogger.debug("Dependency", "Initializing DependencyManager...");
        new DependencyManager(this).loadDependencies();

        // Initialize remaining managers
        try {
            this.xScheduler = new SchedulerUtil(this);
            this.chatInputManager = new ChatInputManager(this);
            DebugLogger.debug("Init", "SchedulerUtil and ChatInputManager initialized");

            this.permissionManager = new PermissionManager(this);
            DebugLogger.debug("Init", "PermissionManager initialized");
            this.roomManager = new RoomManager(this);
            DebugLogger.debug("Init", "RoomManager initialized");
            this.commandManager = new CommandManager(this);
            DebugLogger.debug("Init", "CommandManager initialized");
            this.hologramManager = new HologramManager(this);
            DebugLogger.debug("Init", "HologramManager initialized (provider: " + hologramManager.getProviderName() + ")");
            this.inventoryManager = new InventoryManager(this);
            DebugLogger.debug("Init", "InventoryManager initialized");

            // Register Listeners
            getServer().getPluginManager().registerEvents(new com.fabian.xrooms.listeners.MenuListener(), this);
            getServer().getPluginManager().registerEvents(new com.fabian.xrooms.listeners.RoomListener(this), this);
            DebugLogger.debug("Init", "Listeners registered (MenuListener, RoomListener)");

            // Hide namespaced commands (1.13+)
            try {
                Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
                getServer().getPluginManager().registerEvents(new com.fabian.xrooms.listeners.CommandFilterListener(), this);
                DebugLogger.debug("Init", "CommandFilterListener registered");
            } catch (ClassNotFoundException ignored) {}

            // PlaceholderAPI Integration
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                DebugLogger.debug("PAPI", "PlaceholderAPI found, registering expansion");
                new com.fabian.xrooms.placeholders.XRoomsExpansion(this).register();
            } else {
                DebugLogger.debug("PAPI", "PlaceholderAPI not found, skipping expansion");
            }

            // Hologram Task
            this.xScheduler.runTimer(() -> hologramManager.updateHolograms(), 20L, 100L);
            DebugLogger.debug("Init", "Hologram update task scheduled");

            // Schematic Task
            startSchematicTask();
            DebugLogger.debug("Init", "Schematic task started");

        } catch (Exception e) {
            DebugLogger.debug("Init", "Failed to initialize managers", e);
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check for updates
        if (configManager.getConfig().getBoolean("updates.check", true)) {
            DebugLogger.debug("Update", "Update checker enabled, scheduling check");
            this.updateChecker = new com.fabian.xrooms.utils.UpdateChecker(this);
            xScheduler.runAsync(() -> updateChecker.checkForUpdates());
        }

        // Register update notification listener
        final var ucRef = this.updateChecker;
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                org.bukkit.entity.Player player = event.getPlayer();
                if (!player.isOp() && !player.hasPermission("xrooms.admin")) return;
                if (!configManager.getConfig().getBoolean("updates.notify-on-join", true)) return;
                if (ucRef == null) return;
                if (ucRef.isUpdateAvailable()) {
                    DebugLogger.debug("UpdateListener", "Notifying admin " + player.getName() + " about update");
                    String current = getDescription().getVersion();
                    String latest = ucRef.getLatestVersion();
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&8[&bX-Rooms&8] &eA new version is available: &a" + latest + " &e(current: &c" + current + "&e)"));
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&8[&bX-Rooms&8] &7Download it at: &f" + ucRef.getDownloadUrl()));
                }
            }
        }, this);

        // Initialize bStats Metrics
        setupMetrics();

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Rooms&8] &7----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Rooms&8]   &aEnabled v" + getDescription().getVersion() + "! Enjoy rooms!"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Rooms&8]   &fHolograms: &e" + hologramManager.getProviderName()
                + " &7| &fLanguage: &e" + configManager.getConfig().getString("language", "en").toUpperCase()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Rooms&8] &7----------------------------------------------"));
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

    private void setupMetrics() {
        if (configManager.getConfig().getBoolean("metrics", true)) {
            try {
                com.fabian.xrooms.metrics.Metrics metrics = new com.fabian.xrooms.metrics.Metrics(this, 30894);
                metrics.addCustomChart(new com.fabian.xrooms.metrics.Metrics.SimplePie("total_rooms", () ->
                        String.valueOf(roomManager.getAllRooms().size())));
                metrics.addCustomChart(new com.fabian.xrooms.metrics.Metrics.SimplePie("language", () ->
                        configManager.getConfig().getString("language", "en")));
            } catch (Exception e) {
                logWarning("Could not start bStats Metrics: " + e.getMessage());
            }
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

    @Override
    public void onDisable() {
        DebugLogger.debug("Init", "Plugin disabling...");
        if (roomManager != null) {
            roomManager.saveAll();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Rooms&8] &7----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Rooms&8]   &cDisabled v" + getDescription().getVersion() + "! Out."));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Rooms&8] &7----------------------------------------------"));
    }
}
