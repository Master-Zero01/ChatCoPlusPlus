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
import static org.zeroBzeroT.chatCo.Utils.stripColor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class PublicChat implements Listener {
    public static Main plugin = null;
    private final FileConfiguration permissionConfig;

    public PublicChat(final Main plugin) {
        PublicChat.plugin = plugin;
        File customConfig = Main.PermissionConfig;
        permissionConfig = YamlConfiguration.loadConfiguration(customConfig);
        setupListener();
    }

    public String replacePrefixColors(String message, final Player player) {
        for (String colorName : Utils.getNamedColors().keySet()) {
            String prefix = plugin.getConfig().getString("ChatCo.chatPrefixes." + colorName);
            if (prefix != null && message.startsWith(prefix)) {
                // check for global or player permission
                if (permissionConfig.getBoolean("ChatCo.chatPrefixes." + colorName, false) || player.hasPermission("ChatCo.chatPrefixes." + colorName)) {
                    // Use hardcoded color code for reliability
                    String colorCode = getDirectColorCode(colorName);
                    message = colorCode + message;
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
                // Use hardcoded color code for reliability
                String colorCode = getDirectColorCode(colorName);
                message = message.replace(configColorCode, colorCode);
            }
        }

        return message;
    }

    /**
     * Get direct color code string without using serialization
     */
    private String getDirectColorCode(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "BLACK" -> "§0";
            case "DARK_BLUE" -> "§1";
            case "DARK_GREEN" -> "§2";
            case "DARK_AQUA" -> "§3";
            case "DARK_RED" -> "§4";
            case "DARK_PURPLE" -> "§5";
            case "GOLD" -> "§6";
            case "GRAY" -> "§7";
            case "DARK_GRAY" -> "§8";
            case "BLUE" -> "§9";
            case "GREEN" -> "§a";
            case "AQUA" -> "§b";
            case "RED" -> "§c";
            case "LIGHT_PURPLE" -> "§d";
            case "YELLOW" -> "§e";
            case "WHITE" -> "§f";
            case "BOLD" -> "§l";
            case "ITALIC" -> "§o";
            case "UNDERLINE" -> "§n";
            case "STRIKETHROUGH" -> "§m";
            case "MAGIC" -> "§k";
            default -> "§f"; // Default to white
        };
    }

    private void setupListener() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(PublicChat.plugin,
                ListenerPriority.LOWEST,
                PacketType.Play.Client.CHAT) {
            @SuppressWarnings("deprecation")
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                String message = event.getPacket().getStrings().read(0);

                String legacyMessage = LegacyComponentSerializer.legacyAmpersand().serialize(Component.text(message));
                legacyMessage = replacePrefixColors(legacyMessage, player);
                legacyMessage = replaceInlineColors(legacyMessage, player);

                if (stripColor(legacyMessage).trim().isEmpty()) {
                    event.setCancelled(true);
                    return;
                }

                TextComponent messageText = componentFromLegacyText(legacyMessage);
                TextComponent messageSender = componentFromLegacyText(player.getDisplayName());

                if (PublicChat.plugin.getConfig().getBoolean("ChatCo.whisperOnClick", true)) {
                    messageSender = messageSender.clickEvent(ClickEvent.suggestCommand("/w " + player.getName() + " "));
                    messageSender = messageSender.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Whisper to " + player.getName())));
                }

                TextComponent chatMessage = Component.text("")
                        .append(componentFromLegacyText("<"))
                        .append(messageSender)
                        .append(componentFromLegacyText("> "))
                        .append(messageText);

                boolean isBlackholed = BlackholeModule.isPlayerBlacklisted(player);

                if (!PublicChat.plugin.getConfig().getBoolean("ChatCo.chatDisabled", false)) {
                    if (isBlackholed) {
                        if (!BlackholeModule.isPlayerHidden(player)) {
                            plugin.getLogger().log(Level.INFO, "Blocked message from {0}: {1}", new Object[]{player.getName(), stripColor(LegacyComponentSerializer.legacySection().serialize(chatMessage))});
                        }
                        player.sendMessage(chatMessage);
                    } else {
                        for (Player recipient : plugin.getServer().getOnlinePlayers()) {
                            try {
                                ChatPlayer chatPlayer = PublicChat.plugin.getChatPlayer(recipient);

                                if (chatPlayer.chatDisabled)
                                    continue;

                                if (chatPlayer.isIgnored(player.getName()) && PublicChat.plugin.getConfig().getBoolean("ChatCo.ignoresEnabled", true))
                                    continue;

                                recipient.sendMessage(chatMessage);
                            } catch (NullPointerException e) {
                                plugin.getLogger().log(Level.WARNING, "Error sending chat message", e);
                            }
                        }
                    }
                }
                event.setCancelled(true);
            }
        });
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
