package lol.sylvie.dissonance.minecraft;

import com.mojang.authlib.GameProfile;
import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.Dissonance;
import lol.sylvie.dissonance.config.DissonanceConfig;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import lol.sylvie.dissonance.util.TemplateUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lol.sylvie.dissonance.discord.DiscordClient.CLIENT;

public class MinecraftToDiscordBridge {
    public static boolean ENABLED = true;
    public static IncomingWebhookClient WEBHOOK = null;

    public static final Pattern MENTION_PATTERN = Pattern.compile("@(?=.{2,32}$)(?!(?:everyone|here)$)\\.?[a-z0-9_]+(?:\\.[a-z0-9_]+)*\\.?");

    public static TextChannel getOutputChannel() {
        if (CLIENT == null) throw new IllegalStateException("Nothing Discord related should be called while JDA is uninitialized!");
        try {
            return CLIENT.getTextChannelById(DissonanceConfig.OUTPUT_CHANNEL.get());
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private static boolean shouldUseWebhook() {
        return DissonanceConfig.USE_WEBHOOK_MESSAGES.get() && WEBHOOK != null;
    }

    private static void logQueue(RestAction<?> action, Runnable afterwards) {
        if (Dissonance.SHUTTING_DOWN) return;
        action.queue(
                success -> afterwards.run(),
                failure -> {
                    Constants.LOG.error("Couldn't send message!", failure);
                    afterwards.run();
                }
        );
    }

    private static void logQueue(RestAction<?> action) {
        logQueue(action, () -> {});
    }

    public static String getAvatarUrl(GameProfile profile) {
        return TemplateUtil.replace(DissonanceConfig.AVATAR_API.get(),
                Map.of("%username%", profile.getName(),
                        "%uuid%", profile.getId().toString(),
                        "%random%", UUID.randomUUID().toString()));
    }

    public static @Nullable Member getOrFetchMember(Guild guild, String name) {
        Member member = guild.getMemberByTag(name, "0000");
        if (member != null) return member;

        List<Member> members = guild.findMembers(m -> m.getUser().getName().equalsIgnoreCase(name)).get();
        if (!members.isEmpty()) return members.getFirst();
        return null;
    }

    public static String parsePlayerMessage(@Nullable TextChannel channel, String rawText) {
        if (channel != null && DissonanceConfig.ALLOW_MENTIONS.get() && DissonanceConfig.RESOLVE_USERNAME_MENTIONS.get()) {
            Matcher matcher = MENTION_PATTERN.matcher(rawText);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                String username = matcher.group(0).replaceFirst("@", "");
                // FIXME: For some reason calling channel.getMembers() directly doesn't quite work
                Member member = getOrFetchMember(getOutputChannel().getGuild(), username);
                if (member != null) {
                    matcher.appendReplacement(result,  member.getAsMention());
                } else {
                    matcher.appendReplacement(result, "@" + username);
                }

            }
            matcher.appendTail(result);
            rawText = result.toString();
        }

        return rawText;
    }

    public static String formatPlayerMessage(Player player, String message, boolean webhook) {
        return !webhook || DissonanceConfig.USE_TEMPLATE_FOR_WEBHOOKS.get() ?
                TemplateUtil.replaceWithPlayer(DissonanceConfig.DISCORD_MESSAGE_TEMPLATE.get(), player, Map.of("%content%", message))
                : message;
    }

    public static void onPlayerChatMessage(Player player, String rawText) {
        if (!MinecraftToDiscordBridge.ENABLED) return;
        GameProfile profile = player.getGameProfile();
        TextChannel channel = getOutputChannel();
        String parsedMessage = parsePlayerMessage(channel, rawText);

        HashSet<Message.MentionType> allowedMentions = new HashSet<>();
        allowedMentions.add(Message.MentionType.EMOJI);
        if (DissonanceConfig.ALLOW_MENTIONS.get()) {
            allowedMentions.add(Message.MentionType.USER);
            if (DissonanceConfig.ALLOW_MASS_PINGS.get()) {
                allowedMentions.add(Message.MentionType.EVERYONE);
                allowedMentions.add(Message.MentionType.HERE);
                allowedMentions.add(Message.MentionType.ROLE);
            }
        }

        boolean webhook = shouldUseWebhook();
        String formatted = formatPlayerMessage(player, parsedMessage, webhook);
        if (webhook) {
            logQueue(WEBHOOK.sendMessage(formatted)
                .setAvatarUrl(getAvatarUrl(profile))
                .setUsername(profile.getName())
                .setAllowedMentions(allowedMentions));
        } else {
            logQueue(channel.sendMessage(formatted)
                    .setAllowedMentions(allowedMentions));
        }
    }

    private static String replaceIfPlayer(@Nullable Player player, String template, Map<String, String> placeholders) {
        return player != null ? TemplateUtil.replaceWithPlayer(template, player, placeholders) : TemplateUtil.replace(template, placeholders);
    }

    public static void handleEvent(@Nullable Player player, DissonanceConfig.EventConfigValue eventValue, Map<String, String> placeholders, @Nullable Color colorOverride, Runnable afterwards) {
        if (!ENABLED || !eventValue.enabled.get()) return;
        boolean usingEmbed = eventValue.useEmbed.get();

        String title = replaceIfPlayer(player, eventValue.titleTemplate.get(), placeholders);
        boolean usingTitle = !title.isEmpty();
        String description = usingEmbed ? replaceIfPlayer(player, eventValue.descriptionTemplate.get(), placeholders) : "";
        boolean usingDescription = !description.isEmpty();
        if (!usingTitle && !usingDescription) return;

        String content = title;
        MessageEmbed embed = null;
        if (usingEmbed) {
            EmbedBuilder builder = new EmbedBuilder();
            if (usingTitle) builder.setTitle(title);
            if (usingDescription) builder.setDescription(description);
            builder.setColor(colorOverride == null ? Color.decode(eventValue.color.get()) : colorOverride);

            if (eventValue.usePlayerAuthor.get() && player != null) {
                GameProfile profile = player.getGameProfile();
                builder.setAuthor(
                        DissonanceConfig.NICKNAME_AUTHORS.get() ? player.getDisplayName().getString() : player.getGameProfile().getName(),
                        null,
                        getAvatarUrl(profile)
                );
            }

            content = null;
            embed = builder.build();
        }

        // I don't like this but WEBHOOK and output don't share an interface
        RestAction<?> action;
        if (DissonanceConfig.USE_WEBHOOK_FOR_EVENTS.get() && shouldUseWebhook()) {
            if (usingEmbed)
                action = WEBHOOK.sendMessageEmbeds(embed);
            else action = WEBHOOK.sendMessage(content);
        } else {
            TextChannel output = getOutputChannel();
            assert output != null;
            if (usingEmbed)
                action = output.sendMessageEmbeds(embed);
            else action = output.sendMessage(content);
        }

        logQueue(action, afterwards);
    }

    public static void handleEvent(@Nullable Player player, DissonanceConfig.EventConfigValue eventValue, Map<String, String> placeholders, @Nullable Color colorOverride) {
        handleEvent(player, eventValue, placeholders, colorOverride, () -> {});
    }

    // Event hooks
    private static final Map<String, String> EMPTY = Map.of();
    public static void onPlayerJoin(Player player) {
        handleEvent(player, DissonanceConfig.EVENT_GAME_JOIN, EMPTY, null);
    }

    public static void onPlayerLeave(Player player) {
        handleEvent(player, DissonanceConfig.EVENT_GAME_LEAVE, EMPTY, null);

        DiscordLinking.MC_TO_DISCORD_CACHE.remove(player.getUUID());
    }

    public static void onAdvancementAwarded(ServerPlayer player, AdvancementHolder advancement, DisplayInfo displayInfo) {
        AdvancementType type = displayInfo.getType();
        Map<String, String> placeholders = Map.of(
                "%default_announcement%", type.createAnnouncement(advancement, player).getString(),
                "%default_title%", type.getDisplayName().getString(),
                "%advancement_name%", Advancement.name(advancement).getString(),
                "%advancement_description%", displayInfo.getDescription().getString(),
                "%advancement_type%", type.getSerializedName()
        );

        DissonanceConfig.EventConfigValue configValue = DissonanceConfig.EVENT_GAME_ADVANCEMENT;
        handleEvent(player, configValue, placeholders, configValue.color.get().equalsIgnoreCase("#FFFFFF") ? new Color(type.getChatColor().getColor()) : null);

    }

    public static void onPlayerDeath(Player player, Component message) {
        Map<String, String> placeholders = Map.of("%default_announcement%", message.getString());
        handleEvent(player, DissonanceConfig.EVENT_GAME_DEATH, placeholders, null);
    }

    public static void onServerStarted() {
        handleEvent(null, DissonanceConfig.EVENT_SERVER_START, EMPTY, null);
    }

    public static void onServerStopped(Runnable onShutDown) {
        handleEvent(null, DissonanceConfig.EVENT_SERVER_STOP, EMPTY, null, onShutDown);
    }

    public static void onMiscMessage(Component message) {
        Map<String, String> placeholders = Map.of("%message%", message.getString());
        handleEvent(null, DissonanceConfig.EVENT_GAME_MISC_MESSAGE, placeholders, null);
    }
}
