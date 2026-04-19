package choco.ratel.smartmoving.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("smartmoving.json");

    public static SmartMovingConfig INSTANCE = new SmartMovingConfig();

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            SmartMovingConfig loaded = GSON.fromJson(reader, SmartMovingConfig.class);
            if (loaded != null) INSTANCE = loaded;
        } catch (IOException e) {
            INSTANCE = new SmartMovingConfig();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException ignored) {}
    }

    private ConfigManager() {}
}
