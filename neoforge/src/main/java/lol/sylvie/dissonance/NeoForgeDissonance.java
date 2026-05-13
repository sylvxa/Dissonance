package lol.sylvie.dissonance;


import com.mojang.authlib.GameProfile;
import lol.sylvie.dissonance.config.DissonanceConfig;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import lol.sylvie.dissonance.minecraft.MinecraftEvents;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import lol.sylvie.dissonance.minecraft.command.MinecraftCommands;
import lol.sylvie.dissonance.permission.DissonancePermissions;
import lol.sylvie.dissonance.platform.NeoForgePlatformHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.players.UserWhiteList;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@Mod(Constants.MOD_ID)
public class NeoForgeDissonance {
    public static boolean ENABLE_MIXINS = false;

    public NeoForgeDissonance(ModContainer container, IEventBus bus) {
        container.registerConfig(ModConfig.Type.SERVER, DissonanceConfig.SPEC);

        Dissonance.modInit();

        NeoForge.EVENT_BUS.addListener((Consumer<PermissionGatherEvent.Nodes>) event -> DissonancePermissions.NODES.forEach((node, value) -> {
            PermissionNode<@NotNull Boolean> permissionNode = new PermissionNode<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, node), PermissionTypes.BOOLEAN, (p, u, c) -> value);
            NeoForgePlatformHelper.NODES.put(node, permissionNode);
            event.addNodes(permissionNode);
        }));

        NeoForge.EVENT_BUS.addListener((Consumer<RegisterCommandsEvent>) event -> MinecraftCommands.register(event.getDispatcher()));

        // Events
        NeoForge.EVENT_BUS.addListener((Consumer<ServerStartedEvent>) event -> Dissonance.serverStarted(event.getServer(), () -> registerEvents(bus)));
    }

    public void registerEvents(IEventBus bus) {
        ENABLE_MIXINS = true;

        NeoForge.EVENT_BUS.addListener((Consumer<ServerStoppingEvent>) event -> Dissonance.serverStopping());

        NeoForge.EVENT_BUS.addListener((Consumer<ServerTickEvent.Post>) event -> MinecraftEvents.tick(event.getServer()));

        if (!MinecraftToDiscordBridge.ENABLED) return;
        NeoForge.EVENT_BUS.addListener((Consumer<ServerChatEvent>) event -> MinecraftToDiscordBridge.onPlayerChatMessage(event.getPlayer(), event.getRawText()));

        NeoForge.EVENT_BUS.addListener((Consumer<PlayerEvent.PlayerLoggedInEvent>) event -> MinecraftEvents.onPlayerJoin(event.getEntity()));

        NeoForge.EVENT_BUS.addListener((Consumer<PlayerEvent.PlayerLoggedOutEvent>) event -> MinecraftEvents.onPlayerLeave(event.getEntity()));
    }
}