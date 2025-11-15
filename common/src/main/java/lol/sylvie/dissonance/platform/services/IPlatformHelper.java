package lol.sylvie.dissonance.platform.services;

import net.minecraft.server.level.ServerPlayer;

public interface IPlatformHelper {
    String getPlatformName();

    boolean hasPermission(ServerPlayer player, String node, int defaultValue);

    boolean isDevelopmentEnvironment();
}