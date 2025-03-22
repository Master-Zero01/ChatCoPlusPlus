package org.zeroBzeroT.chatCo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import static org.zeroBzeroT.chatCo.Utils.componentFromLegacyText;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Announcer {
    private final JavaPlugin plugin;
    private List<String> announcements;
    private String prefix;
    private int delay;
    private int currentIndex;
    private BukkitRunnable announcementTask;
    
    // Map to convert color names to TextColor objects
    private static final Map<String, TextColor> NAMED_COLORS = new HashMap<>();
    
    static {
        NAMED_COLORS.put("BLACK", NamedTextColor.BLACK);
        NAMED_COLORS.put("DARK_BLUE", NamedTextColor.DARK_BLUE);
        NAMED_COLORS.put("DARK_GREEN", NamedTextColor.DARK_GREEN);
        NAMED_COLORS.put("DARK_AQUA", NamedTextColor.DARK_AQUA);
        NAMED_COLORS.put("DARK_RED", NamedTextColor.DARK_RED);
        NAMED_COLORS.put("DARK_PURPLE", NamedTextColor.DARK_PURPLE);
        NAMED_COLORS.put("GOLD", NamedTextColor.GOLD);
        NAMED_COLORS.put("GRAY", NamedTextColor.GRAY);
        NAMED_COLORS.put("DARK_GRAY", NamedTextColor.DARK_GRAY);
        NAMED_COLORS.put("BLUE", NamedTextColor.BLUE);
        NAMED_COLORS.put("GREEN", NamedTextColor.GREEN);
        NAMED_COLORS.put("AQUA", NamedTextColor.AQUA);
        NAMED_COLORS.put("RED", NamedTextColor.RED);
        NAMED_COLORS.put("LIGHT_PURPLE", NamedTextColor.LIGHT_PURPLE);
        NAMED_COLORS.put("YELLOW", NamedTextColor.YELLOW);
        NAMED_COLORS.put("WHITE", NamedTextColor.WHITE);
    }

    private static final Pattern FORMAT_PATTERN = Pattern.compile("<([A-Z_]+)>");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://)([-\\w.]+)(/\\S*)?");

    private static final List<String> FORMAT_TAGS = List.of(
            "BOLD", "ITALIC", "UNDERLINE", "STRIKETHROUGH", "MAGIC"
    );

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
        prefix = "&6[&eAnarchadia&6]"; // Using & format for legacy color codes
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
                builder.append(LegacyComponentSerializer.legacyAmpersand().deserialize(parseFormatting(beforeUrl)));
            }

            String protocol = urlMatcher.group(1);
            String domain = urlMatcher.group(2);
            String path = urlMatcher.group(3) != null ? urlMatcher.group(3) : "";
            String fullUrl = protocol + domain + path;

            // Create clickable domain component with formatting
            Component urlComponent = Component.text(domain);
            
            // Apply formatting from before the URL
            String formatBefore = getFormatBefore(message, urlMatcher.start());
            urlComponent = applyFormatting(urlComponent, formatBefore);

            // Set click and hover events
            urlComponent = urlComponent.clickEvent(ClickEvent.openUrl(fullUrl))
                           .hoverEvent(HoverEvent.showText(Component.text("Click to visit " + fullUrl)));

            builder.append(urlComponent);
            lastEnd = urlMatcher.end();
        }

        // Add remaining text after the last URL
        if (lastEnd < message.length()) {
            builder.append(LegacyComponentSerializer.legacyAmpersand().deserialize(parseFormatting(message.substring(lastEnd))));
        }

        return builder.build();
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
        return NAMED_COLORS.containsKey(tag);
    }

    private Component applyFormatting(Component component, String formatting) {
        Matcher matcher = FORMAT_PATTERN.matcher(formatting);
        
        // Start with the original component
        Component formattedComponent = component;
        
        while (matcher.find()) {
            String tag = matcher.group(1);
            
            if (FORMAT_TAGS.contains(tag)) {
                // Convert to rule-based switch expression
                formattedComponent = switch (tag) {
                    case "BOLD" -> formattedComponent.decoration(TextDecoration.BOLD, true);
                    case "ITALIC" -> formattedComponent.decoration(TextDecoration.ITALIC, true);
                    case "UNDERLINE" -> formattedComponent.decoration(TextDecoration.UNDERLINED, true);
                    case "STRIKETHROUGH" -> formattedComponent.decoration(TextDecoration.STRIKETHROUGH, true);
                    case "MAGIC" -> formattedComponent.decoration(TextDecoration.OBFUSCATED, true);
                    default -> formattedComponent;
                };
            } else if (isValidColor(tag)) {
                formattedComponent = formattedComponent.color(NAMED_COLORS.get(tag));
            } else {
                // Use proper formatted logging
                plugin.getLogger().log(Level.WARNING, "Invalid tag: {0}", tag);
            }
        }
        
        return formattedComponent;
    }

    private String parseFormatting(String message) {
        // First, handle legacy color codes
        message = LegacyComponentSerializer.legacyAmpersand().serialize(
                 LegacyComponentSerializer.legacyAmpersand().deserialize(message));

        Matcher matcher = FORMAT_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group(1);
            try {
                if (FORMAT_TAGS.contains(tag)) {
                    // Apply formatting
                    String replacementCode = getFormattingCode(tag);
                    matcher.appendReplacement(result, replacementCode);
                } else if (isValidColor(tag)) {
                    // Apply color
                    Component colorComponent = Component.text("").color(NAMED_COLORS.get(tag));
                    String legacyColorCode = LegacyComponentSerializer.legacySection().serialize(colorComponent).substring(0, 2);
                    matcher.appendReplacement(result, legacyColorCode);
                } else {
                    // Use proper formatted logging
                    plugin.getLogger().log(Level.WARNING, "Invalid tag in announcement: {0}", tag);
                    matcher.appendReplacement(result, "");
                }
            } catch (Exception e) {
                // Use proper formatted logging
                plugin.getLogger().log(Level.WARNING, "Error processing tag in announcement: {0} - {1}", 
                        new Object[]{tag, e.getMessage()});
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }
    
    private String getFormattingCode(String format) {
        // Handle all format cases with proper null safety
        TextDecoration decoration = switch (format) {
            case "BOLD" -> TextDecoration.BOLD;
            case "ITALIC" -> TextDecoration.ITALIC;
            case "UNDERLINE" -> TextDecoration.UNDERLINED;
            case "STRIKETHROUGH" -> TextDecoration.STRIKETHROUGH;
            case "MAGIC" -> TextDecoration.OBFUSCATED;
            default -> {
                plugin.getLogger().log(Level.WARNING, "Unknown format tag: {0}", format);
                yield TextDecoration.ITALIC; // Default to a safe decoration as fallback
            }
        };
        
        Component component = Component.text().decoration(decoration, true).build();
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public void disable() {
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }
    }
}