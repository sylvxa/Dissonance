package lol.sylvie.dissonance.discord.events;

import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import lol.sylvie.dissonance.discord.proximity.DiscordProximity;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordEvents extends ListenerAdapter {
    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        if (!DiscordLinking.isConnected()) return;
        DiscordLinking.onGuildMemberRemove(event);
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (!DiscordProximity.ENABLED) return;
        DiscordProximity.onGuildVoiceUpdate(event);
    }
}
