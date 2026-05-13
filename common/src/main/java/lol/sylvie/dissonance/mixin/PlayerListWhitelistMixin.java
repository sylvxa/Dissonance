package lol.sylvie.dissonance.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public class PlayerListWhitelistMixin {
    @ModifyReturnValue(method = "canPlayerLogin", at = @At("TAIL"))
    public Component dissonance$addWhitelistCheck(Component original, @Local(argsOnly = true) NameAndId profile) {
        if (original != null) return original;

        return DiscordLinking.canPlayerJoin((PlayerList) (Object) this, profile);
    }
}
