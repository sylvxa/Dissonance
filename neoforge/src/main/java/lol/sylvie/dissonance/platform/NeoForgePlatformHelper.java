package lol.sylvie.dissonance.platform;

import lol.sylvie.dissonance.platform.services.IPlatformHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

import java.util.HashMap;

public class NeoForgePlatformHelper implements IPlatformHelper {
    public static HashMap<String, PermissionNode<Integer>> INTEGER_NODES = new HashMap<>();

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean hasPermission(ServerPlayer player, String node, int defaultValue) {
        return player.hasPermissions(PermissionAPI.getPermission(player, INTEGER_NODES.get(node)));
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }
}