package org.zeroBzeroT.chatCo;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zeroBzeroT.chatCo.Utils.componentFromLegacyText;
import static org.zeroBzeroT.chatCo.Utils.now;

public record Whispers(Main plugin) implements Listener {

    private static Map<String, ChatColor> colorMap = Map.of();

    public Whispers(Main plugin) {
        this.plugin = plugin;
        colorMap = new HashMap<>();
        initializeColorMap();
    }

    private void initializeColorMap() {
        colorMap.put("%BLACK%", ChatColor.BLACK);
        colorMap.put("%DARK_BLUE%", ChatColor.DARK_BLUE);
        colorMap.put("%DARK_GREEN%", ChatColor.DARK_GREEN);
        colorMap.put("%DARK_AQUA%", ChatColor.DARK_AQUA);
        colorMap.put("%DARK_RED%", ChatColor.DARK_RED);
        colorMap.put("%DARK_PURPLE%", ChatColor.DARK_PURPLE);
        colorMap.put("%GOLD%", ChatColor.GOLD);
        colorMap.put("%GRAY%", ChatColor.GRAY);
        colorMap.put("%DARK_GRAY%", ChatColor.DARK_GRAY);
        colorMap.put("%BLUE%", ChatColor.BLUE);
        colorMap.put("%GREEN%", ChatColor.GREEN);
        colorMap.put("%AQUA%", ChatColor.AQUA);
        colorMap.put("%RED%", ChatColor.RED);
        colorMap.put("%LIGHT_PURPLE%", ChatColor.LIGHT_PURPLE);
        colorMap.put("%YELLOW%", ChatColor.YELLOW);
        colorMap.put("%WHITE%", ChatColor.WHITE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final String[] args = event.getMessage().split(" ");
        final Player sender = event.getPlayer();

        if (plugin.getConfig().getBoolean("ChatCo.lastCommand", true) && (args[0].equalsIgnoreCase("/l") || args[0].equalsIgnoreCase("/last"))) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /l <message>");
                event.setCancelled(true);
                return;
            }

            final Player target = plugin.getChatPlayer(sender).getLastReceiver();

            if ((target == null && plugin.getChatPlayer(sender).LastReceiver != null)
                    || Utils.isVanished(target)) {
                sender.sendMessage(ChatColor.RED + "The last person you sent a private message to is offline.");
            } else if (target == null) {
                sender.sendMessage(ChatColor.RED + "You have not initiated any private message in this session.");
            } else {
                String message = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
                sendPrivateMessage(sender, target, message);
            }

            event.setCancelled(true);
        } else if (plugin.getConfig().getBoolean("ChatCo.replyCommands", true) && (args[0].equalsIgnoreCase("/r") || args[0].equalsIgnoreCase("/reply"))) {
            if (args.length == 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /r <message>");
                event.setCancelled(true);
                return;
            }

            final Player target = plugin.getChatPlayer(sender).getLastMessenger();

            if ((target == null && plugin.getChatPlayer(sender).LastMessenger != null)
                    || Utils.isVanished(target)) {
                sender.sendMessage(ChatColor.RED + "The last person you received a private message from is offline.");
            } else if (target == null) {
                sender.sendMessage(ChatColor.RED + "You have not received any private messages in this session.");
            } else {
                String message = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
                sendPrivateMessage(sender, target, message);
            }

            event.setCancelled(true);
        } else if (args[0].equalsIgnoreCase("/tell") || args[0].equalsIgnoreCase("/msg") || args[0].equalsIgnoreCase("/t") || args[0].equalsIgnoreCase("/w") || args[0].equalsIgnoreCase("/whisper") || args[0].equalsIgnoreCase("/pm")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /w <player> <message>");
                event.setCancelled(true);
                return;
            }

