package com.fabian.xrooms.managers;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.utils.DebugLogger;
import com.fabian.xrooms.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {

    private final XRooms plugin;
    // Maps are keyed by the player's UUID (NOT by the Player object) so that the
    // pending callback does not keep a strong reference to a stale Player instance
    // after the player has disconnected. Callers' lambda bodies may capture `p`
    // directly; that is the caller's responsibility — the manager itself never
    // stores the Player, only the UUID, and re-resolves the player via
    // `Bukkit.getPlayer(uuid)` at fire-time (see onChat).
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
        // Key by UUID only — the manager must NOT retain the Player reference.
        UUID uuid = player.getUniqueId();
        pendingInputs.put(uuid, callback);
        if (onCancel != null) {
            pendingCancels.put(uuid, onCancel);
        }
    }

    /**
     * Remove any pending input consumer / cancel runnable for the given player.
     * Called on {@link PlayerQuitEvent} to prevent the Player object from being
     * retained by the callback lambdas (which would be a memory leak and would
     * also fire stale callbacks if the same UUID reconnected later).
     */
    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        Consumer<String> cb = pendingInputs.remove(uuid);
        Runnable onCancel = pendingCancels.remove(uuid);
        if (cb != null || onCancel != null) {
            DebugLogger.debug("ChatInputManager",
                    "Cleared pending chat input for " + player.getName() + " on disconnect");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!pendingInputs.containsKey(p.getUniqueId())) {
            return;
        }
        e.setCancelled(true);
        String input = e.getMessage();
        UUID uuid = p.getUniqueId();
        Consumer<String> callback = pendingInputs.remove(uuid);
        Runnable onCancel = pendingCancels.remove(uuid);

        if (input.equalsIgnoreCase("cancel")) {
            DebugLogger.debug("ChatInputManager", "Input cancelled by " + p.getName());
            p.sendMessage(plugin.getConfigManager().getMessageRaw("chat-action-cancelled"));
            if (onCancel != null) {
                // Execute the cancel runnable on the main thread. Re-resolve the player
                // first — they may have disconnected between the chat packet and the
                // scheduled task firing.
                SchedulerUtil.runTask(plugin, () -> {
                    Player online = Bukkit.getPlayer(uuid);
                    if (online == null || !online.isOnline()) {
                        DebugLogger.debug("ChatInputManager",
                                "Skipping onCancel runnable: player " + uuid + " is no longer online");
                        return;
                    }
                    try {
                        onCancel.run();
                    } catch (Throwable t) {
                        DebugLogger.debug("ChatInputManager",
                                "onCancel runnable threw for " + uuid, t);
                    }
                });
            }
            return;
        }

        // Execute the callback on the main thread.
        DebugLogger.debug("ChatInputManager", "Processing input from " + p.getName() + ": " + input);
        SchedulerUtil.runTask(plugin, () -> {
            // Re-resolve the player: they may have typed a chat message and then
            // disconnected before this scheduled task fired. The callback lambdas
            // (caller-supplied) typically capture the original Player `p` and would
            // throw IllegalStateException ("Player not online") if invoked on an
            // offline player. Guard against that here.
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || !online.isOnline()) {
                DebugLogger.debug("ChatInputManager",
                        "Skipping chat input callback: player " + uuid + " is no longer online");
                return;
            }
            try {
                callback.accept(input);
            } catch (Throwable t) {
                // Defensive: a callback that throws on the main thread would otherwise
                // kill the scheduler task silently.
                DebugLogger.debug("ChatInputManager",
                        "Chat input callback threw for " + uuid, t);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clear(e.getPlayer());
    }
}
