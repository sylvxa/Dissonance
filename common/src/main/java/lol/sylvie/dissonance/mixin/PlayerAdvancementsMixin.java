package lol.sylvie.dissonance.mixin;

import lol.sylvie.dissonance.minecraft.MinecraftEvents;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    public void dissonance$onAwardAdvancement(AdvancementHolder advancement, String criterionKey, CallbackInfoReturnable<Boolean> cir) {
        if (!MinecraftToDiscordBridge.ENABLED) return;
        // this is more code copying than i would have liked but lambda mixins just don't work for some reason
        advancement.value().display().ifPresent((display) -> {
            if (display.shouldAnnounceChat() && player.level().getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS))
                MinecraftEvents.onAdvancementAwarded(player, advancement, display);
        });
    }
}
