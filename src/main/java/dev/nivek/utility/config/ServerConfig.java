package dev.nivek.utility.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.nivek.utility.NivekUtility;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Regras do servidor: limites máximos e permissão de cada recurso. Arquivo: config/nivekutility-server.json */
public class ServerConfig {
    public boolean allowTree = true;
    public boolean allowVein = true;
    public boolean allowArea = true;
    public boolean allowFarmAuto = true;
    public boolean allowTorch = true;
    public boolean allowOrganize = true;
    public int maxTreeBlocks = 256;
    public int maxVeinBlocks = 256;
    public int maxFarmRadius = 5;

    private static ServerConfig instance = new ServerConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ServerConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("nivekutility-server.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                ServerConfig c = GSON.fromJson(Files.readString(p), ServerConfig.class);
                if (c != null) instance = c;
            }
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(instance));
        } catch (Exception e) {
            NivekUtility.LOGGER.warn("Não foi possível ler/gravar a config do servidor", e);
        }
    }
}
