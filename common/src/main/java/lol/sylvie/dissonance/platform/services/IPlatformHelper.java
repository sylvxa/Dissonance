package lol.sylvie.dissonance.platform.services;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IPlatformHelper {
    String getPlatformName();

    boolean hasPermission(ServerPlayer player, String node, int defaultValue);

    boolean isDevelopmentEnvironment();
}