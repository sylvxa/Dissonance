package lol.sylvie.dissonance.mixin;

import lol.sylvie.dissonance.NeoForgeDissonance;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    // NeoForge doesn't have this equivalent event while Fabric does.
    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V", at = @At("HEAD"))
    private void dissonance$onSendGameMessage(Component message, Function<ServerPlayer, Component> playerMessageFactory, boolean overlay, CallbackInfo ci) {
        if (!NeoForgeDissonance.ENABLE_MIXINS || !MinecraftToDiscordBridge.ENABLED) return;
        MinecraftToDiscordBridge.onMiscMessage(message);
    }
}
