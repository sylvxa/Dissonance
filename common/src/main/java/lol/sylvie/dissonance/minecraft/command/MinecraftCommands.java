package lol.sylvie.dissonance.minecraft.command;

import com.google.gson.FormattingStyle;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import lol.sylvie.dissonance.platform.Services;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.sql.SQLException;
import java.util.UUID;

public class MinecraftCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("dissonance");

        registerPurgeCommand(builder);
        registerUnlinkCommand(builder);

        dispatcher.register(builder);
    }

    private static final SimpleCommandExceptionType NO_CHANNEL = new SimpleCommandExceptionType(Component.literal("The output channel cannot be accessed or the bot is missing required permissions."));
    private static void registerPurgeCommand(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("purge")
                .requires(s -> Services.PLATFORM.hasPermission(s.getPlayer(), "dissonance.purge", Commands.LEVEL_OWNERS))
                .then(Commands.argument("amount", IntegerArgumentType.integer())
                .executes(context -> {
                    TextChannel channel = MinecraftToDiscordBridge.getOutputChannel();
                    if (channel == null || !channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
                        throw NO_CHANNEL.create();
                    }
                    channel.getIterableHistory()
                            .takeAsync(IntegerArgumentType.getInteger(context, "amount"))
                            .thenAccept(channel::purgeMessages)
                            .thenAccept((f) -> {
                                context.getSource().sendSuccess(() -> Component.literal("Successfully purged messages!"), false);
                            })
                            .exceptionally((error) -> {
                                Constants.LOG.error("Couldn't purge messages.", error);
                                context.getSource().sendFailure(Component.literal("There was an error while trying to purge messages."));
                                return null;
                            });

                    context.getSource().sendSystemMessage(Component.literal("Attempting to purge messages..."));
                    return 0;
                })));
    }

    private static final SimpleCommandExceptionType ALREADY_LINKED = new SimpleCommandExceptionType(Component.literal("You already have a Discord account linked."));
    private static final SimpleCommandExceptionType NOT_LINKED = new SimpleCommandExceptionType(Component.literal("You do not have a Discord account linked."));
    private static final SimpleCommandExceptionType DATABASE_ERROR = new SimpleCommandExceptionType(Component.literal("There was an error accessing the link database! Contact the server owner."));
    private static void registerUnlinkCommand(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("unlink")
                .requires(s -> Services.PLATFORM.hasPermission(s.getPlayer(), "dissonance.unlink", Commands.LEVEL_ALL))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    UUID uuid = player.getUUID();
                    if (DiscordLinking.getDiscordFromMinecraft(uuid) == null) {
                        throw NOT_LINKED.create();
                    }

                    try {
                        DiscordLinking.removeLinkFromMinecraft(uuid);
                        MutableComponent message = Component.literal("Successfully unlinked account!");
                        if (DiscordLinking.isWhitelistEnabled()) {
                            player.connection.disconnect(message.withStyle(ChatFormatting.GREEN));
                        } else {
                            context.getSource().sendSuccess(() -> message, false);
                        }
                    } catch (SQLException exception) {
                        throw DATABASE_ERROR.create();
                    }


                    return 0;
                }));
    }
}
