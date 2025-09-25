package org.zeroBzeroT.chatCo;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import static org.zeroBzeroT.chatCo.Utils.componentFromLegacyText;
import static org.zeroBzeroT.chatCo.Utils.containsUnicode;
import static org.zeroBzeroT.chatCo.Utils.getDirectColorCode;
import static org.zeroBzeroT.chatCo.Utils.parseFormattingTags;
import static org.zeroBzeroT.chatCo.Utils.stripColor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Iterator;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class PublicChat implements Listener {
    public static Main plugin = null;
    private final FileConfiguration permissionConfig;

    public PublicChat(final Main plugin) {
        PublicChat.plugin = plugin;
        File customConfig = Main.PermissionConfig;
        permissionConfig = YamlConfiguration.loadConfiguration(customConfig);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public String replacePrefixColors(String message, final Player player) {
        for (String colorName : Utils.getNamedColors().keySet()) {
            String prefix = plugin.getConfig().getString("ChatCo.chatPrefixes." + colorName);
            if (prefix != null && message.startsWith(prefix)) {
                // check for global or player permission
                if (permissionConfig.getBoolean("ChatCo.chatPrefixes." + colorName, false) || player.hasPermission("ChatCo.chatPrefixes." + colorName)) {
                    message = getDirectColorCode(colorName) + message;
                }

                // break here since we found a prefix color code
                break;
            }
        }

        return message;
    }

    public String replaceInlineColors(String message, final Player player) {
        for (String colorName : Utils.getNamedColors().keySet()) {
            String configColorCode = plugin.getConfig().getString("ChatCo.chatColors." + colorName);
            if (configColorCode != null && (permissionConfig.getBoolean("ChatCo.chatColors." + colorName, false) || player.hasPermission("ChatCo.chatColors." + colorName))) {
                message = message.replace(configColorCode, getDirectColorCode(colorName));
            }
        }

        return message;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void preProcessChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Check for unicode characters if the feature is enabled
        if (PublicChat.plugin.getConfig().getBoolean("ChatCo.blockUnicodeText", false) && containsUnicode(message)) {
            // Log blocked message if debug is enabled
            if (PublicChat.plugin.getConfig().getBoolean("ChatCo.debugUnicodeBlocking", false)) {
                plugin.getLogger().info("Blocked unicode message from " + player.getName() + ": " + message);
            }
            event.setCancelled(true);
            return;
        }
        
        // Check for blacklisted words
        if (PublicChat.plugin.getBlacklistFilter().containsBlacklistedWord(message)) {
            // Log blocked message if debug is enabled
            if (PublicChat.plugin.getConfig().getBoolean("ChatCo.debugBlacklistBlocking", false)) {
                plugin.getLogger().info("Blocked blacklisted word from " + player.getName() + ": " + message);
            }
            event.setCancelled(true);
            return;
        }

        // Apply prefix colors
        String legacyMessage = replacePrefixColors(message, player);
        
        // Apply inline colors
        legacyMessage = replaceInlineColors(legacyMessage, player);
        
        // Parse any formatting tags like <BOLD>, <UNDERLINE>, etc.
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
        boolean chatDisabledGlobal = PublicChat.plugin.getConfig().getBoolean("ChatCo.chatDisabled", false);
        if (chatDisabledGlobal) {
            event.setCancelled(true);
            return;
        }

        boolean isBlackholed = BlackholeModule.isPlayerBlacklisted(player);

        Iterator<Player> iterator = event.getRecipients().iterator();
        while (iterator.hasNext()) {
            Player recipient = iterator.next();
            if (recipient.equals(player)) {
                continue; // Sender always sees their own message
            }

            ChatPlayer chatPlayer = PublicChat.plugin.getChatPlayer(recipient);
            if (chatPlayer != null) {
                if (chatPlayer.chatDisabled) {
                    iterator.remove();
                    continue;
                }
                if (chatPlayer.isIgnored(player.getName()) && PublicChat.plugin.getConfig().getBoolean("ChatCo.ignoresEnabled", true)) {
                    iterator.remove();
                    continue;
                }
            }
        }

        if (isBlackholed) {
            // Only sender sees it; remove all other recipients
            iterator = event.getRecipients().iterator();
            while (iterator.hasNext()) {
                Player recipient = iterator.next();
                if (!recipient.equals(player)) {
                    iterator.remove();
                }
            }
            // Log blocked message if not hidden
            if (!BlackholeModule.isPlayerHidden(player)) {
                String legacyMessage = event.getMessage();
                plugin.getLogger().log(Level.INFO, "Blocked message from {0}: {1}", new Object[]{player.getName(), stripColor(legacyMessage)});
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void logChatToConsole(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!PublicChat.plugin.getConfig().getBoolean("ChatCo.chatToConsole", true)) {
            return;
        }
        Player player = event.getPlayer();
        String fullMessage = "<" + stripColor(player.getDisplayName()) + "> " + stripColor(event.getMessage());
        plugin.getLogger().log(Level.INFO, "[CHAT] {0}", fullMessage);
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        plugin.remove(e.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent e) {
        plugin.remove(e.getPlayer());
    }
}