            final Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null || Utils.isVanished(target)) {
                sender.sendMessage(ChatColor.RED + args[1] + " is offline.");
                event.setCancelled(true);
                return;
            }

            if (plugin.getConfig().getBoolean("ChatCo.newCommands", true)) {
                String message = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
                sendPrivateMessage(sender, target, message);
                event.setCancelled(true);
                plugin.getChatPlayer(sender).setLastReceiver(target);
            } else if (args[0].equalsIgnoreCase("/tell ") || args[0].equalsIgnoreCase("/w ") || args[0].equalsIgnoreCase("/msg ")) {
                String message = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));
                sendPrivateMessage(sender, target, message);
                event.setCancelled(true);
                plugin.getChatPlayer(sender).setLastReceiver(target);
            }
        }
    }

    public TextComponent whisperFormat(Boolean send, final Player sender, final Player target) {
        String legacyMessage = send ? plugin.getConfig().getString("ChatCo.whisperFormat.send") : plugin.getConfig().getString("ChatCo.whisperFormat.receive");

        // Replace placeholders with their corresponding ChatColor values
        for (Map.Entry<String, ChatColor> entry : colorMap.entrySet()) {
            legacyMessage = legacyMessage.replace(entry.getKey(), entry.getValue().toString());
        }

        // Process the full message first
        if (send) {
            legacyMessage = legacyMessage.replace("%SENDER%", sender.getName());
            legacyMessage = legacyMessage.replace("%RECEIVER%", target.getName());
        } else {
            legacyMessage = legacyMessage.replace("%RECEIVER%", target.getName());
            legacyMessage = legacyMessage.replace("%SENDER%", sender.getName());
        }

        // Now we can safely deserialize the whole message
        return LegacyComponentSerializer.legacySection().deserialize(legacyMessage);
    }

    private void sendPrivateMessage(Player sender, Player receiver, String message) {
        boolean doNotSend = false;
        boolean isIgnoring = false;
        ChatPlayer target = plugin.getChatPlayer(receiver);

        if (target != null && target.tellsDisabled) {
            doNotSend = true;
        }

        if (target != null && target.isIgnored(sender.getName())) {
            isIgnoring = true;
        }

        TextComponent senderMessage = whisperFormat(true, sender, receiver);
        TextComponent receiverMessage = whisperFormat(false, sender, receiver);

        receiverMessage = receiverMessage.append(Component.text(ChatColor.translateAlternateColorCodes('&', message)));
        senderMessage = senderMessage.append(Component.text(ChatColor.translateAlternateColorCodes('&', message)));

        boolean isBlackholed = BlackholeModule.isPlayerBlacklisted(sender);
        if (isBlackholed) {
            plugin.getLogger().info("Blocked message from " + sender.getName() + ": " + ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(senderMessage)));
        }

        sender.sendMessage(senderMessage);

        if (isIgnoring && plugin.getConfig().getBoolean("ChatCo.ignoreMessageEnabled", true)) {
            sender.sendMessage(ChatColor.RED + receiver.getName() + " is ignoring you.");
        } else if (doNotSend && plugin.getConfig().getBoolean("ChatCo.chatDisabledMessageEnabled", true)) {
            sender.sendMessage(ChatColor.RED + receiver.getName() + "'s chat is disabled.");
        } else if (!doNotSend && !isIgnoring && !isBlackholed) {
            receiver.sendMessage(receiverMessage);

            if (target != null)
                target.setLastMessenger(sender);
        }

        // Logging
        String logText = message;

        if (doNotSend || isIgnoring) {
            logText = "***WAS NOT SENT*** " + logText;
        }
        if (plugin.getConfig().getBoolean("ChatCo.whisperLog", false)) {
            whisperLog(logText, sender.getName());
        }
        if (plugin.getConfig().getBoolean("ChatCo.whisperMonitoring", false)) {
            plugin.getLogger().info(sender.getName() + ": " + logText);
        }
    }

    public void whisperLog(final String text, final String sender) {
        try {
            final FileWriter fwo = new FileWriter(Main.WhisperLog, true);
            final BufferedWriter bwo = new BufferedWriter(fwo);
            bwo.write(now() + " " + sender + ": " + text);
            bwo.newLine();
            bwo.close();
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }
}
