package org.zeroBzeroT.chatCo;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Announcer {
    private final JavaPlugin plugin;
    private List<String> announcements;
    private String prefix;
    private int delay;
    private int currentIndex;
    private BukkitRunnable announcementTask;

    private static final Pattern FORMAT_PATTERN = Pattern.compile("<([A-Z_]+)>");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://)([-\\w.]+)(/\\S*)?");

    private static final List<String> FORMAT_TAGS = List.of(
            "BOLD", "ITALIC", "UNDERLINE", "STRIKETHROUGH", "MAGIC"
    );

    public Announcer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentIndex = 0;  // Initialize index
        loadConfig();
    }

    public void loadConfig() {
        // Cancel existing task if it exists
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }

        FileConfiguration config = plugin.getConfig();
        announcements = config.getStringList("ChatCo.announcements.messages");
        prefix = org.bukkit.ChatColor.GOLD + "[" + org.bukkit.ChatColor.YELLOW + "Anarchadia" + org.bukkit.ChatColor.GOLD + "]";
        delay = config.getInt("ChatCo.announcements.delay", 300);
        currentIndex = 0;  // Reset index on reload

        // Only start if enabled
        if (config.getBoolean("ChatCo.announcements.enabled", true)) {
            startAnnouncementTask();
        }
    }

    private void startAnnouncementTask() {
        if (announcements == null || announcements.isEmpty()) {
            plugin.getLogger().warning("No announcements configured!");
            return;
        }

        // Create new task
        announcementTask = new BukkitRunnable() {
            @Override
            public void run() {
                broadcastAnnouncement();
            }
        };

        // Start the task with the configured delay
        announcementTask.runTaskTimer(plugin, 20L * delay, 20L * delay);
    }

    private void broadcastAnnouncement() {
        if (announcements.isEmpty()) return;

        // Get the current message
        String message = announcements.get(currentIndex);

        // Create and broadcast the component
        TextComponent component = parseMessage(message);
        Bukkit.spigot().broadcast(component);

        // Update index for next run
        currentIndex = (currentIndex + 1) % announcements.size();
    }

    private TextComponent parseMessage(String message) {
        ComponentBuilder builder = new ComponentBuilder();

        // Add prefix
        builder.append(prefix + " ");

        Matcher urlMatcher = URL_PATTERN.matcher(message);
        int lastEnd = 0;

        while (urlMatcher.find()) {
            // Add text before the URL
            String beforeUrl = message.substring(lastEnd, urlMatcher.start());
            if (!beforeUrl.isEmpty()) {
                builder.append(TextComponent.fromLegacyText(parseFormatting(beforeUrl)));
            }

            String protocol = urlMatcher.group(1);
            String domain = urlMatcher.group(2);
            String path = urlMatcher.group(3) != null ? urlMatcher.group(3) : "";
            String fullUrl = protocol + domain + path;

            // Create clickable domain component
            TextComponent urlComponent = new TextComponent(domain);

            // Apply formatting from before the URL
            String formatBefore = getFormatBefore(message, urlMatcher.start());
            applyFormatting(urlComponent, formatBefore);

            // Set click and hover events
            urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, fullUrl));
            urlComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click to visit " + fullUrl).create()));

            builder.append(urlComponent);
            lastEnd = urlMatcher.end();
        }

        // Add remaining text after the last URL
        if (lastEnd < message.length()) {
            builder.append(TextComponent.fromLegacyText(parseFormatting(message.substring(lastEnd))));
        }

        return new TextComponent(builder.create());
    }

    private String getFormatBefore(String message, int position) {
        StringBuilder format = new StringBuilder();
        Matcher matcher = FORMAT_PATTERN.matcher(message.substring(0, position));
        while (matcher.find()) {
            String tag = matcher.group(1);
            if (FORMAT_TAGS.contains(tag) || isValidColor(tag)) {
                format.append("<").append(tag).append(">");
            }
        }
        return format.toString();
    }

    private boolean isValidColor(String tag) {
        try {
            ChatColor.valueOf(tag);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void applyFormatting(TextComponent component, String formatting) {
        Matcher matcher = FORMAT_PATTERN.matcher(formatting);
        while (matcher.find()) {
            String tag = matcher.group(1);
            try {
                if (FORMAT_TAGS.contains(tag)) {
                    switch (tag) {
                        case "BOLD": component.setBold(true); break;
                        case "ITALIC": component.setItalic(true); break;
                        case "UNDERLINE": component.setUnderlined(true); break;
                        case "STRIKETHROUGH": component.setStrikethrough(true); break;
                        case "MAGIC": component.setObfuscated(true); break;
                    }
                } else {
                    component.setColor(ChatColor.valueOf(tag));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid tag: <" + tag + ">");
            }
        }
    }

    private String parseFormatting(String message) {
        message = ChatColor.translateAlternateColorCodes('&', message);

        Matcher matcher = FORMAT_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group(1);
            try {
                if (FORMAT_TAGS.contains(tag)) {
                    ChatColor format = ChatColor.valueOf(tag);
                    matcher.appendReplacement(result, format.toString());
                } else {
                    ChatColor color = ChatColor.valueOf(tag);
                    matcher.appendReplacement(result, color.toString());
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid tag in announcement: <" + tag + ">");
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public void disable() {
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }
    }
}