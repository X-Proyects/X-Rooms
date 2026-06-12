package com.fabian.xrooms.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import com.fabian.xrooms.utils.DebugLogger;

import java.util.Collection;

public class CommandFilterListener implements Listener {

    // Namespace prefixes that should be hidden from tab-completion
    private static final String[] HIDDEN_NAMESPACES = {
        "x-rooms:xrooms", "xrooms:xrooms",
        "x-rooms:rooms", "xrooms:rooms",
        "x-rooms:room", "xrooms:room",
        "x-rooms:xr", "xrooms:xr"
    };

    public CommandFilterListener() {
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        Collection<String> commands = event.getCommands();
        int before = commands.size();

        // Only hide our own namespaced commands, leave others untouched
        commands.removeIf(command -> {
            String lower = command.toLowerCase();
            for (String ns : HIDDEN_NAMESPACES) {
                if (lower.equals(ns)) return true;
            }
            return false;
        });
        DebugLogger.debug("CommandFilter", "Filtered " + (before - commands.size()) + " namespaced commands for " + event.getPlayer().getName());
    }
}