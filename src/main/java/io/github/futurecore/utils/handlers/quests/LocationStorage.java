package io.github.futurecore.utils.handlers.quests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import static io.github.futurecore.utils.handlers.kairos.KairosStorage.getFolder;

public class LocationStorage {
    private static final Gson gson = new GsonBuilder ().setPrettyPrinting().create();

    public static void saveNpcLocations( JavaPlugin plugin, Map<String, Location> map) {
        File file = new File(getFolder(plugin), "npc_locations.json");
        try (Writer writer = new OutputStreamWriter (new FileOutputStream (file), StandardCharsets.UTF_8)) {
            Map<String, Map<String, Object>> serializableMap = new HashMap<>();
            for (Map.Entry<String, Location> entry : map.entrySet()) {
                Location loc = entry.getValue();
                Map<String, Object> locData = new HashMap<> ();
                locData.put("world", loc.getWorld().getName());
                locData.put("x", loc.getX());
                locData.put("y", loc.getY());
                locData.put("z", loc.getZ());
                locData.put("yaw", loc.getYaw());
                locData.put("pitch", loc.getPitch());
                serializableMap.put(entry.getKey(), locData);
            }
            gson.toJson(serializableMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Map<String, Location> loadNpcLocations(JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "npc_locations.json");
        Map<String, Location> result = new HashMap<>();

        if (!file.exists()) return result;

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Map<String, Object>>> () {}.getType();
            Map<String, Map<String, Object>> data = gson.fromJson(reader, type);
            for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
                Map<String, Object> locData = entry.getValue();
                World world = Bukkit.getWorld((String) locData.get("world"));
                if (world == null) continue;
                double x = ((Number) locData.get("x")).doubleValue();
                double y = ((Number) locData.get("y")).doubleValue();
                double z = ((Number) locData.get("z")).doubleValue();
                float yaw = ((Number) locData.get("yaw")).floatValue();
                float pitch = ((Number) locData.get("pitch")).floatValue();
                result.put(entry.getKey(), new Location(world, x, y, z, yaw, pitch));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

}
