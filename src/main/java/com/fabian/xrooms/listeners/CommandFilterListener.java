package com.fabian.xrooms.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import com.fabian.xrooms.utils.DebugLogger;

import java.util.Collection;

public class CommandFilterListener implements Listener {

    public CommandFilterListener() {
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        // Obtenemos todos los comandos que se van a enviar al jugador
        Collection<String> commands = event.getCommands();
        int before = commands.size();
        
        // Eliminamos todos los que tengan el formato "plugin:comando"
        // Esto quita "x-rooms:xrooms", "minecraft:tp", etc.
        commands.removeIf(command -> command.contains(":"));
        DebugLogger.debug("CommandFilter", "Filtered " + (before - commands.size()) + " namespaced commands for " + event.getPlayer().getName());
    }
}
