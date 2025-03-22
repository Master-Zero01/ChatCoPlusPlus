package org.zeroBzeroT.chatCo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Utils {
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    // Map of legacy color names to Adventure TextColor objects
    private static final Map<String, TextColor> NAMED_COLORS = new HashMap<>();

    static {
        // Standard Minecraft colors (matching legacy ChatColor names)
        NAMED_COLORS.put("BLACK", TextColor.color(0, 0, 0));
        NAMED_COLORS.put("DARK_BLUE", TextColor.color(0, 0, 170));
        NAMED_COLORS.put("DARK_GREEN", TextColor.color(0, 170, 0));
        NAMED_COLORS.put("DARK_AQUA", TextColor.color(0, 170, 170));
        NAMED_COLORS.put("DARK_RED", TextColor.color(170, 0, 0));
        NAMED_COLORS.put("DARK_PURPLE", TextColor.color(170, 0, 170));
        NAMED_COLORS.put("GOLD", TextColor.color(255, 170, 0));
        NAMED_COLORS.put("GRAY", TextColor.color(170, 170, 170));
        NAMED_COLORS.put("DARK_GRAY", TextColor.color(85, 85, 85));
        NAMED_COLORS.put("BLUE", TextColor.color(85, 85, 255));
        NAMED_COLORS.put("GREEN", TextColor.color(85, 255, 85));
        NAMED_COLORS.put("AQUA", TextColor.color(85, 255, 255));
        NAMED_COLORS.put("RED", TextColor.color(255, 85, 85));
        NAMED_COLORS.put("LIGHT_PURPLE", TextColor.color(255, 85, 255));
        NAMED_COLORS.put("YELLOW", TextColor.color(255, 255, 85));
        NAMED_COLORS.put("WHITE", TextColor.color(255, 255, 255));
    }

    /**
     * Gets a TextColor by its legacy color name
     *
     * @param colorName - the legacy color name (e.g., "RED", "GOLD")
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
     * Strip color formatting from a string (replaces ChatColor.stripColor)
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
     * Convert a legacy color code string to its modern representation (replaces ChatColor.toString())
     *
     * @param colorName - the name of the color (e.g., "RED", "GOLD")
     * @return the legacy color code string
     */
    public static String colorToString(String colorName) {
        TextColor color = getColorByName(colorName);
        if (color == null) return "";

        // Convert back to legacy format for compatibility
        Component component = Component.text("").color(color);
        return LegacyComponentSerializer.legacySection().serialize(component).substring(0, 2);
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
