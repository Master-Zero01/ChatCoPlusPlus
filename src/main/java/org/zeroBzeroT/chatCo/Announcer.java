package org.zeroBzeroT.chatCo;

import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import static org.zeroBzeroT.chatCo.Utils.FORMAT_PATTERN;
import static org.zeroBzeroT.chatCo.Utils.URL_PATTERN;
import static org.zeroBzeroT.chatCo.Utils.componentFromLegacyText;
import static org.zeroBzeroT.chatCo.Utils.isValidColor;
import static org.zeroBzeroT.chatCo.Utils.isValidFormat;
import static org.zeroBzeroT.chatCo.Utils.parseFormattingTags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Announcer {
    private final JavaPlugin plugin;
    private List<String> announcements;
    private String prefix;
    private int delay;
    private int currentIndex;
    private BukkitRunnable announcementTask;

    public Announcer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentIndex = 0;  // Initialize index
        // Use a private initialization method instead of calling an overridable method
        initialize();
    }

    // Private initialization method to avoid overridable method call in constructor
    private void initialize() {
        // Cancel existing task if it exists
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }

        FileConfiguration config = plugin.getConfig();
        announcements = config.getStringList("ChatCo.announcements.messages");
        prefix = config.getString("ChatCo.announcements.prefix", "&6[&eAnarchadia&6]");
        delay = config.getInt("ChatCo.announcements.delay", 300);
        currentIndex = 0;  // Reset index on reload

        // Only start if enabled
        if (config.getBoolean("ChatCo.announcements.enabled", true)) {
            startAnnouncementTask();
        }
    }

    public void loadConfig() {
        initialize();
    }

    private void startAnnouncementTask() {
        if (announcements == null || announcements.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "No announcements configured!");
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
        Component component = parseMessage(message);
        Bukkit.getServer().sendMessage(component);

        // Update index for next run
        currentIndex = (currentIndex + 1) % announcements.size();
    }

    private Component parseMessage(String message) {
        TextComponent.Builder builder = Component.text();

        // Add prefix
        builder.append(componentFromLegacyText(prefix + " "));

        Matcher urlMatcher = URL_PATTERN.matcher(message);
        int lastEnd = 0;

        while (urlMatcher.find()) {
            // Add text before the URL
            String beforeUrl = message.substring(lastEnd, urlMatcher.start());
            if (!beforeUrl.isEmpty()) {
                builder.append(LegacyComponentSerializer.legacyAmpersand().deserialize(parseFormattingTags(beforeUrl)));
            }

            String protocol = urlMatcher.group(1);
            String domain = urlMatcher.group(2);
            String path = urlMatcher.group(3) != null ? urlMatcher.group(3) : "";
            String fullUrl = protocol + domain + path;

            // Create clickable domain component with formatting
            Component urlComponent = Component.text(domain);

            // Apply formatting from before the URL
            String formatBefore = getFormatBefore(message, urlMatcher.start());
            urlComponent = applyFormattingFromString(urlComponent, formatBefore);

            // Set click and hover events
            urlComponent = urlComponent.clickEvent(ClickEvent.openUrl(fullUrl))
            .hoverEvent(HoverEvent.showText(Component.text("Click to visit " + fullUrl)));

            builder.append(urlComponent);
            lastEnd = urlMatcher.end();
        }

        // Add remaining text after the last URL
        if (lastEnd < message.length()) {
            builder.append(LegacyComponentSerializer.legacyAmpersand().deserialize(parseFormattingTags(message.substring(lastEnd))));
        }

        return builder.build();
    }

    private String getFormatBefore(String message, int position) {
        StringBuilder format = new StringBuilder();
        Matcher matcher = FORMAT_PATTERN.matcher(message.substring(0, position));
        while (matcher.find()) {
            String tag = matcher.group(1);
            if (isValidFormat(tag) || isValidColor(tag)) {
                format.append("<").append(tag).append(">");
            }
        }
        return format.toString();
    }

    private Component applyFormattingFromString(Component component, String formatting) {
        Matcher matcher = FORMAT_PATTERN.matcher(formatting);

        // Start with the original component
        Component formattedComponent = component;

        while (matcher.find()) {
            String tag = matcher.group(1);
            formattedComponent = Utils.applyFormatting(formattedComponent, tag);
        }

        return formattedComponent;
    }

    public void disable() {
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }
    }
}