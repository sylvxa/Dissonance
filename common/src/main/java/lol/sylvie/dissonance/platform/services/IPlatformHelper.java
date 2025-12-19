package lol.sylvie.dissonance.platform.services;

import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {
    Platform getPlatform();

    boolean hasPermission(ServerPlayer player, String node, boolean defaultValue);

    boolean isDevelopmentEnvironment();

    Path getConfigDir();

    enum Platform {
        NEOFORGE("NeoForge"),
        FABRIC("Fabric");

        private final String name;

        Platform(String name) {
            this.name = name;
        }

        public String getRealName() {
            return name;
        }
    }
}