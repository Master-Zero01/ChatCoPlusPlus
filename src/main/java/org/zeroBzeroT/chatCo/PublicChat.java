package org.zeroBzeroT.chatCo;

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
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;

import static org.zeroBzeroT.chatCo.Utils.componentFromLegacyText;

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
        for (ChatColor color : ChatColor.values()) {
            if (plugin.getConfig().getString("ChatCo.chatPrefixes." + color.name()) != null && message.startsWith(plugin.getConfig().getString("ChatCo.chatPrefixes." + color.name()))) {

                // check for global or player permission
                if (permissionConfig.getBoolean("ChatCo.chatPrefixes." + color.name(), false) || player.hasPermission("ChatCo.chatPrefixes." + color.name())) {
                    message = color + message;
                }

                // break here since we found a prefix color code
                break;
            }
        }

        return message;
    }

    public String replaceInlineColors(String message, final Player player) {
        for (ChatColor color : ChatColor.values()) {
            if ((permissionConfig.getBoolean("ChatCo.chatColors." + color.name(), false) || player.hasPermission("ChatCo.chatColors." + color.name()))
                    && plugin.getConfig().getString("ChatCo.chatColors." + color.name()) != null) {
                message = message.replace(plugin.getConfig().getString("ChatCo.chatColors." + color.name()), color.toString());
            }
        }

        return message;
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

                if (ChatColor.stripColor(legacyMessage).trim().isEmpty()) {
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
                            plugin.getLogger().info("Blocked message from " + player.getName() + ": " + ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(chatMessage)));
                        }
                        player.sendMessage(chatMessage);
                    } else {
                        for (Player recipient : player.getWorld().getPlayers()) {
                            try {
                                ChatPlayer chatPlayer = PublicChat.plugin.getChatPlayer(recipient);

                                if (chatPlayer.chatDisabled)
                                    continue;

                                if (chatPlayer.isIgnored(player.getName()) && PublicChat.plugin.getConfig().getBoolean("ChatCo.ignoresEnabled", true))
                                    continue;

                                recipient.sendMessage(chatMessage);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
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
