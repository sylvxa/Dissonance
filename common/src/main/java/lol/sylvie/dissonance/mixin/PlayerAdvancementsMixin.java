package lol.sylvie.dissonance.mixin;

import lol.sylvie.dissonance.minecraft.MinecraftEvents;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "lambda$award$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public void dissonance$onAwardAdvancement(AdvancementHolder holder, DisplayInfo display, CallbackInfo ci) {
        if (!MinecraftToDiscordBridge.ENABLED) return;

        MinecraftEvents.onAdvancementAwarded(player, holder, display);
    }
}
