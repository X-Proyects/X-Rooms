package com.fabian.xrooms.managers;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {

    private final XRooms plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();
    private final Map<UUID, Runnable> pendingCancels = new ConcurrentHashMap<>();

    public ChatInputManager(XRooms plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void requestInput(Player player, String message, Consumer<String> callback) {
        requestInput(player, message, callback, null);
    }

    public void requestInput(Player player, String message, Consumer<String> callback, Runnable onCancel) {
        DebugLogger.debug("ChatInputManager", "Requesting input from " + player.getName());
        player.closeInventory();
        player.sendMessage(plugin.getConfigManager().color(message));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("chat-cancel-info"));
        pendingInputs.put(player.getUniqueId(), callback);
        if (onCancel != null) {
            pendingCancels.put(player.getUniqueId(), onCancel);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (pendingInputs.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
            String input = e.getMessage();
            Consumer<String> callback = pendingInputs.remove(p.getUniqueId());
            Runnable onCancel = pendingCancels.remove(p.getUniqueId());

            if (input.equalsIgnoreCase("cancel")) {
                DebugLogger.debug("ChatInputManager", "Input cancelled by " + p.getName());
                p.sendMessage(plugin.getConfigManager().getMessageRaw("chat-action-cancelled"));
                if (onCancel != null) {
                    plugin.getXScheduler().runTask(onCancel);
                }
                return;
            }

            // Execute the callback on the main thread
            DebugLogger.debug("ChatInputManager", "Processing input from " + p.getName() + ": " + input);
            plugin.getXScheduler().runTask(() -> callback.accept(input));
        }
    }
}
