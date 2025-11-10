package lol.sylvie.dissonance.discord.command.impl;

import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.SQLException;

public class UnlinkCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("unlink")) return;

        User user = event.getUser();
        if (DiscordLinking.getMinecraftFromDiscord(user.getId()) == null) {
            event.reply("Your account is not linked! **(use `/link`)**").queue();
            return;
        }

        try {
            DiscordLinking.removeLinkFromDiscord(user.getId());
            event.reply("Successfully unlinked account!").queue();
        } catch (SQLException e) {
            Constants.LOG.error("Error while unlinking!", e);
            event.reply("There was an error trying to link your account.").queue();
        }
    }
}
