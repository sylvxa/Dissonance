package lol.sylvie.dissonance;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import lol.sylvie.dissonance.config.DissonanceConfig;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import lol.sylvie.dissonance.minecraft.MinecraftEvents;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import lol.sylvie.dissonance.minecraft.command.MinecraftCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;

public class FabricDissonance implements ModInitializer {
    @Override
    public void onInitialize() {
        NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, DissonanceConfig.SPEC);

        Dissonance.modInit();

        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> MinecraftCommands.register(dispatcher));

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            Component failureReason = DiscordLinking.canPlayerJoin(server, handler.authenticatedProfile);
            if (failureReason == null) return;
            handler.disconnect(failureReason);
        });

        // Events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> Dissonance.serverStarted(server, this::registerEvents));
    }

    public void registerEvents() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Dissonance.serverStopping());

        ServerTickEvents.END_SERVER_TICK.register(MinecraftEvents::tick);

        if (!MinecraftToDiscordBridge.ENABLED) return;
        ServerMessageEvents.CHAT_MESSAGE.register((message, player, bound) -> MinecraftToDiscordBridge.onPlayerChatMessage(player, message.signedContent()));

        ServerPlayerEvents.JOIN.register(MinecraftEvents::onPlayerJoin);
        ServerPlayerEvents.LEAVE.register(MinecraftEvents::onPlayerLeave);
        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            if (!overlay) MinecraftEvents.onMiscMessage(message);
        });
    }
}
