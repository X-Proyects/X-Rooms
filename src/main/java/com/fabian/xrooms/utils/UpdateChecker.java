package com.fabian.xrooms.utils;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final XRooms plugin;
    private final int resourceId;
    @lombok.Getter
    private String latestVersion;
    @lombok.Getter
    private boolean updateAvailable;

    public UpdateChecker(XRooms plugin) {
        this.plugin = plugin;
        this.resourceId = 134537; // Placeholder ID (Update later)
        this.updateAvailable = false;
    }

    public void checkForUpdates() {
        checkForUpdates(null);
    }

    public void checkForUpdates(CommandSender sender) {
        plugin.getXScheduler().runAsync(() -> {
            try {
                String currentVersion = plugin.getDescription().getVersion();
                
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Fabian/X-Rooms/" + currentVersion);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String version = reader.readLine();
                reader.close();

                this.latestVersion = version;

                if (latestVersion != null && isNewer(currentVersion, latestVersion)) {
                    this.updateAvailable = true;
                    ConfigManager cm = plugin.getConfigManager();
                    
                    String msgRaw = cm.getMessages().getString("update-available", "&aNew update available! &e{latest} &7(Current: {current})");
                    String linkRaw = cm.getMessages().getString("update-download", "&bDownload: &f{link}");
                    
                    String msg = cm.color(cm.getPrefix() + msgRaw
                            .replace("{latest}", latestVersion)
                            .replace("{current}", currentVersion));
                    String link = cm.color(cm.getPrefix() + linkRaw
                            .replace("{link}", getDownloadUrl()));

                    if (sender != null) {
                        sender.sendMessage(msg);
                        sender.sendMessage(link);
                    } else {
                        Bukkit.getConsoleSender().sendMessage(msg);
                        Bukkit.getConsoleSender().sendMessage(link);
                    }
                }

            } catch (Exception e) {
                // Ignore update errors
            }
        });
    }

    public String getDownloadUrl() {
        return "https://www.spigotmc.org/resources/" + resourceId + "/";
    }

    private boolean isNewer(String current, String latest) {
        String[] currentParts = current.replace("v", "").split("\\.");
        String[] latestParts = latest.replace("v", "").split("\\.");
        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i].replaceAll("[^0-9]", "")) : 0;
            if (latestPart > currentPart) return true;
            if (latestPart < currentPart) return false;
        }
        return false;
    }
}
