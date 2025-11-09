package lol.sylvie.dissonance;

import net.fabricmc.api.ModInitializer;

public class FabricDissonance implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world!");
        Dissonance.init();
    }
}
