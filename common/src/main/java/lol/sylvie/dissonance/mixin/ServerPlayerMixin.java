package lol.sylvie.dissonance.mixin;

import com.mojang.authlib.GameProfile;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;", shift = At.Shift.AFTER))
    public void dissonance$onDeath(DamageSource cause, CallbackInfo ci) {
        if (!MinecraftToDiscordBridge.ENABLED) return;
        MinecraftToDiscordBridge.onPlayerDeath(this, this.getCombatTracker().getDeathMessage());
    }
}
