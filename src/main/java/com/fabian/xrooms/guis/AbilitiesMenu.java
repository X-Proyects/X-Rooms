package com.fabian.xrooms.guis;

import com.cryptomorin.xseries.XMaterial;
import com.fabian.xrooms.models.Room;
import com.fabian.xrooms.utils.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;

public class AbilitiesMenu extends Menu {

    private final Room room;

    // Ability key -> display config
    private static final String[][] ABILITIES = {
        {"strength",        "DIAMOND_SWORD"},
        {"speed",           "SUGAR"},
        {"resistance",      "IRON_CHESTPLATE"},
        {"haste",           "GOLDEN_PICKAXE"},
        {"regeneration",    "GOLDEN_APPLE"},
        {"fire_resistance", "MAGMA_CREAM"},
        {"jump",            "RABBIT_FOOT"},
        {"saturation",      "COOKED_BEEF"},
        {"absorption",      "GOLDEN_APPLE"},
        {"fly",             "FEATHER"},
    };

    // Slots where each ability will be placed
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};

    public AbilitiesMenu(Room room) {
        this.room = room;
    }

    @Override
    public String getMenuName() {
        return plugin.getConfigManager().getMessageRaw("gui-abilities-title").replace("{name}", room.getDisplayName());
    }

    @Override
    public int getSlots() {
        return 36;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (e.getSlot() == 31) {
            new RoomEditMenu(room).open(p);
            return;
        }

        for (int i = 0; i < ABILITIES.length; i++) {
            if (e.getSlot() == SLOTS[i]) {
                String key = ABILITIES[i][0].toLowerCase();
                int current = room.getAbilities().getOrDefault(key, 0);
                int limit = plugin.getConfigManager().getAbilitiesLimits().getOrDefault(key, 5);

                if (e.isShiftClick() && e.isLeftClick()) {
                    String abilityName = plugin.getConfigManager().getMessageRaw("gui-ability-" + key);
                    String prompt = plugin.getConfigManager().getMessageRaw("gui-ability-set-level-prompt")
                            .replace("{ability}", abilityName);
                    
                    plugin.getChatInputManager().requestInput(p, prompt, input -> {
                        try {
                            int level = Integer.parseInt(input);
                            if (level > limit) {
                                p.sendMessage(plugin.getConfigManager().getMessageRaw("gui-ability-limit-reached")
                                        .replace("{ability}", abilityName)
                                        .replace("{limit}", String.valueOf(limit)));
                                level = limit;
                            }
                            room.getAbilities().put(key, Math.max(0, level));
                            plugin.getRoomManager().saveRoom(room);
                            p.sendMessage(plugin.getConfigManager().getMessage("room-edited").replace("{room}", room.getName()));
                            this.open(p);
                        } catch (NumberFormatException ex) {
                            p.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
                            this.open(p);
                        }
                    }, () -> this.open(p));
                    return;
                }

                if (e.isLeftClick()) {
                    // Increase level with limit
                    if (current >= limit) {
                        String abilityName = plugin.getConfigManager().getMessageRaw("gui-ability-" + key);
                        p.sendMessage(plugin.getConfigManager().getMessageRaw("gui-ability-limit-reached")
                                .replace("{ability}", abilityName)
                                .replace("{limit}", String.valueOf(limit)));
                        return;
                    }
                    int newLevel = current + 1;
                    room.getAbilities().put(key, newLevel);
                } else if (e.isRightClick()) {
                    // Decrease level (min 0 = OFF)
                    int newLevel = Math.max(current - 1, 0);
                    room.getAbilities().put(key, newLevel);
                }

                plugin.getRoomManager().saveRoom(room);
                // Refresh menu
                new AbilitiesMenu(room).open(p);
                return;
            }
        }
    }

    @Override
    public void setMenuItems() {
        DebugLogger.debug("AbilitiesMenu", "Opening abilities menu for room " + room.getName());
        fillBorders(XMaterial.PURPLE_STAINED_GLASS_PANE.parseMaterial());

        for (int i = 0; i < ABILITIES.length; i++) {
            String key = ABILITIES[i][0].toLowerCase();
            String materialName = ABILITIES[i][1];
            String displayName = plugin.getConfigManager().getMessageRaw("gui-ability-" + key);
            int level = room.getAbilities().getOrDefault(key, 0);

            String status = level > 0
                    ? plugin.getConfigManager().getMessageRaw("gui-ability-status-on").replace("{level}", String.valueOf(level))
                    : plugin.getConfigManager().getMessageRaw("gui-ability-status-off");

            inventory.setItem(SLOTS[i], createItem(
                    XMaterial.matchXMaterial(materialName).orElse(XMaterial.BARRIER).parseMaterial(),
                    displayName,
                    Arrays.asList(
                            "",
                            status,
                            "",
                            plugin.getConfigManager().getMessageRaw("gui-ability-left-click"),
                            plugin.getConfigManager().getMessageRaw("gui-ability-right-click"),
                            plugin.getConfigManager().getMessageRaw("gui-ability-shift-click")
                    )
            ));
        }

        inventory.setItem(31, createItem(XMaterial.ARROW.parseMaterial(),
                plugin.getConfigManager().getMessageRaw("gui-back"), null));
    }
}
