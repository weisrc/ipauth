package studio.weis.ipauth;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Files;

public class ConfigManager {
    private final File configFile;
    public Config config;

    public ConfigManager() {
        config = new Config();
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), IpAuth.MOD_ID + ".json");
        try {
            if (configFile.exists()) {
                IpAuth.LOGGER.info("Loading configuration...");
                config = IpAuth.GSON.fromJson(Files.readString(configFile.toPath()), Config.class);
            } else {
                IpAuth.LOGGER.info("Creating new configuration...");
                saveConfig();
            }
        } catch (Exception exception) {
            IpAuth.LOGGER.error("Failed to load configuration.", exception);
        }
    }

    public void saveConfig() {
        try {
            Files.writeString(configFile.toPath(), config.serialize());
        } catch (Exception exception) {
            IpAuth.LOGGER.error("Failed to save configuration.", exception);
        }
    }
}
