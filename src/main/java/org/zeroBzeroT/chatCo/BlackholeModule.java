package org.zeroBzeroT.chatCo;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class BlackholeModule implements Listener {

    private static JavaPlugin plugin = null;
    private static final Set<String> blacklist = new HashSet<>();

    public BlackholeModule(JavaPlugin plugin) {
        BlackholeModule.plugin = plugin;
        loadBlacklist();
    }

    public static void addPlayerToBlacklist(Player player) {
        blacklist.add(player.getUniqueId().toString());
        saveBlacklist();
    }

    public static void removePlayerFromBlacklist(Player player) {
        blacklist.remove(player.getUniqueId().toString());
        saveBlacklist();
    }

    public static boolean isPlayerBlacklisted(Player player) {
        return blacklist.contains(player.getUniqueId().toString());
    }

    public static void reloadConfiguration() {
        plugin.reloadConfig();
        loadBlacklist();
    }

    private static void saveBlacklist() {
        List<String> blacklistEntries = new ArrayList<>(blacklist);
        plugin.getConfig().set("blacklist", blacklistEntries);
        plugin.saveConfig();
    }

    private static void loadBlacklist() {
        List<String> blacklistEntries = plugin.getConfig().getStringList("blacklist");
        blacklist.clear();
        blacklist.addAll(blacklistEntries);
    }
}
