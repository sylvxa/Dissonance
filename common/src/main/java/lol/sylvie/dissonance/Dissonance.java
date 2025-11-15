package lol.sylvie.dissonance;

import lol.sylvie.dissonance.config.DissonanceConfig;
import lol.sylvie.dissonance.discord.DiscordToMinecraftBridge;
import lol.sylvie.dissonance.discord.DiscordClient;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import lol.sylvie.dissonance.platform.Services;
import lol.sylvie.dissonance.util.ConsoleUtil;
import net.minecraft.server.MinecraftServer;

import java.sql.SQLException;

// This class handles event listeners and stuff
public class Dissonance {
    public static boolean SHUTTING_DOWN = false;

    public static void modInit() {
        Constants.LOG.info("Dissonance running on {}", Services.PLATFORM.getPlatformName());

        if (Services.PLATFORM.isDevelopmentEnvironment()) {
            Constants.LOG.info(":O We are in a development environment! Hi, developer <3");
        }
    }

    // This ensures configs and chat and the server exist before we start doing stuff
    // Otherwise, we might run into weird race conditions where someone chats as the server is starting. That wouldn't be good!
    public static void serverStarted(MinecraftServer server, Runnable ifSuccessful) {
        MinecraftToDiscordBridge.ENABLED = DissonanceConfig.MINECRAFT_TO_DISCORD_ENABLED.get();
        DiscordToMinecraftBridge.ENABLED = DissonanceConfig.DISCORD_TO_MINECRAFT_ENABLED.get();

        Constants.LOG.info("Logging into Discord...");
        boolean whitelistEnabled = DiscordLinking.isWhitelistEnabled();
        if (DiscordClient.createClientSafely(server) == null) {
            MinecraftToDiscordBridge.ENABLED = false;
            DiscordToMinecraftBridge.ENABLED = false;

            if (whitelistEnabled) {
                Constants.LOG.error("You have the Discord linking whitelist enabled! These errors *must* be resolved before players can properly join the server!");
            }

            Constants.LOG.warn("Dissonance is shutting down!");
            return;
        }

        try {
            if (DiscordLinking.init(server))
                Constants.LOG.info("Connected to linking database!");
            else if (whitelistEnabled) ConsoleUtil.friendlyMessageBox("HEY! The linking guild was unable to be fetched. (is your guild ID set correctly?)", "In order to keep un-whitelisted players out, non-operators will be able to join until this is fixed.");
        } catch (SQLException exception) {
            Constants.LOG.error("Couldn't load linking database!", exception);
        }

        ifSuccessful.run();
        MinecraftToDiscordBridge.onServerStarted();
    }

    private static void stop() {
        DiscordLinking.close();

        SHUTTING_DOWN = true;
        DiscordClient.CLIENT.shutdownNow();
    }

    public static void serverStopping() {
        if (MinecraftToDiscordBridge.ENABLED && DissonanceConfig.EVENT_SERVER_STOP.enabled.get()) {
            MinecraftToDiscordBridge.onServerStopped(Dissonance::stop);
        } else {
            stop();
        }

    }
}