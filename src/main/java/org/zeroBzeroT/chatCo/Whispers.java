package org.zeroBzeroT.chatCo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import static org.zeroBzeroT.chatCo.Utils.componentFromLegacyText;
import static org.zeroBzeroT.chatCo.Utils.now;
import static org.zeroBzeroT.chatCo.Utils.stripColor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public record Whispers(Main plugin) implements Listener {

    private static Map<String, TextColor> colorMap = Map.of();

    public Whispers(Main plugin) {
        this.plugin = plugin;
        colorMap = new HashMap<>();
        initializeColorMap();
    }

    private void initializeColorMap() {
        colorMap.put("%BLACK%", TextColor.color(0, 0, 0));
        colorMap.put("%DARK_BLUE%", TextColor.color(0, 0, 170));
        colorMap.put("%DARK_GREEN%", TextColor.color(0, 170, 0));
        colorMap.put("%DARK_AQUA%", TextColor.color(0, 170, 170));
        colorMap.put("%DARK_RED%", TextColor.color(170, 0, 0));
        colorMap.put("%DARK_PURPLE%", TextColor.color(170, 0, 170));
        colorMap.put("%GOLD%", TextColor.color(255, 170, 0));
        colorMap.put("%GRAY%", TextColor.color(170, 170, 170));
        colorMap.put("%DARK_GRAY%", TextColor.color(85, 85, 85));
        colorMap.put("%BLUE%", TextColor.color(85, 85, 255));
        colorMap.put("%GREEN%", TextColor.color(85, 255, 85));
        colorMap.put("%AQUA%", TextColor.color(85, 255, 255));
        colorMap.put("%RED%", TextColor.color(255, 85, 85));
        colorMap.put("%LIGHT_PURPLE%", TextColor.color(255, 85, 255));
        colorMap.put("%YELLOW%", TextColor.color(255, 255, 85));
        colorMap.put("%WHITE%", TextColor.color(255, 255, 255));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final String[] args = event.getMessage().split(" ");
        final Player sender = event.getPlayer();

        if (plugin.getConfig().getBoolean("ChatCo.lastCommand", true) && (args[0].equalsIgnoreCase("/l") || args[0].equalsIgnoreCase("/last"))) {
            if (args.length == 1) {
                sender.sendMessage(componentFromLegacyText("&eUsage: /l <message>"));
                event.setCancelled(true);
                return;
            }

            final Player target = plugin.getChatPlayer(sender).getLastReceiver();

            if ((target == null && plugin.getChatPlayer(sender).LastReceiver != null)
                    || Utils.isVanished(target)) {
                sender.sendMessage(componentFromLegacyText("&cThe last person you sent a private message to is offline."));
            } else if (target == null) {
                sender.sendMessage(componentFromLegacyText("&cYou have not initiated any private message in this session."));
            } else {
                String message = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
                sendPrivateMessage(sender, target, message);
            }

            event.setCancelled(true);
        } else if (plugin.getConfig().getBoolean("ChatCo.replyCommands", true) && (args[0].equalsIgnoreCase("/r") || args[0].equalsIgnoreCase("/reply"))) {
            if (args.length == 1) {
                sender.sendMessage(componentFromLegacyText("&eUsage: /r <message>"));
                event.setCancelled(true);
                return;
            }

            final Player target = plugin.getChatPlayer(sender).getLastMessenger();

            if ((target == null && plugin.getChatPlayer(sender).LastMessenger != null)
                    || Utils.isVanished(target)) {
                sender.sendMessage(componentFromLegacyText("&cThe last person you received a private message from is offline."));
            } else if (target == null) {
                sender.sendMessage(componentFromLegacyText("&cYou have not received any private messages in this session."));
            } else {
                String message = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
                sendPrivateMessage(sender, target, message);
            }

            event.setCancelled(true);
        } else if (args[0].equalsIgnoreCase("/tell") || args[0].equalsIgnoreCase("/msg") || args[0].equalsIgnoreCase("/t") || args[0].equalsIgnoreCase("/w") || args[0].equalsIgnoreCase("/whisper") || args[0].equalsIgnoreCase("/pm")) {
            if (args.length < 3) {
                sender.sendMessage(componentFromLegacyText("&eUsage: /w <player> <message>"));
                event.setCancelled(true);
                return;
            }

            final Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null || Utils.isVanished(target)) {
                sender.sendMessage(componentFromLegacyText("&c" + args[1] + " is offline."));
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
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(target, "Target cannot be null");
        
        String legacyMessage = send ? plugin.getConfig().getString("ChatCo.whisperFormat.send") : plugin.getConfig().getString("ChatCo.whisperFormat.receive");
        if (legacyMessage == null) {
            legacyMessage = send ? "&7To &f%RECEIVER%&7: " : "&7From &f%SENDER%&7: ";
        }

        for (Map.Entry<String, TextColor> entry : colorMap.entrySet()) {
            Component colorComponent = Component.text("").color(entry.getValue());
            String legacyColorCode = LegacyComponentSerializer.legacySection().serialize(colorComponent).substring(0, 2);
            legacyMessage = legacyMessage.replace(entry.getKey(), legacyColorCode);
        }

        String senderName = sender.getName();
        String targetName = target.getName();
        
        if (send) {
            legacyMessage = legacyMessage.replace("%SENDER%", senderName);
            legacyMessage = legacyMessage.replace("%RECEIVER%", targetName);
        } else {
            legacyMessage = legacyMessage.replace("%RECEIVER%", targetName);
            legacyMessage = legacyMessage.replace("%SENDER%", senderName);
        }

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

        TextComponent coloredMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        receiverMessage = receiverMessage.append(coloredMessage);
        senderMessage = senderMessage.append(coloredMessage);

        boolean isBlackholed = BlackholeModule.isPlayerBlacklisted(sender);
        if (isBlackholed && !BlackholeModule.isPlayerHidden(sender)) {
            plugin.getLogger().log(Level.INFO, "Blocked message from {0}: {1}",
                    new Object[]{sender.getName(), stripColor(LegacyComponentSerializer.legacySection().serialize(senderMessage))});
        }

        sender.sendMessage(senderMessage);

        if (isIgnoring && plugin.getConfig().getBoolean("ChatCo.ignoreMessageEnabled", true)) {
            sender.sendMessage(componentFromLegacyText("&c" + receiver.getName() + " is ignoring you."));
        } else if (doNotSend && plugin.getConfig().getBoolean("ChatCo.chatDisabledMessageEnabled", true)) {
            sender.sendMessage(componentFromLegacyText("&c" + receiver.getName() + "'s chat is disabled."));
        } else if (!doNotSend && !isIgnoring && !isBlackholed) {
            receiver.sendMessage(receiverMessage);

            if (target != null)
                target.setLastMessenger(sender);
        }

        String logText = message;

        if (doNotSend || isIgnoring) {
            logText = "***WAS NOT SENT*** " + logText;
        }
        if (plugin.getConfig().getBoolean("ChatCo.whisperLog", false)) {
            whisperLog(logText, sender.getName());
        }
        if (plugin.getConfig().getBoolean("ChatCo.whisperMonitoring", false) && !BlackholeModule.isPlayerHidden(sender)) {
            plugin.getLogger().log(Level.INFO, "{0}: {1}", new Object[]{sender.getName(), logText});
        }
    }

    public void whisperLog(final String text, final String sender) {
        try (FileWriter fwo = new FileWriter(Main.WhisperLog, true);
            BufferedWriter bwo = new BufferedWriter(fwo)) {
            bwo.write(now() + " " + sender + ": " + text);
            bwo.newLine();
        } catch (IOException ioexception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write to whisper log", ioexception);
        }
    }
}
