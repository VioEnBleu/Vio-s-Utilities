package com.vioenbleu.viosutilities.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource; // Mojang equivalent of WorldSavePath

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReturnLocationData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<UUID, SavedPos> data = new HashMap<>();

    public record SavedPos(String worldId, double x, double y, double z, float yaw, float pitch) {}

    public static void saveLocation(UUID uuid, SavedPos pos, MinecraftServer server) {
        data.put(uuid, pos);
        saveToFile(server);
    }

    public static SavedPos getLocation(UUID uuid, MinecraftServer server) {
        // Load data if the map is empty (e.g., after a server restart)
        if (data.isEmpty()) loadFromFile(server);
        return data.get(uuid);
    }

    private static File getFile(MinecraftServer server) {
        // MOJANG FIX: getWorldPath instead of getSavePath
        // MOJANG FIX: LevelResource.ROOT instead of WorldSavePath.ROOT
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("vios_return_data.json");
        return path.toFile();
    }

    private static void saveToFile(MinecraftServer server) {
        try (Writer writer = new FileWriter(getFile(server))) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadFromFile(MinecraftServer server) {
        File file = getFile(server);
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Map<UUID, SavedPos> loadedData = GSON.fromJson(reader, new TypeToken<Map<UUID, SavedPos>>(){}.getType());
            if (loadedData != null) {
                data = loadedData;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}