package lol.sylvie.dissonance.util;

import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class TemplateUtil {
    public static String replace(String template, Map<String, String> replacements) {
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            template = template.replace(replacement.getKey(), replacement.getValue());
        }
        return template;
    }

    public static String replaceWithPlayer(String template, Player player) {
        return template.replace("%username%", player.getGameProfile().getName())
                .replace("%nickname%", player.getDisplayName().getString());
    }

    public static String replaceWithPlayer(String template, Player player, Map<String, String> replacements) {
        return replace(replaceWithPlayer(template, player), replacements);
    }
}
