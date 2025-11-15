package lol.sylvie.dissonance.platform;

import lol.sylvie.dissonance.platform.services.IPlatformHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean hasPermission(ServerPlayer player, String node, int defaultValue) {
        return Permissions.check(player, node, defaultValue);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
