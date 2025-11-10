package lol.sylvie.dissonance.discord;

import lol.sylvie.dissonance.config.DissonanceConfig;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public class DiscordToMinecraftBridge extends MinecraftOwnedListener {
    public static boolean ENABLED = true;

    public DiscordToMinecraftBridge(MinecraftServer server) {
        super(server);
    }

    public static Component optionallyColorMember(int color, MutableComponent component) {
        return DissonanceConfig.USE_ROLE_COLORS.get() ? component.withColor(color) : component;
    }

    // I'm not sure how much of a performance impact string replacement has,
    // but it might be worth adding aliases like %username% or %content%
    public static Component formatDiscordMessage(Member member, Channel channel, Message message) {
        String template = DissonanceConfig.MINECRAFT_MESSAGE_TEMPLATE.get();
        int color = member.getColorRaw();

        Component username = optionallyColorMember(color, Component.literal(member.getUser().getName()));
        Component description = optionallyColorMember(color, Component.literal(member.getEffectiveName()));
        Component content = Component.literal(message.getContentDisplay());
        Component channelName = Component.literal(channel.getName());

        return Component.translatable(template,
                username,
                description,
                content,
                channelName);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!DiscordToMinecraftBridge.ENABLED || event.getMember() == null || event.getAuthor().isBot() || !DissonanceConfig.INPUT_CHANNELS.get().contains(event.getChannel().getId())) return;
        Component formatted = formatDiscordMessage(event.getMember(), event.getChannel(), event.getMessage());
        this.minecraft.execute(() -> {
            this.minecraft.getPlayerList().broadcastSystemMessage(formatted, false);
        });
    }
}
