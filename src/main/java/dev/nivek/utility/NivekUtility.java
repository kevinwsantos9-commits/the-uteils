package dev.nivek.utility;

import dev.nivek.utility.config.ServerConfig;
import dev.nivek.utility.farming.FarmingHandler;
import dev.nivek.utility.mining.BreakHandler;
import dev.nivek.utility.network.Payloads;
import dev.nivek.utility.network.ServerNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrada comum (servidor + cliente). Só contém lógica que roda no lado do servidor. */
public class NivekUtility implements ModInitializer {
    public static final String MOD_ID = "nivekutility";
    public static final Logger LOGGER = LoggerFactory.getLogger("NivekUtility");

    @Override
    public void onInitialize() {
        ServerConfig.load();
        Payloads.registerAll();
        ServerNetworking.register();
        BreakHandler.register();
        FarmingHandler.register();
        LOGGER.info("Nivek Utility carregado.");
    }
}
