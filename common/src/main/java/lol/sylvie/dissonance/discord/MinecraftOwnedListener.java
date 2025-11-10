package lol.sylvie.dissonance.discord;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;

public abstract class MinecraftOwnedListener extends ListenerAdapter {
    protected final MinecraftServer minecraft;

    public MinecraftOwnedListener(MinecraftServer minecraft) {
        this.minecraft = minecraft;
    }
}
