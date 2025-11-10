package lol.sylvie.dissonance.discord.command;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class DiscordCommands {
    public static void register(Guild guild) {
        guild.updateCommands().addCommands(
                Commands.slash("link", "Links your Minecraft account to your Discord account.")
                        .addOption(OptionType.STRING, "code", "Link code (displayed when joining the server)", true),
                Commands.slash("unlink", "Unlinks your Minecraft account to your Discord account.")
        ).queue();
    }
}
