package lol.sylvie.dissonance.discord.command.impl;

import com.mojang.datafixers.util.Pair;
import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.minecraft.ChatFormatting;
import net.minecraft.server.players.NameAndId;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.SQLException;

public class LinkCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("link")) return;

        User user = event.getUser();
        if (DiscordLinking.getMinecraftFromDiscord(user.getId()) != null) {
            event.reply("Your account is already linked! **(use `/unlink`)**").queue();
            return;
        }

        OptionMapping mapping = event.getOption("code");
        if (mapping == null) {
            event.reply("You must provide a link code!").queue();
            return;
        }

        String code = mapping.getAsString();

        if (code.length() != 6 || !NumberUtils.isDigits(code)) {
            event.reply("That code is invalid!").queue();
            return;
        }

        Pair<Long, NameAndId> result = DiscordLinking.LINK_CODES.get(code);
        if (result != null && !DiscordLinking.hasCodeExpired(result)) {
            try {
                NameAndId profile = result.getSecond();
                DiscordLinking.addLink(user, profile.id());

                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(ChatFormatting.GREEN.getColor());
                builder.setImage("https://mc-heads.net/head/" + profile.id());
                builder.setTitle("Your account is linked!");
                builder.setDescription(String.format("%s <-> %s", event.getUser().getAsMention(), profile.name()));
                event.replyEmbeds(builder.build()).queue();

                DiscordLinking.LINK_CODES.remove(code);
            } catch (SQLException e) {
                Constants.LOG.error("Error while linking!", e);
                event.reply("There was an error trying to link your account.").setEphemeral(true).queue();
            }
        } else {
            event.reply("Wrong code!").setEphemeral(true).queue();
        }
    }
}
