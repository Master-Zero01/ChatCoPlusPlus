package org.zeroBzeroT.chatCo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class BlackholeModule implements Listener {

    private static JavaPlugin plugin = null;
    private static final Map<String, List<String>> playerSettings = new HashMap<>();
    private static final String HIDDEN_SETTING = "hidden";

    public BlackholeModule(JavaPlugin plugin) {
        BlackholeModule.plugin = plugin;
        loadSettings();
    }

    public static void addPlayerToBlacklist(Player player, boolean hidden) {
        String uuid = player.getUniqueId().toString();
        List<String> settings = new ArrayList<>();
        if (hidden) {
            settings.add(HIDDEN_SETTING);
        }
        playerSettings.put(uuid, settings);

        // Immediately update config
        plugin.getConfig().set("blacklist_settings." + uuid, settings);
        plugin.saveConfig();
    }

    public static void removePlayerFromBlacklist(Player player) {
        String uuid = player.getUniqueId().toString();
        playerSettings.remove(uuid);

        // Ensure the entry is removed from config
        plugin.getConfig().set("blacklist_settings." + uuid, null);
        plugin.saveConfig();
    }

    public static boolean isPlayerBlacklisted(Player player) {
        return playerSettings.containsKey(player.getUniqueId().toString());
    }

    public static boolean isPlayerHidden(Player player) {
        List<String> settings = playerSettings.get(player.getUniqueId().toString());
        return settings != null && settings.contains(HIDDEN_SETTING);
    }

    public static void setPlayerHidden(Player player, boolean hidden) {
        String uuid = player.getUniqueId().toString();
        List<String> settings = playerSettings.getOrDefault(uuid, new ArrayList<>());

        if (hidden && !settings.contains(HIDDEN_SETTING)) {
            settings.add(HIDDEN_SETTING);
        } else if (!hidden) {
            settings.remove(HIDDEN_SETTING);
        }

        playerSettings.put(uuid, settings);

        // Immediately update config
        plugin.getConfig().set("blacklist_settings." + uuid, settings);
        plugin.saveConfig();
    }

    public static void reloadConfiguration() {
        plugin.reloadConfig();
        loadSettings();
    }

    private static void loadSettings() {
        playerSettings.clear();

        // Load from config
        if (plugin.getConfig().contains("blacklist_settings")) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("blacklist_settings");
            if (section != null) {
                Map<String, Object> loadedData = section.getValues(false);
                for (Map.Entry<String, Object> entry : loadedData.entrySet()) {
                    if (entry.getValue() instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> settings = (List<String>) entry.getValue();
                        playerSettings.put(entry.getKey(), settings);
                    }
                }
            } else {
                plugin.getLogger().log(Level.WARNING, "Configuration contains 'blacklist_settings' key but it's not a valid section");
            }
        }

        // Check for entries in the old blacklist format and convert them
        List<String> oldBlacklist = plugin.getConfig().getStringList("blacklist");
        if (!oldBlacklist.isEmpty()) {
            for (String uuid : oldBlacklist) {
                if (!playerSettings.containsKey(uuid)) {
                    playerSettings.put(uuid, new ArrayList<>());
                    // Immediately save the conversion
                    plugin.getConfig().set("blacklist_settings." + uuid, new ArrayList<>());
                }
            }
            // Remove old format and save
            plugin.getConfig().set("blacklist", null);
            plugin.saveConfig();
        }
    }
}
