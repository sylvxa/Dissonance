package lol.sylvie.dissonance.minecraft;

import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.config.DissonanceConfig;
import lol.sylvie.dissonance.discord.DiscordClient;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import lol.sylvie.dissonance.discord.proximity.DiscordProximity;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class MinecraftEvents {
    public static void onPlayerJoin(Player player) {
        MinecraftToDiscordBridge.onPlayerJoin(player);
    }

    public static void onPlayerLeave(Player player) {
        MinecraftToDiscordBridge.onPlayerLeave(player);

        DiscordLinking.MC_TO_DISCORD_CACHE.remove(player.getUUID());
        DiscordProximity.onDisconnect(player);
    }

    public static void onAdvancementAwarded(ServerPlayer player, AdvancementHolder advancement, DisplayInfo displayInfo) {
        MinecraftToDiscordBridge.onAdvancementAwarded(player, advancement, displayInfo);

    }

    public static void onPlayerDeath(Player player, Component message) {
        MinecraftToDiscordBridge.onPlayerDeath(player, message);
    }

    public static void onMiscMessage(Component message) {
        MinecraftToDiscordBridge.onMiscMessage(message);
    }

    // this is the most error-prone area of the mod
    private static int tickAttemptsProximity = 0;
    private static boolean isOnInterval(int ticks, int frequency) {
        return ticks % frequency == 0;
    }

    public static void tick(MinecraftServer server) {
        try {
            int ticksPassed = server.getTickCount();
            if (isOnInterval(ticksPassed, DissonanceConfig.PROXIMITY_UPDATE_FREQUENCY.get())) {
                DiscordProximity.update(server);
                tickAttemptsProximity = 0;
            }

            if (isOnInterval(ticksPassed, DissonanceConfig.ACTIVITY_UPDATE_FREQUENCY.get() * 20)) {
                DiscordClient.updateActivity(server);
            }
        } catch (RuntimeException e) {
            Constants.LOG.error("Couldn't update Discord proximity!", e);
            tickAttemptsProximity++;

            if (tickAttemptsProximity > 10) {
                Constants.LOG.error("Discord proximity has been disabled as there were 10 failed ticks in a row.");
                DiscordProximity.ENABLED = false;
            }
        }
    }
}
