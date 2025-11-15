package lol.sylvie.dissonance.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DissonanceConfig {
    public static final ModConfigSpec SPEC;

    private static final Predicate<Object> LONG_LIST_VALIDATOR = o -> o instanceof List<?> list && list.stream().allMatch(n -> n instanceof Long);
    private static final Predicate<Object> COLOR_VALIDATOR = (value) -> {
        if (!(value instanceof String string)) return false;
        try {
            Color.decode(string);
            return true;
        } catch (NumberFormatException ignored) {}
        return false;
    };

    public static final ModConfigSpec.ConfigValue<String> DISCORD_TOKEN;
    public static final ModConfigSpec.ConfigValue<String> DISCORD_WEBHOOK;

    public static final ModConfigSpec.ConfigValue<Boolean> MINECRAFT_TO_DISCORD_ENABLED;

    public static final ModConfigSpec.ConfigValue<String> MINECRAFT_MESSAGE_TEMPLATE;
    public static final ModConfigSpec.ConfigValue<Boolean> USE_ROLE_COLORS;
    public static final ModConfigSpec.ConfigValue<Boolean> ADD_USERNAME_HOVER_TO_NICKNAME;
    public static final ModConfigSpec.ConfigValue<Boolean> LINK_TO_MESSAGE;
    public static final ModConfigSpec.ConfigValue<Boolean> LINK_PARSING;
    public static final ModConfigSpec.ConfigValue<Boolean> SHOW_ATTACHMENTS;
    public static final ModConfigSpec.ConfigValue<String> LINK_COLOR;

    public static final ModConfigSpec.ConfigValue<Boolean> DISCORD_TO_MINECRAFT_ENABLED;

    public static final ModConfigSpec.ConfigValue<List<Long>> INPUT_CHANNELS;
    public static final ModConfigSpec.ConfigValue<Long> OUTPUT_CHANNEL;
    public static final ModConfigSpec.ConfigValue<Boolean> USE_WEBHOOK_MESSAGES;
    public static final ModConfigSpec.ConfigValue<String> AVATAR_API;
    public static final ModConfigSpec.ConfigValue<String> DISCORD_MESSAGE_TEMPLATE;
    public static final ModConfigSpec.ConfigValue<Boolean> USE_TEMPLATE_FOR_WEBHOOKS;

    public static final ModConfigSpec.ConfigValue<Boolean> ALLOW_MENTIONS;
    public static final ModConfigSpec.ConfigValue<Boolean> RESOLVE_USERNAME_MENTIONS;
    public static final ModConfigSpec.ConfigValue<Boolean> ALLOW_MASS_PINGS;

    public static final ModConfigSpec.ConfigValue<Boolean> LINKING_ENABLED;
    public static final ModConfigSpec.ConfigValue<Long> GUILD_ID;
    public static final ModConfigSpec.ConfigValue<String> LINK_MESSAGE_TEMPLATE;

    public static final ModConfigSpec.ConfigValue<Boolean> WHITELIST_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> WHITELIST_MESSAGE;
    public static final ModConfigSpec.ConfigValue<List<Long>> WHITELISTED_ROLES;
    public static final ModConfigSpec.ConfigValue<List<Long>> BLACKLISTED_ROLES;

    public static final ModConfigSpec.ConfigValue<Boolean> PROXIMITY_ENABLED;
    public static final ModConfigSpec.ConfigValue<Long> PROXIMITY_CATEGORY_ID;
    public static final ModConfigSpec.ConfigValue<Long> PROXIMITY_LOBBY_ID;
    public static final ModConfigSpec.ConfigValue<Integer> PROXIMITY_RADIUS;
    public static final ModConfigSpec.ConfigValue<Integer> PROXIMITY_GRACE_PERIOD;
    public static final ModConfigSpec.ConfigValue<Integer> PROXIMITY_UPDATE_FREQUENCY;

    public static final ModConfigSpec.ConfigValue<Boolean> USE_WEBHOOK_FOR_EVENTS;
    public static final ModConfigSpec.ConfigValue<Boolean> NICKNAME_AUTHORS;

    public static final EventConfigValue EVENT_GAME_JOIN;
    public static final EventConfigValue EVENT_GAME_LEAVE;
    public static final EventConfigValue EVENT_GAME_ADVANCEMENT;
    public static final EventConfigValue EVENT_GAME_DEATH;
    public static final EventConfigValue EVENT_SERVER_START;
    public static final EventConfigValue EVENT_SERVER_STOP;

    public static final EventConfigValue EVENT_GAME_MISC_MESSAGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Credentials Dissonance will use to interface with Discord.");
        builder.push("credentials");

        DISCORD_TOKEN = builder
                .comment("REQUIRED: Discord token of the bot")
                .define("token", "CHANGE_ME");

        DISCORD_WEBHOOK = builder
                .comment("Webhook URL used for sending messages (can be disabled)", "This will be created automatically if left default")
                .define("webhook", "PLACEHOLDER");

        builder.pop();

        builder.comment("Options about how messages are sent and received on Discord");
        builder.push("discord");

        DISCORD_TO_MINECRAFT_ENABLED = builder
                .comment("If messages sent in the following Discord channels are sent in Minecraft")
                .define("discord_to_mc", true);

        INPUT_CHANNELS = builder
                .comment("Where messages from Discord are sent to Minecraft (channel IDs)", "You normally don't need more than one here.")
                .define("input_channels", new ArrayList<>(List.of(0L)), LONG_LIST_VALIDATOR);

        // TODO: it would require a refactor but we could have multiple here? je ne sais pas
        OUTPUT_CHANNEL = builder
                .comment("Where messages from Minecraft are sent in Discord (channel ID)")
                .define("output_channel", 0L);

        USE_WEBHOOK_MESSAGES = builder
                .comment("If user messages should be sent via webhook (will be sent by the bot directly if not)", "This is what allows avatar profile pictures to work, but if you can't use webhooks for some reason set this to false.")
                .define("use_webhook", true);

        AVATAR_API = builder
                .comment("The API used for webhook message avatars (will do nothing if use_webhook is false)", "Placeholders: %username%, %uuid%, %random%")
                .define("avatar_api", "https://mc-heads.net/avatar/%uuid%");

        DISCORD_MESSAGE_TEMPLATE = builder
                .comment("How Minecraft messages should be formatted when being sent to Discord", "Placeholders: %username%, %nickname%, %content%")
                .define("message_template", "<%username%> %content%");

        USE_TEMPLATE_FOR_WEBHOOKS = builder
                .comment("If the above message template should be used for webhook messages.", "(only really useful if there's some weird formatting you want to do)")
                .define("use_template_for_webhooks", false);

        builder.push("mentions");

        ALLOW_MENTIONS = builder
                .comment("If mentions for user messages should be allowed. (ex. <@269856865008091138>)")
                .define("allow_mentions", true);

        RESOLVE_USERNAME_MENTIONS = builder
                .comment("If username mentions should be resolved. (ex. @sylvxa)", "This can cause lag depending on how big your server is!")
                .define("resolve_username_mentions", true);

        ALLOW_MASS_PINGS = builder
                .comment("If mass mentions should be allowed. (ex. @everyone or @here)")
                .define("allow_mass_mentions", false);

        builder.pop();

        builder.push("linking");

        LINKING_ENABLED = builder
                .comment("If Discord <-> Minecraft linking is enabled")
                .define("enabled", false);

        GUILD_ID = builder
                .comment("The guild ID of the server linking is enabled in")
                .define("guild_id", 0L);

        builder.push("whitelist");

        WHITELIST_ENABLED = builder
                .comment("If the Discord whitelist is enabled")
                .define("enabled", false);

        LINK_MESSAGE_TEMPLATE = builder
                .comment("The message shown to players when they have to link their account.", "Placeholders: %code%")
                .define("link_message", "§c§lThis server requires that you link with their Discord to join!\n\n§7Run the §i/link §r§7command with the code §l%code%§r§7 to link your account.");

        WHITELIST_MESSAGE = builder
                .comment("The message shown to players if they are linked but not allowed to join")
                .define("kick_message", "§cYou are not allowed to join this server!");

        WHITELISTED_ROLES = builder
                .comment("Discord role IDs that are allowed to join the server", "Operators are exempt implicitly, and leaving this empty will mean anyone in the Discord may join.")
                .define("allowed_roles", new ArrayList<>(), LONG_LIST_VALIDATOR);

        BLACKLISTED_ROLES = builder
                .comment("Discord role IDs that are NOT allowed to join the server", "Operators are exempt implicitly, but this takes priority over the above whitelist.")
                .define("disallowed_roles", new ArrayList<>(), LONG_LIST_VALIDATOR);

        builder.pop();

        builder.push("proximity");

        PROXIMITY_ENABLED = builder
                .comment("If pseudo-proximity chat is enabled.")
                .define("enabled", false);

        PROXIMITY_CATEGORY_ID = builder
                .comment("The ID of the category that proximity chat channels are created in", "Dissonance will wipe out any existing voice channels, so make sure to create a separate category!")
                .define("category_id", 0L);

        PROXIMITY_LOBBY_ID = builder
                .comment("The ID of the lobby that players will wait in if alone.", "It's highly recommended that you disable the \"Speak\" permission for this channel.")
                .define("lobby_id", 0L);

        PROXIMITY_RADIUS = builder
                .comment("How far proximity chat will extend in blocks.")
                .define("range", 48);

        PROXIMITY_GRACE_PERIOD = builder
                .comment("How far a player can move out of range before being kicked from the proximity channel.")
                .define("grace", 8);

        PROXIMITY_UPDATE_FREQUENCY = builder
                .comment("How often proximity chat will update.", "Value in ticks (checks every N ticks)")
                .define("frequency", 20);

        builder.pop();

        builder.pop();

        builder.pop();

        builder.comment("Options about how messages are sent and received in Minecraft");
        builder.push("minecraft");

        MINECRAFT_TO_DISCORD_ENABLED = builder
                .comment("If messages sent in Minecraft chat are sent to Discord")
                .define("mc_to_discord", true);

        MINECRAFT_MESSAGE_TEMPLATE = builder
                .comment("How Discord messages should be formatted when being sent in Minecraft", "Placeholders: username = %1$s, nickname = %2$s, content = %3$s, channel = %4$s")
                .define("message_template", "<%2$s> %3$s");

        USE_ROLE_COLORS = builder
                .comment("If the top role color on Discord should be passed through to Minecraft")
                .define("use_role_colors", true);

        ADD_USERNAME_HOVER_TO_NICKNAME = builder
                .comment("If the nickname field should have a hover text with the user's Discord username")
                .define("nickname_username_hover", true);

        LINK_TO_MESSAGE = builder
                .comment("If the messages in Minecraft should link to their Discord equivalent")
                .define("link_to_message", true);

        LINK_PARSING = builder
                .comment("If links should be parsed and made clickable")
                .define("link_parsing", true);

        SHOW_ATTACHMENTS = builder
                .comment("If attachments should be appended to the end of messages")
                .define("show_attachments", true);

        LINK_COLOR = builder
                .comment("The color of links in chat (in case blue isn't your style)")
                .comment("Set this to \"role\" to have it mimic role colors.")
                .define("link_color", "#5555FF", (o) -> COLOR_VALIDATOR.test(o) || (o instanceof String s && s.equalsIgnoreCase("role")));

        builder.push("events");

        USE_WEBHOOK_FOR_EVENTS = builder
                .comment("If the main webhook should also be used for events.")
                .define("use_webhook_for_events", false);

        NICKNAME_AUTHORS = builder
                .comment("If the player's nickname should be used instead of their username for embed authors.")
                .define("nickname_authors", true);

        String green = "#55FF55";
        String red = "#FF5555";
        String white = "#FFFFFF";
        EVENT_GAME_JOIN = new EventConfigValue(builder,
                "game_join",
                "This event is fired when a player joins the game",
                "%username%, %nickname%",
                null,
                "%nickname% joined the game!",
                "",
                true,
                green);

        EVENT_GAME_LEAVE = new EventConfigValue(builder,
                "game_leave",
                "This event is fired when a player leaves the game",
                "%username%, %nickname%",
                null,
                "%nickname% left the game.",
                "",
                true,
                red);

        EVENT_GAME_ADVANCEMENT = new EventConfigValue(builder,
                "game_advancement",
                "This event is fired when a player receives an advancement (will only fire if the advancement shows in chat)",
                "%username%, %nickname%, %default_announcement%, %default_title%, %advancement_name%, %advancement_description%, %advancement_type% (advancement, challenge, goal)",
                "If this is set to full white, the color will instead corresponding to advancement type (task & goal = green, challenge = dark purple)",
                "%default_title%",
                "**%advancement_name%**: %advancement_description%",
                true,
                white);

        EVENT_GAME_DEATH = new EventConfigValue(builder,
                "game_death",
                "This event is fired when a player dies",
                "%username%, %nickname%, %default_announcement%",
                null,
                "%default_announcement%",
                "",
                true,
                red);

        EVENT_SERVER_START = new EventConfigValue(builder,
                "server_start",
                "This event is fired when the server finishes starting",
                null,
                null,
                "Server started!",
                "",
                true,
                green);

        EVENT_SERVER_STOP = new EventConfigValue(builder,
                "server_stop",
                "This event is fired when the server begins to shutdown",
                null,
                null,
                "Server stopped!",
                "",
                true,
                red);

        EVENT_GAME_MISC_MESSAGE = new EventConfigValue(builder,
                "game_misc_message",
                "This event is fired when ANY game message is sent (this should be left off unless you know what you are doing.",
                "%message%",
                null,
                "%message%",
                "",
                false,
                white);

        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }

    public static class EventConfigValue {
        public final ModConfigSpec.ConfigValue<Boolean> enabled;
        public final ModConfigSpec.ConfigValue<Boolean> useEmbed;
        public final ModConfigSpec.ConfigValue<Boolean> usePlayerAuthor;
        public final ModConfigSpec.ConfigValue<String> titleTemplate;
        public final ModConfigSpec.ConfigValue<String> descriptionTemplate;
        public final ModConfigSpec.ConfigValue<String> color;

        public EventConfigValue(ModConfigSpec.Builder builder, String id, String sectionComment, @Nullable String placeholderComment, @Nullable String colorComment, String defaultTitle, String defaultDescription, boolean defaultEnabled, String defaultColor) {
            builder.comment(sectionComment);
            if (placeholderComment != null) builder.comment("Placeholders: " + placeholderComment);
            builder.push(id);

            enabled = builder
                    .comment("If this event should be announced.")
                    .define("enabled", defaultEnabled);

            useEmbed = builder
                    .comment("If an embed should be used for this event.")
                    .define("use_embed", true);

            usePlayerAuthor = builder
                    .comment("If players' username and avatar should be the author of embeds.")
                    .define("use_player_author", true);

            titleTemplate = builder
                    .comment("The title of this event's embed (this will be used as the main template if embeds are disabled)",  "Leave empty to not use a title.")
                    .define("title", defaultTitle);

            descriptionTemplate = builder
                    .comment("The description of this event's embed",  "Leave empty to not use a description.")
                    .define("description", defaultDescription);

            ModConfigSpec.Builder colorBuilder = builder
                    .comment("The color of this event's embed");
            if (colorComment != null) colorBuilder.comment(colorComment);

            color = colorBuilder.define("color", defaultColor, COLOR_VALIDATOR);

            builder.pop();
        }
    }
}
