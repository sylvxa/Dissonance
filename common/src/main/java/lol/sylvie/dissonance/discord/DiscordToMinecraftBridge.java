package lol.sylvie.dissonance.discord;

import lol.sylvie.dissonance.config.DissonanceConfig;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordToMinecraftBridge extends MinecraftOwnedListener {
    public static boolean ENABLED = true;
    public static final Pattern URL_PATTERN = Pattern.compile("https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*");

    public DiscordToMinecraftBridge(MinecraftServer server) {
        super(server);
    }

    public static MutableComponent optionallyColorMember(int color, MutableComponent component) {
        return DissonanceConfig.USE_ROLE_COLORS.get() ? component.withColor(color) : component;
    }


    private static @NotNull MutableComponent getAttachmentHover(Message.Attachment attachment) {
        MutableComponent hoverComponent = Component.empty();
        if (attachment.getDescription() != null) hoverComponent.append(attachment.getDescription() + "\n");

        boolean isImage = attachment.isImage();
        if (isImage || attachment.isVideo()) {
            hoverComponent.append(String.format("%s dimensions: %sx%s", isImage ? "Image" : "Video", attachment.getWidth(), attachment.getHeight()));
        } else {
            hoverComponent.append("Content-Type: " + attachment.getContentType());
        }
        return hoverComponent;
    }

    public static MutableComponent formatDiscordContent(int roleColor, Message message) {
        String content = message.getContentDisplay();
        MutableComponent baseComponent = Component.empty();

        String colorSetting = DissonanceConfig.LINK_COLOR.get();
        int color = 0xFFFFFF;
        if (colorSetting.equalsIgnoreCase("role")) {
            color = roleColor;
        } else {
            try {
                color = Integer.decode(colorSetting);
            } catch (NumberFormatException ignored) {}
        }

        // Parse links
        if (DissonanceConfig.LINK_PARSING.get()) {
            Matcher matcher = URL_PATTERN.matcher(content);
            int lastEnd = 0;
            while (matcher.find()) {
                baseComponent.append(content.substring(lastEnd, matcher.start()));

                String url = matcher.group(0);
                baseComponent.append(Component.literal(url)
                        .withStyle(Style.EMPTY
                                .withUnderlined(true)
                                .withColor(color)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        ));

                lastEnd = matcher.end();
            }
            baseComponent.append(Component.literal(content.substring(lastEnd)));
        } else {
            baseComponent.append(content);
        }

        // Append attachments
        if (DissonanceConfig.SHOW_ATTACHMENTS.get()) {
            for (Message.Attachment attachment : message.getAttachments()) {
                MutableComponent hoverComponent = getAttachmentHover(attachment);

                String name = attachment.isSpoiler() ? "SPOILER" : attachment.getFileName();
                Component attachmentComponent = Component.literal(" [" + name + "]")
                        .withStyle(Style.EMPTY
                                .withColor(color)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                        );

                baseComponent.append(attachmentComponent);
            }
        }

        return baseComponent;
    }

    // I'm not sure how much of a performance impact string replacement has,
    // but it might be worth adding aliases like %username% or %content%
    public static Component formatDiscordMessage(Member member, Channel channel, Message message) {
        String template = DissonanceConfig.MINECRAFT_MESSAGE_TEMPLATE.get();
        int color = member.getColorRaw();

        String usernameString = member.getUser().getName();
        MutableComponent username = optionallyColorMember(color, Component.literal(usernameString));
        MutableComponent nickname = optionallyColorMember(color, Component.literal(member.getEffectiveName()));
        if (DissonanceConfig.ADD_USERNAME_HOVER_TO_NICKNAME.get())
            nickname = nickname.withStyle(nickname.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("@" + usernameString + " on Discord"))));
        MutableComponent content = formatDiscordContent(color, message);
        MutableComponent channelName = Component.literal(channel.getName());

        MutableComponent fullMessage = Component.translatable(template,
                username,
                nickname,
                content,
                channelName);

        if (DissonanceConfig.LINK_TO_MESSAGE.get()) {
            try {
                fullMessage = fullMessage.withStyle(fullMessage.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message.getJumpUrl())));
            } catch (IllegalArgumentException ignored) {}
        }

        return fullMessage;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!DiscordToMinecraftBridge.ENABLED || event.getMember() == null || event.getAuthor().isBot() || !DissonanceConfig.INPUT_CHANNELS.get().contains(event.getChannel().getIdLong())) return;
        Component formatted = formatDiscordMessage(event.getMember(), event.getChannel(), event.getMessage());
        this.minecraft.execute(() -> this.minecraft.getPlayerList().broadcastSystemMessage(formatted, false));
    }
}
