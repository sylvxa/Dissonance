package lol.sylvie.dissonance;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class NeoForgeDissonance {
    public NeoForgeDissonance(IEventBus eventBus) {
        Constants.LOG.info("Hello NeoForge world!");
        Dissonance.init();
    }
}