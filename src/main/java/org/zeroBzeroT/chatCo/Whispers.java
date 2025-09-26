package org.zeroBzeroT.chatCo;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import static org.zeroBzeroT.chatCo.Utils.*;

public class Whispers implements Listener {
    private final Main plugin;

    private static final Map<String, String> COLOR_PLACEHOLDER_MAP = new HashMap<>();

    static {
        for (String colorName : Utils.getNamedColors().keySet()) {
            COLOR_PLACEHOLDER_MAP.put("%" + colorName + "%", colorName);
        }
    }

    public Whispers(Main plugin) {
        this.plugin = plugin;
    }

    /** Apply whisper format from config */
    public TextComponent whisperFormat(boolean send, Player sender, Player target) {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(target, "Target cannot be null");

        String key = send ? "ChatCo.whisperFormat.send" : "ChatCo.whisperFormat.receive";
        String legacyMessage = plugin.getConfig().getString(key,
                send ? "&7To &f%RECEIVER%&7: " : "&7From &f%SENDER%&7: ");

        for (Map.Entry<String, String> entry : COLOR_PLACEHOLDER_MAP.entrySet()) {
            legacyMessage = legacyMessage.replace(entry.getKey(), getDirectColorCode(entry.getValue()));
        }

        return LegacyComponentSerializer.legacySection().deserialize(
                legacyMessage.replace("%SENDER%", sender.getName())
                             .replace("%RECEIVER%", target.getName())
        );
    }

    /** Run unicode + blacklist checks */
    private boolean isMessageAllowed(Player sender, String message) {
        if (plugin.getConfig().getBoolean("ChatCo.blockUnicodeText", false) && containsUnicode(message)) {
            if (plugin.getConfig().getBoolean("ChatCo.debugUnicodeBlocking", false)) {
                plugin.getLogger().info("Blocked unicode whisper from " + sender.getName() + ": " + message);
            }
            return false;
        }

        if (plugin.getBlacklistFilter().containsBlacklistedWord(message)) {
            if (plugin.getConfig().getBoolean("ChatCo.debugBlacklistBlocking", false)) {
                plugin.getLogger().info("Blocked blacklisted whisper from " + sender.getName() + ": " + message);
            }
            return false;
        }

        return true;
    }

    /** Send private message with full handling */
    private void sendPrivateMessage(Player sender, Player receiver, String message) {
        if (!isMessageAllowed(sender, message)) return;

        ChatPlayer target = plugin.getChatPlayer(receiver);
        boolean doNotSend = target != null && target.tellsDisabled;
        boolean isIgnoring = target != null && target.isIgnored(sender.getName());

        TextComponent senderMessage = whisperFormat(true, sender, receiver);
        TextComponent receiverMessage = whisperFormat(false, sender, receiver);

        message = parseFormattingTags(message);
        TextComponent coloredMessage = LegacyComponentSerializer.legacySection().deserialize(message);

        senderMessage = senderMessage.append(coloredMessage);
        receiverMessage = receiverMessage.append(coloredMessage);

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
            if (target != null) target.setLastMessenger(sender);
        }

        String logText = (doNotSend || isIgnoring ? "***WAS NOT SENT*** " : "") + message;
        if (plugin.getConfig().getBoolean("ChatCo.whisperLog", false)) {
            whisperLog(logText, sender.getName());
        }
        if (plugin.getConfig().getBoolean("ChatCo.whisperMonitoring", false) && !BlackholeModule.isPlayerHidden(sender)) {
            plugin.getLogger().log(Level.INFO, "{0}: {1}", new Object[]{sender.getName(), logText});
        } else if (plugin.getConfig().getBoolean("ChatCo.chatToConsole", true) && !BlackholeModule.isPlayerHidden(sender)) {
            plugin.getLogger().log(Level.INFO, "[WHISPER] {0} -> {1}: {2}",
                    new Object[]{sender.getName(), receiver.getName(), stripColor(logText)});
        }
    }

    /** Log whispers to file */
    private void whisperLog(String text, String sender) {
        try (BufferedWriter bwo = new BufferedWriter(new FileWriter(Main.WhisperLog, true))) {
            bwo.write(now() + " " + sender + ": " + text);
            bwo.newLine();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to write to whisper log", ex);
        }
    }

    // ---------------- Command Handling ----------------

    private void handleLastCommand(Player sender, String[] args, PlayerCommandPreprocessEvent event) {
        if (args.length < 2) {
            sender.sendMessage(componentFromLegacyText("&eUsage: /l <message>"));
            event.setCancelled(true);
            return;
        }

        Player target = plugin.getChatPlayer(sender).getLastReceiver();
        if (target == null || isVanished(target)) {
            sender.sendMessage(componentFromLegacyText("&cThe last person you sent a private message to is offline."));
            event.setCancelled(true);
            return;
        }

        String whisperMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (isMessageAllowed(sender, whisperMessage)) {
            sendPrivateMessage(sender, target, whisperMessage);
        }
        event.setCancelled(true);
    }

    private void handleReplyCommand(Player sender, String[] args, PlayerCommandPreprocessEvent event) {
        if (args.length < 2) {
            sender.sendMessage(componentFromLegacyText("&eUsage: /r <message>"));
            event.setCancelled(true);
            return;
        }

        Player target = plugin.getChatPlayer(sender).getLastMessenger();
        if (target == null || isVanished(target)) {
            sender.sendMessage(componentFromLegacyText("&cThe last person you received a private message from is offline."));
            event.setCancelled(true);
            return;
        }

        String whisperMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (isMessageAllowed(sender, whisperMessage)) {
            sendPrivateMessage(sender, target, whisperMessage);
        }
        event.setCancelled(true);
    }

    private void handleWhisperCommand(Player sender, String[] args, PlayerCommandPreprocessEvent event) {
        if (args.length < 3) {
            sender.sendMessage(componentFromLegacyText("&eUsage: /w <player> <message>"));
            event.setCancelled(true);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || isVanished(target)) {
            sender.sendMessage(componentFromLegacyText("&c" + args[1] + " is offline."));
            event.setCancelled(true);
            return;
        }

        String whisperMessage = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (isMessageAllowed(sender, whisperMessage)) {
            sendPrivateMessage(sender, target, whisperMessage);
            plugin.getChatPlayer(sender).setLastReceiver(target);
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        String[] args = event.getMessage().split(" ");
        String cmdName = args[0].substring(1).toLowerCase();

        if (plugin.getConfig().getBoolean("ChatCo.lastCommand", true) &&
            (cmdName.equals("l") || cmdName.equals("last"))) {
            handleLastCommand(sender, args, event);

        } else if (plugin.getConfig().getBoolean("ChatCo.replyCommands", true) &&
                   (cmdName.equals("r") || cmdName.equals("reply"))) {
            handleReplyCommand(sender, args, event);

        } else if (Arrays.asList("tell", "msg", "t", "w", "whisper", "pm").contains(cmdName)) {
            handleWhisperCommand(sender, args, event);
        }
    }
}
