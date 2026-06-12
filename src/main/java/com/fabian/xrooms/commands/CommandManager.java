package com.fabian.xrooms.commands;

import com.fabian.xrooms.XRooms;
import com.fabian.xrooms.menus.RoomsMenu;
import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import com.fabian.xrooms.utils.WorldEditUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import java.util.List;
import org.bukkit.Location;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final XRooms plugin;

    public CommandManager(XRooms plugin) {
        this.plugin = plugin;
        plugin.getCommand("xrooms").setExecutor(this);
        plugin.getCommand("xrooms").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("gui");
            completions.add("create");
            completions.add("delete");
            completions.add("reload");
            completions.add("help");
            completions.add("redefine");
            completions.add("edit");
            completions.add("tp");
            completions.add("saveschematic");
            completions.add("debug");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("redefine") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("saveschematic"))) {
            return plugin.getRoomManager().getAllRooms().stream()
                    .map(Room::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
            completions.add("rename");
            completions.add("pvp-duration");
            completions.add("min-players");
            completions.add("max-players");
            completions.add("entry-sound");
            completions.add("kill-sound");
            completions.add("hologram");
            completions.add("abilities");
            completions.add("view-equipment");
            completions.add("winner-location");
            completions.add("schematic-reset");
            completions.add("title");
            completions.add("subtitle");
            return completions.stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("edit")) {
            if (args[2].equalsIgnoreCase("abilities") || args[2].equalsIgnoreCase("ability")) {
                completions.add("strength");
                completions.add("speed");
                completions.add("resistance");
                completions.add("haste");
                completions.add("regeneration");
                completions.add("fire_resistance");
                completions.add("jump");
                completions.add("saturation");
                completions.add("absorption");
                completions.add("fly");
                return completions.stream()
                        .filter(s -> s.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[2].equalsIgnoreCase("hologram")) {
                completions.add("movehere");
                completions.add("move");
                completions.add("line");
                return completions.stream()
                        .filter(s -> s.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[2].equalsIgnoreCase("entry-sound") || args[2].equalsIgnoreCase("kill-sound")) {
                List<String> soundNames = new ArrayList<>();
                try {
                    // Modern Paper/Folia: Sound is an interface, use Registry
                    org.bukkit.Registry<org.bukkit.Sound> registry = org.bukkit.Registry.SOUNDS;
                    for (org.bukkit.Sound sound : registry) {
                        soundNames.add(sound.getKey().getKey().toUpperCase().replace(".", "_"));
                    }
                } catch (Exception | NoSuchFieldError ex) {
                    try {
                        // Legacy Spigot: Sound is an Enum
                        for (Object s : org.bukkit.Sound.class.getEnumConstants()) {
                            soundNames.add(((Enum<?>) s).name());
                        }
                    } catch (Exception ignored) {}
                }
                String prefix = args[3].toUpperCase();
                return soundNames.stream()
                        .filter(s -> s.startsWith(prefix))
                        .sorted()
                        .collect(Collectors.toList());
            } else if (args[2].equalsIgnoreCase("schematic-reset")) {
                completions.add("true");
                completions.add("false");
                return completions.stream()
                        .filter(s -> s.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 5 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("hologram") && args[3].equalsIgnoreCase("line")) {
            completions.add("add");
            completions.add("edit");
            return completions.stream()
                    .filter(s -> s.startsWith(args[4].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DebugLogger.debug("CommandManager", "Command executed by " + sender.getName() + ": /xrooms " + String.join(" ", args));
        if (args.length == 0) {
            if (sender instanceof Player) {
                sendHelp((Player) sender);
            } else {
                sendConsoleHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        // Console-allowed commands
        if (sub.equals("reload")) {
            if (!sender.hasPermission("xrooms.admin.reload")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            DebugLogger.debug("CommandManager", "Reload command executed");
            plugin.getConfigManager().reload();
            plugin.getRoomManager().loadAll();
            plugin.startSchematicTask();
            sender.sendMessage(plugin.getConfigManager().getMessage("plugin-reloaded"));
            return true;
        }

        if (sub.equals("help")) {
            if (sender instanceof Player) {
                sendHelp((Player) sender);
            } else {
                sendConsoleHelp(sender);
            }
            return true;
        }

        if (sub.equals("debug")) {
            boolean dbg = plugin.getConfigManager().getConfig().getBoolean("debug", false);
            plugin.getConfig().set("debug", !dbg);
            plugin.saveConfig();
            sender.sendMessage(com.fabian.xrooms.utils.ColorUtils.translateColors(
                    "&8[&bX-Rooms&8] &7Debug mode: " + (!dbg ? "&aenabled" : "&cdisabled")));
            return true;
        }

        // Player-only commands
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;

        if (sub.equals("-gui") || sub.equals("gui")) {
            if (!plugin.getPermissionManager().canOpenGui(player)) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            new RoomsMenu().open(player);
            return true;
        }

        if (sub.equals("create")) {
            if (!plugin.getPermissionManager().canCreate(player)) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-create")));
                return true;
            }

            WorldEditUtils.RegionSelection sel = WorldEditUtils.getSelection(player);
            if (sel == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-selection"));
                return true;
            }

            String name = args[1];
            if (plugin.getRoomManager().getRoom(name) != null) {
                player.sendMessage(plugin.getConfigManager().getMessage("room-already-exists").replace("{name}", name));
                return true;
            }
            Room room = new Room(name);
            room.setWorldName(sel.world);
            room.setMinX(Math.min(sel.min.getX(), sel.max.getX()));
            room.setMinY(Math.min(sel.min.getY(), sel.max.getY()));
            room.setMinZ(Math.min(sel.min.getZ(), sel.max.getZ()));
            room.setMaxX(Math.max(sel.min.getX(), sel.max.getX()));
            room.setMaxY(Math.max(sel.min.getY(), sel.max.getY()));
            room.setMaxZ(Math.max(sel.min.getZ(), sel.max.getZ()));

            plugin.getRoomManager().createRoom(room);
            DebugLogger.debug("CommandManager", "Room created: " + name);
            player.sendMessage(plugin.getConfigManager().getMessage("room-created").replace("{name}", name));
            return true;
        }

        if (sub.equals("delete")) {
            if (!plugin.getPermissionManager().canDelete(player)) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-delete")));
                return true;
            }

            String name = args[1];
            if (plugin.getRoomManager().getRoom(name) == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("room-not-found"));
                return true;
            }

            plugin.getRoomManager().deleteRoom(name);
            player.sendMessage(plugin.getConfigManager().getMessage("room-deleted").replace("{name}", name));
            return true;
        }



        if (sub.equals("redefine")) {
            if (!plugin.getPermissionManager().canEdit(player)) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-redefine")));
                return true;
            }

            Room room = plugin.getRoomManager().getRoom(args[1]);
            if (room == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("room-not-found"));
                return true;
            }

            WorldEditUtils.RegionSelection sel = WorldEditUtils.getSelection(player);
            if (sel == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-selection"));
                return true;
            }

            room.setWorldName(sel.world);
            room.setMinX(Math.min(sel.min.getX(), sel.max.getX()));
            room.setMinY(Math.min(sel.min.getY(), sel.max.getY()));
            room.setMinZ(Math.min(sel.min.getZ(), sel.max.getZ()));
            room.setMaxX(Math.max(sel.min.getX(), sel.max.getX()));
            room.setMaxY(Math.max(sel.min.getY(), sel.max.getY()));
            room.setMaxZ(Math.max(sel.min.getZ(), sel.max.getZ()));

            plugin.getRoomManager().saveRoom(room);
            player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
            return true;
        }

        if (sub.equals("tp")) {
            if (!plugin.getPermissionManager().canTp(player)) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-tp")));
                return true;
            }
            Room room = plugin.getRoomManager().getRoom(args[1]);
            if (room == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("room-not-found"));
                return true;
            }
            double cx = (room.getMinX() + room.getMaxX()) / 2.0;
            double cy = room.getMinY() + 1.0;
            double cz = (room.getMinZ() + room.getMaxZ()) / 2.0;
            Location loc = new Location(org.bukkit.Bukkit.getWorld(room.getWorldName()), cx, cy, cz);
            plugin.getSchedulerUtil().teleport(player, loc);
            player.sendMessage(plugin.getConfigManager().getMessage("teleported").replace("{name}", room.getDisplayName()));
            return true;
        }

        if (sub.equals("saveschematic")) {
            if (!plugin.getPermissionManager().canEdit(player)) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-saveschematic")));
                return true;
            }
            Room room = plugin.getRoomManager().getRoom(args[1]);
            if (room == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("room-not-found"));
                return true;
            }
            
            player.sendMessage(plugin.getConfigManager().getMessage("saving-schematic"));
            WorldEditUtils.saveSchematic(plugin, room, player, success -> {
                if (success) {
                    room.setSchematicEnabled(true);
                    plugin.getRoomManager().saveRoom(room);
                    player.sendMessage(plugin.getConfigManager().getMessage("schematic-saved"));
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("schematic-error"));
                }
            });
            return true;
        }

        if (sub.equals("edit")) {
            if (!plugin.getPermissionManager().canEdit(player)) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 3) {
                                player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit")));

                return true;
            }

            String roomName = args[1];
            Room room = plugin.getRoomManager().getRoom(roomName);
            if (room == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("room-not-found"));
                return true;
            }

            String action = args[2].toLowerCase();
            switch (action) {
                case "rename":
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-rename").replace("{room}", roomName)));
                        return true;
                    }
                    String newName = args[3];
                    room.setDisplayName(newName);
                    plugin.getRoomManager().saveRoom(room);
                    player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    break;
                case "pvp-duration":
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-pvp-duration").replace("{room}", roomName)));
                        return true;
                    }
                    try {
                        room.setPvpDuration(Integer.parseInt(args[3]));
                        plugin.getRoomManager().saveRoom(room);
                        player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
                    }
                    break;
                case "min-players":
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-min-players").replace("{room}", roomName)));
                        return true;
                    }
                    try {
                        int num = Integer.parseInt(args[3]);
                        if (num < 2) num = 2;
                        room.setMinPlayers(num);
                        plugin.getRoomManager().saveRoom(room);
                        player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
                    }
                    break;
                case "max-players":
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-max-players").replace("{room}", roomName)));
                        return true;
                    }
                    try {
                        room.setMaxPlayers(Integer.parseInt(args[3]));
                        plugin.getRoomManager().saveRoom(room);
                        player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
                    }
                    break;
                case "entry-sound":
                    player.sendMessage(plugin.getConfigManager().getMessage("sound-info-entry"));
                    player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-entry-sound").replace("{room}", roomName)));
                    room.setEntrySound(args[3].toUpperCase());
                    plugin.getRoomManager().saveRoom(room);
                    player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    break;
                case "kill-sound":
                    player.sendMessage(plugin.getConfigManager().getMessage("sound-info-kill"));
                    player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-kill-sound").replace("{room}", roomName)));
                    room.setKillSound(args[3].toUpperCase());
                    plugin.getRoomManager().saveRoom(room);
                    player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    break;
                case "title":
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-title").replace("{room}", roomName)));
                        return true;
                    }
                    StringBuilder titleBuilder = new StringBuilder();
                    for (int i = 3; i < args.length; i++) titleBuilder.append(args[i]).append(" ");
                    room.setEntryTitle(titleBuilder.toString().trim());
                    plugin.getRoomManager().saveRoom(room);
                    player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    break;
                case "subtitle":
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-subtitle").replace("{room}", roomName)));
                        return true;
                    }
                    StringBuilder subBuilder = new StringBuilder();
                    for (int i = 3; i < args.length; i++) subBuilder.append(args[i]).append(" ");
                    room.setEntrySubtitle(subBuilder.toString().trim());
                    plugin.getRoomManager().saveRoom(room);
                    player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    break;
                case "hologram":
                    if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("DecentHolograms") && !org.bukkit.Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
                        player.sendMessage(plugin.getConfigManager().getMessage("no-hologram-plugin"));
                        return true;
                    }
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", "/xroom edit " + roomName + " hologram <movehere/move/line>"));
                        return true;
                    }
                    String holoAction = args[3].toLowerCase();
                    switch (holoAction) {
                        case "movehere":
                            room.setHologramLocation(player.getLocation());
                            plugin.getRoomManager().saveRoom(room);
                            player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                            break;
                        case "move":
                            if (args.length < 7) {
                                player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", "/xroom edit " + roomName + " hologram move <x> <y> <z>"));
                                return true;
                            }
                            try {
                                double x = Double.parseDouble(args[4]);
                                double y = Double.parseDouble(args[5]);
                                double xz = Double.parseDouble(args[6]);
                                room.setHologramLocation(new Location(player.getWorld(), x, y, xz));
                                plugin.getRoomManager().saveRoom(room);
                                player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                            } catch (NumberFormatException e) {
                                player.sendMessage(plugin.getConfigManager().getMessage("invalid-coordinates"));
                            }
                            break;
                        case "line":
                            if (args.length < 6) {
                                player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", "/xroom edit " + roomName + " hologram line <edit/add> ..."));
                                return true;
                            }
                            String lineAction = args[4].toLowerCase();
                            if (lineAction.equals("add")) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 5; i < args.length; i++) sb.append(args[i]).append(" ");
                                room.getHologramLines().add(sb.toString().trim());
                                plugin.getRoomManager().saveRoom(room);
                                player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                            } else if (lineAction.equals("edit")) {
                                if (args.length < 7) {
                                    player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", "/xroom edit " + roomName + " hologram line edit <linea> <texto>"));
                                    return true;
                                }
                                try {
                                    int index = Integer.parseInt(args[5]) - 1;
                                    if (index < 0 || index >= room.getHologramLines().size()) {
                                        player.sendMessage(plugin.getConfigManager().getMessage("invalid-line"));
                                        return true;
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    for (int i = 6; i < args.length; i++) sb.append(args[i]).append(" ");
                                    room.getHologramLines().set(index, sb.toString().trim());
                                    plugin.getRoomManager().saveRoom(room);
                                    player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                                } catch (NumberFormatException e) {
                                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-line-number"));
                                }
                            }
                            break;
                    }
                    break;
                case "winner-location":
                    room.setWinnerLocation(player.getLocation());
                    plugin.getRoomManager().saveRoom(room);
                    player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    break;
                case "view-equipment":
                    new com.fabian.xrooms.menus.EquipmentViewMenu(room, true).open(player);
                    break;
                case "abilities":
                case "ability":
                    player.sendMessage(plugin.getConfigManager().getMessage("abilities-info"));
                    player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit-abilities").replace("{room}", roomName)));
                    String ability = args[3].toLowerCase();
                    int level = 1;
                    if (args.length >= 5) {
                        try {
                            level = Integer.parseInt(args[4]);
                        } catch (NumberFormatException e) {
                            level = 1;
                        }
                    }

                    int current = room.getAbilities().getOrDefault(ability, 0);
                    if (current > 0 && args.length < 5) {
                        room.getAbilities().put(ability, 0);
                    } else {
                        room.getAbilities().put(ability, level);
                    }

                    plugin.getRoomManager().saveRoom(room);
                    int newLevel = room.getAbilities().getOrDefault(ability, 0);
                    String status = newLevel > 0 ? 
                            plugin.getConfigManager().getMessageRaw("gui-ability-status-on").replace("{level}", String.valueOf(newLevel)) : 
                            plugin.getConfigManager().getMessageRaw("gui-ability-status-off");
                            
                    plugin.getConfigManager().sendMessageRaw(player, 
                        plugin.getConfigManager().getMessageRaw("room-edited").replace("{room}", roomName) 
                        + " &7(" + status + "&7)"
                    );
                    break;
                case "reward":
                case "equipment":
                    new com.fabian.xrooms.menus.RoomEditMenu(room).open(player);
                    break;
                case "schematic-reset":
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", "/xroom edit " + roomName + " schematic-reset <true/false>"));
                        return true;
                    }
                    boolean enable = Boolean.parseBoolean(args[3]);
                    room.setSchematicEnabled(enable);
                    plugin.getRoomManager().saveRoom(room);
                    player.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", roomName));
                    break;
                default:
                    player.sendMessage(plugin.getConfigManager().getMessage("usage").replace("{usage}", plugin.getConfigManager().getMessageRaw("usage-edit")));
                    break;
            }
            return true;
        }

        return true;
    }

    private void sendHelp(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        player.sendMessage(cm.getMessage("help-header"));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-gui")));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-create")));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-delete")));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-redefine")));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-tp")));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-saveschematic")));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-edit")));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-edit-options")));
        player.sendMessage(cm.color("&f" + cm.getMessage("help-reload")));
        player.sendMessage(cm.getMessage("help-footer"));
    }

    private void sendConsoleHelp(CommandSender sender) {
        ConfigManager cm = plugin.getConfigManager();
        sender.sendMessage(cm.getMessage("help-header"));
        sender.sendMessage(cm.color("&f" + cm.getMessage("help-reload")));
        sender.sendMessage(cm.color("&f" + cm.getMessage("help-console-only")));
        sender.sendMessage(cm.getMessage("help-footer"));
    }
}
