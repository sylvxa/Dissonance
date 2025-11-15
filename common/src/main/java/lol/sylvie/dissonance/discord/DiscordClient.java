package lol.sylvie.dissonance.discord;

import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.config.DissonanceConfig;
import lol.sylvie.dissonance.discord.command.impl.LinkCommand;
import lol.sylvie.dissonance.discord.command.impl.UnlinkCommand;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

import static lol.sylvie.dissonance.util.ConsoleUtil.friendlyMessageBox;

public class DiscordClient {
    public static JDA CLIENT = null;

    private static void ensureOutputs() {
        if (!MinecraftToDiscordBridge.ENABLED) return;
        TextChannel channel = MinecraftToDiscordBridge.getOutputChannel();
        if (channel == null || !channel.canTalk()) {
            friendlyMessageBox("The output channel is invalid! Make sure the bot has access to/can type in it and that the ID is correct.");
            MinecraftToDiscordBridge.ENABLED = false;
            return;
        }

        if (!DissonanceConfig.USE_WEBHOOK_MESSAGES.get()) return;
        try {
            String webhookUri = DissonanceConfig.DISCORD_WEBHOOK.get();
            MinecraftToDiscordBridge.WEBHOOK = WebhookClient.createClient(CLIENT, webhookUri);

            CLIENT.retrieveWebhookById(MinecraftToDiscordBridge.WEBHOOK.getId()).complete();
            return;
        }
        catch (IllegalArgumentException ignored) {}
        catch (ErrorResponseException exception) {
            Constants.LOG.error("Failed to fetch webhook!", exception);
        }

        if (channel.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
            Constants.LOG.warn("Webhook is invalid, generating new one. (you should only see this once!)");
            Webhook webhook = channel.createWebhook("Dissonance").complete();
            String uri = webhook.getUrl();
            DissonanceConfig.DISCORD_WEBHOOK.set(uri);
            MinecraftToDiscordBridge.WEBHOOK = WebhookClient.createClient(CLIENT, uri);
            DissonanceConfig.SPEC.save();
        } else {
            friendlyMessageBox("Webhook messages are enabled, but the stored URL is invalid and the bot doesn't have permission to make one.", "Either make one and specify it in the config file, or give the bot the \"Manage Webhooks\" permission.");
        }
    }

    private static void ensureInputs() {
        for (Long id : DissonanceConfig.INPUT_CHANNELS.get()) {
            TextChannel channel;
            try {
                channel = CLIENT.getTextChannelById(id);
            } catch (NumberFormatException e) {
                Constants.LOG.error("Channel ID {} is invalid!", id);
                continue;
            }

            if (channel == null) {
                Constants.LOG.error("Cannot access channel {}, make sure the ID is valid and that the bot has the \"View Channel\" permission.", id);
                continue;
            } else if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL)) {
                Constants.LOG.error("The bot can't read #{}, make sure that the bot has permission to view it.", channel.getName());
            }
        }
    }

    public static @Nullable JDA createClientSafely(MinecraftServer server) {
        try {
            CLIENT = JDABuilder.createLight(DissonanceConfig.DISCORD_TOKEN.get(), EnumSet.of(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS))
                    .addEventListeners(new DiscordToMinecraftBridge(server))
                    .addEventListeners(new LinkCommand(), new UnlinkCommand())
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.VOICE_STATE)
                    .build();
            CLIENT.awaitReady();

            ensureOutputs();
            ensureInputs();
        } catch (InvalidTokenException e) {
            friendlyMessageBox(
                    "HEY, LISTEN! Your Discord token is invalid! (it's okay if this is the first time the server has been started)",
                    "Please edit and fill out \"config/dissonance-server.toml\", then restart the server!"
            );
        } catch (InterruptedException | IllegalStateException e) {
            Constants.LOG.error("The error in question: ", e);
            friendlyMessageBox(
                    "There was an error while logging into Discord.",
                    "Please ensure that you have the \"Server Members\" and \"Message Content\" intents turned on in the \"Bot\" section of the Discord developer portal."
            );
        }

        return CLIENT;
    }
}
