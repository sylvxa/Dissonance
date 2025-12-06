package lol.sylvie.dissonance.platform.services;

import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {
    String getPlatformName();

    boolean hasPermission(ServerPlayer player, String node, int defaultValue);

    boolean isDevelopmentEnvironment();

    Path getConfigDir();
}