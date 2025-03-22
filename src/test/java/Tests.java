import java.util.Map;

import org.bukkit.entity.Player;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.zeroBzeroT.chatCo.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Tests {
    @Test
    public void testPlayer() {
        Player player = PowerMockito.mock(Player.class);
        // STUB FOR TESTS
    }

    @Test
    public void testSplit() {
        String legacyMessage = "%LIGHT_PURPLE%To %RECEIVER%: ";

        Map<String, TextColor> colorMap = Utils.getNamedColors();
        for (Map.Entry<String, TextColor> entry : colorMap.entrySet()) {
            Component colorComponent = Component.text("").color(entry.getValue());
            String legacyColorCode = LegacyComponentSerializer.legacySection().serialize(colorComponent).substring(0, 2);
            legacyMessage = legacyMessage.replace("%" + entry.getKey() + "%", legacyColorCode);
        }

        String[] parts;
        TextComponent messagePlayer;
        String targetName = "Olaf";

        legacyMessage = legacyMessage.replace("%SENDER%", "Bianca");
        parts = legacyMessage.split("%RECEIVER%", 2);
    }
}
