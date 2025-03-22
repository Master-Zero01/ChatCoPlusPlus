package org.zeroBzeroT.chatCo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Utils {
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    // Map of color names to Adventure TextColor objects
    private static final Map<String, TextColor> NAMED_COLORS = new HashMap<>();
    
    // List of text formatting tags
    public static final List<String> FORMAT_TAGS = List.of(
            "BOLD", "ITALIC", "UNDERLINE", "STRIKETHROUGH", "MAGIC", "RESET"
    );
    
    // URL pattern for detecting links in text
    public static final Pattern URL_PATTERN = Pattern.compile("(https?://)([-\\w.]+)(/\\S*)?");
    
    // Pattern for detecting formatting tags like <RED> or <BOLD>
    public static final Pattern FORMAT_PATTERN = Pattern.compile("<([A-Z_]+)>");

    static {
        // Standard Minecraft colors (using NamedTextColor constants)
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

    /**
     * Gets a TextColor by its name
     *
     * @param colorName - the color name (e.g., "RED", "GOLD")
     * @return The corresponding TextColor or null if not found
     */
    public static TextColor getColorByName(String colorName) {
        return NAMED_COLORS.get(colorName.toUpperCase());
    }

    /**
     * Get all available named colors
     *
     * @return map of color names to TextColor objects
     */
    public static Map<String, TextColor> getNamedColors() {
        return new HashMap<>(NAMED_COLORS);
    }

    /**
     * Check if a tag is a valid color name
     * 
     * @param tag - the tag to check
     * @return true if the tag is a valid color name
     */
    public static boolean isValidColor(String tag) {
        return NAMED_COLORS.containsKey(tag.toUpperCase());
    }

    /**
     * Check if a tag is a valid formatting tag
     * 
     * @param tag - the tag to check
     * @return true if the tag is a valid formatting tag
     */
    public static boolean isValidFormat(String tag) {
        return FORMAT_TAGS.contains(tag.toUpperCase());
    }

    /**
     * Strip color formatting from a string
     *
     * @param text - the text to strip colors from
     * @return the text without color codes
     */
    public static String stripColor(String text) {
        if (text == null) return null;
        return Pattern.compile("(?i)§[0-9A-FK-ORX]").matcher(text).replaceAll("");
    }

    /**
     * Determine if a player is in vanish mode. Works with most Vanish-Plugins.
     *
     * @param player - the player.
     * @return TRUE if the player is invisible, FALSE otherwise.
     */
    public static boolean isVanished(Player player) {
        if (player != null && player.hasMetadata("vanished") && !player.getMetadata("vanished").isEmpty()) {
            return player.getMetadata("vanished").get(0).asBoolean();
        }

        return false;
    }

    /**
     * Converts a string into a single text component while retaining the old formatting
     */
    public static TextComponent componentFromLegacyText(String legacyText) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacyText);
    }

    /**
     * Get the direct color code for a color name
     * 
     * @param colorName - the name of the color (e.g., "RED", "GOLD")
     * @return the section symbol color code (e.g., "§c" for "RED")
     */
    public static String getDirectColorCode(String colorName) {
        if (colorName == null) return "§f"; // Default to white if null
        
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
            default -> "§f"; // Default to white if unknown
        };
    }

    /**
     * Get the direct formatting code for a format name
     * 
     * @param formatName - the name of the format (e.g., "BOLD", "ITALIC")
     * @return the section symbol format code (e.g., "§l" for "BOLD")
     */
    public static String getDirectFormatCode(String formatName) {
        if (formatName == null) return "";
        
        return switch (formatName.toUpperCase()) {
            case "BOLD" -> "§l";
            case "ITALIC" -> "§o";
            case "UNDERLINE" -> "§n";
            case "STRIKETHROUGH" -> "§m";
            case "MAGIC" -> "§k";
            case "RESET" -> "§r";
            default -> "";
        };
    }

    /**
     * Get the TextDecoration object for a format name
     * 
     * @param formatName - the name of the format (e.g., "BOLD", "ITALIC")
     * @return the corresponding TextDecoration object or null if not found
     */
    public static TextDecoration getDecorationByName(String formatName) {
        if (formatName == null) return null;
        
        return switch (formatName.toUpperCase()) {
            case "BOLD" -> TextDecoration.BOLD;
            case "ITALIC" -> TextDecoration.ITALIC;
            case "UNDERLINE" -> TextDecoration.UNDERLINED;
            case "STRIKETHROUGH" -> TextDecoration.STRIKETHROUGH;
            case "MAGIC" -> TextDecoration.OBFUSCATED;
            default -> null;
        };
    }

    /**
     * Parse formatting tags in a message and convert to legacy color codes
     * 
     * @param message - the message to parse
     * @return the message with formatting tags replaced with legacy color codes
     */
    public static String parseFormattingTags(String message) {
        // First, handle legacy color codes
        message = LegacyComponentSerializer.legacyAmpersand().serialize(
            LegacyComponentSerializer.legacyAmpersand().deserialize(message));

        java.util.regex.Matcher matcher = FORMAT_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String tag = matcher.group(1);
            if (isValidFormat(tag)) {
                // Handle formatting tags
                matcher.appendReplacement(result, getDirectFormatCode(tag));
            } else if (isValidColor(tag)) {
                // Handle color tags
                matcher.appendReplacement(result, getDirectColorCode(tag));
            } else {
                // Unknown tag - leave empty
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Apply formatting to a component based on a format tag
     * 
     * @param component - the component to format
     * @param formatTag - the format tag to apply (e.g., "BOLD", "ITALIC")
     * @return the formatted component
     */
    public static Component applyFormatting(Component component, String formatTag) {
        TextDecoration decoration = getDecorationByName(formatTag);
        if (decoration != null) {
            return component.decoration(decoration, true);
        } else if (isValidColor(formatTag)) {
            return component.color(getColorByName(formatTag));
        }
        return component;
    }

    /**
     * Saves a stream to a file
     */
    public static void saveStreamToFile(final InputStream stream, final File file) {
        if (stream == null || file == null) {
            Main.getPlugin(Main.class).getLogger().warning("Cannot save stream to file: stream or file is null");
            return;
        }

        try (OutputStream out = Files.newOutputStream(file.toPath());
            InputStream in = stream) {
            final byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            Main.getPlugin(Main.class).getLogger().warning(String.format("Error saving stream to file: %s", e.getMessage()));
        } catch (SecurityException e) {
            Main.getPlugin(Main.class).getLogger().warning(String.format("Security error saving stream to file: %s", e.getMessage()));
        }
    }

    /**
     * returns a formatted Date/Time string
     */
    public static String now() {
        final Calendar cal = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }
}
