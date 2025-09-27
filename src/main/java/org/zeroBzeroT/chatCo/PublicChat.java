package org.zeroBzeroT.chatCo;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static org.zeroBzeroT.chatCo.Utils.componentFromLegacyText;
import static org.zeroBzeroT.chatCo.Utils.containsUnicode;
import static org.zeroBzeroT.chatCo.Utils.getDirectColorCode;
import static org.zeroBzeroT.chatCo.Utils.parseFormattingTags;
import static org.zeroBzeroT.chatCo.Utils.stripColor;

public class PublicChat implements Listener {
    public static Main plugin = null;
    private final FileConfiguration permissionConfig;

    // Track recent messages per player for duplicate detection
    private final HashMap<UUID, LinkedList<String>> recentMessages = new HashMap<>();
    private final int duplicateThreshold;
    private final int duplicateHistorySize;

    public PublicChat(final Main plugin) {
        PublicChat.plugin = plugin;
        File customConfig = Main.PermissionConfig;
        permissionConfig = YamlConfiguration.loadConfiguration(customConfig);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        this.duplicateThreshold = plugin.getConfig().getInt("ChatCo.duplicateMessageThreshold", 2);
        this.duplicateHistorySize = plugin.getConfig().getInt("ChatCo.duplicateMessageHistorySize", 5);
    }

    public String replacePrefixColors(String message, final Player player) {
        for (String colorName : Utils.getNamedColors().keySet()) {
            String prefix = plugin.getConfig().getString("ChatCo.chatPrefixes." + colorName);
            if (prefix != null && message.startsWith(prefix)) {
                if (permissionConfig.getBoolean("ChatCo.chatPrefixes." + colorName, false) ||
                        player.hasPermission("ChatCo.chatPrefixes." + colorName)) {
                    message = getDirectColorCode(colorName) + message;
                }
                break;
            }
        }
        return message;
    }

    public String replaceInlineColors(String message, final Player player) {
        for (String colorName : Utils.getNamedColors().keySet()) {
            String configColorCode = plugin.getConfig().getString("ChatCo.chatColors." + colorName);
            if (configColorCode != null &&
                    (permissionConfig.getBoolean("ChatCo.chatColors." + colorName, false) ||
                            player.hasPermission("ChatCo.chatColors." + colorName))) {
                message = message.replace(configColorCode, getDirectColorCode(colorName));
            }
        }
        return message;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void preProcessChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Check for Unicode blocking
        if (plugin.getConfig().getBoolean("ChatCo.blockUnicodeText", false) && containsUnicode(message)) {
            if (plugin.getConfig().getBoolean("ChatCo.debugUnicodeBlocking", false)) {
                plugin.getLogger().info("Blocked unicode message from " + player.getName() + ": " + message);
            }
            event.setCancelled(true);
            return;
        }

        // Check for blacklisted words
        if (plugin.getBlacklistFilter().containsBlacklistedWord(message)) {
            if (plugin.getConfig().getBoolean("ChatCo.debugBlacklistBlocking", false)) {
                plugin.getLogger().info("Blocked blacklisted word from " + player.getName() + ": " + message);
            }
            event.setCancelled(true);
            return;
        }

        // Count-based duplicate prevention
        LinkedList<String> history = recentMessages.computeIfAbsent(player.getUniqueId(), k -> new LinkedList<>());
        String strippedMessage = stripColor(message.toLowerCase().trim());
        int duplicateCount = 0;
        for (String pastMessage : history) {
            if (strippedMessage.equals(stripColor(pastMessage.toLowerCase().trim()))) {
                duplicateCount++;
            }
        }

        if (duplicateCount >= duplicateThreshold) {
            player.sendMessage(componentFromLegacyText("&cYou are sending duplicate messages too often!"));
            event.setCancelled(true);
            return;
        }

        history.add(message);
        if (history.size() > duplicateHistorySize) {
            history.removeFirst();
        }

        // Apply colors and formatting
        String legacyMessage = replacePrefixColors(message, player);
        legacyMessage = replaceInlineColors(legacyMessage, player);
        legacyMessage = parseFormattingTags(legacyMessage);

        if (stripColor(legacyMessage).trim().isEmpty()) {
            event.setCancelled(true);
            return;
        }

        event.setMessage(legacyMessage);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void filterChatRecipients(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        boolean chatDisabledGlobal = plugin.getConfig().getBoolean("ChatCo.chatDisabled", false);
        if (chatDisabledGlobal) {
            event.setCancelled(true);
            return;
        }

        boolean isBlackholed = BlackholeModule.isPlayerBlacklisted(player);

        Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();
            if (recipient.equals(player)) continue;

            ChatPlayer chatPlayer = plugin.getChatPlayer(recipient);
            if (chatPlayer != null) {
                if (chatPlayer.chatDisabled || 
                        (chatPlayer.isIgnored(player.getName()) && plugin.getConfig().getBoolean("ChatCo.ignoresEnabled", true))) {
                    iterator.remove();
                }
            }
        }

        if (isBlackholed) {
            iterator = event.getRecipients().iterator();
            while (iterator.hasNext()) {
                Player recipient = iterator.next();
                if (!recipient.equals(player)) iterator.remove();
            }

            if (!BlackholeModule.isPlayerHidden(player)) {
                plugin.getLogger().log(Level.INFO, "Blocked message from {0}: {1}", new Object[]{player.getName(), stripColor(event.getMessage())});
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void logChatToConsole(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!plugin.getConfig().getBoolean("ChatCo.chatToConsole", true)) return;

        Player player = event.getPlayer();
        String fullMessage = "<" + stripColor(player.getDisplayName()) + "> " + stripColor(event.getMessage());
        plugin.getLogger().log(Level.INFO, "[CHAT] {0}", fullMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        plugin.remove(e.getPlayer());
        recentMessages.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent e) {
        plugin.remove(e.getPlayer());
        recentMessages.remove(e.getPlayer().getUniqueId());
    }
}
