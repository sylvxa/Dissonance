package lol.sylvie.dissonance.minecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import lol.sylvie.dissonance.platform.Services;
import lol.sylvie.dissonance.platform.services.IPlatformHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Predicate;

public class MinecraftCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("dissonance");

        registerPurgeCommand(builder);
        registerLinkCommand(builder);
        registerUnlinkCommand(builder);

        dispatcher.register(builder);
    }

    public static Predicate<CommandSourceStack> getPermissionPredicate(String permission, PermissionCheck value) {
        return s -> {
            boolean defaultValue = value.check(s.permissions());
            return s.isPlayer() && !Services.PLATFORM.getPlatform().equals(IPlatformHelper.Platform.NEOFORGE) ? Services.PLATFORM.hasPermission(s.getPlayer(), permission, defaultValue) : value.check(s.permissions());
        };
    }

    private static final SimpleCommandExceptionType NO_CHANNEL = new SimpleCommandExceptionType(Component.literal("The output channel cannot be accessed or the bot is missing required permissions."));
    private static void registerPurgeCommand(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("purge")
                .requires(CommandSourceStack::isPlayer)
                .requires(getPermissionPredicate("dissonance.purge", Commands.LEVEL_OWNERS))
                .then(Commands.argument("amount", IntegerArgumentType.integer())
                .executes(context -> {
                    TextChannel channel = MinecraftToDiscordBridge.getOutputChannel();
                    if (channel == null || !channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
                        throw NO_CHANNEL.create();
                    }
                    channel.getIterableHistory()
                            .takeAsync(IntegerArgumentType.getInteger(context, "amount"))
                            .thenAccept(channel::purgeMessages)
                            .thenAccept((f) -> context.getSource().sendSuccess(() -> Component.literal("Successfully purged messages!"), false))
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
    private static final SimpleCommandExceptionType LINKING_OFF = new SimpleCommandExceptionType(Component.literal("Account linking is unavailable."));
    private static void registerUnlinkCommand(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("unlink")
                .requires(getPermissionPredicate("dissonance.unlink", Commands.LEVEL_ALL))
                .executes(context -> {
                    if (!DiscordLinking.isConnected()) throw LINKING_OFF.create();

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

    private static void registerLinkCommand(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("link")
                .requires(getPermissionPredicate("dissonance.link", Commands.LEVEL_ALL))
                .executes(context -> {
                    if (!DiscordLinking.isConnected()) throw LINKING_OFF.create();

                    ServerPlayer player = context.getSource().getPlayerOrException();
                    UUID uuid = player.getUUID();
                    if (DiscordLinking.getDiscordFromMinecraft(uuid) != null) {
                        throw ALREADY_LINKED.create();
                    }

                    String linkCode = DiscordLinking.generateCode(player.nameAndId());
                    context.getSource().sendSuccess(() -> Component.literal("Your link code is ").append(Component.literal(linkCode).withStyle(ChatFormatting.BOLD)), false);

                    return 0;
                }));
    }
}
