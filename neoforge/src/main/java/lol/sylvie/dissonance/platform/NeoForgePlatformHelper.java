package lol.sylvie.dissonance.platform;

import lol.sylvie.dissonance.platform.services.IPlatformHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class NeoForgePlatformHelper implements IPlatformHelper {
    public static final HashMap<String, PermissionNode<@NotNull Boolean>> NODES = new HashMap<>();

    @Override
    public Platform getPlatform() {
        return Platform.NEOFORGE;
    }

    @Override
    public boolean hasPermission(ServerPlayer player, String node, boolean defaultValue) {
        return PermissionAPI.getPermission(player, NODES.get(node));
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public Path getConfigDir() {
        Path configDir = FMLLoader.getCurrent().getGameDir().resolve("config");
        if (Files.notExists(configDir)) if (!configDir.toFile().mkdir()) throw new IllegalStateException("Cannot get or make config directory!");
        return configDir;
    }
}