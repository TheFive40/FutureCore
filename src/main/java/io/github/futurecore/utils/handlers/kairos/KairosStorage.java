package io.github.futurecore.utils.handlers.kairos;
import io.github.futurecore.utils.data.KairosData.ItemKairos;
import io.github.futurecore.utils.data.KairosData.PKairos;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class KairosStorage {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static File getFolder ( JavaPlugin plugin ) {
        File folder = new File(plugin.getDataFolder(), "FutureCore/kairos/player");
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    public static void saveKairosList(JavaPlugin plugin, List<PKairos> list) {
        File file = new File(getFolder(plugin), "pkairos.json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<PKairos> loadKairosList(JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "pkairos.json");
        if (!file.exists()) return new CopyOnWriteArrayList<>();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<PKairos>>() {}.getType();
            List<PKairos> loaded = gson.fromJson(reader, type);
            return new CopyOnWriteArrayList<>(loaded != null ? loaded : new CopyOnWriteArrayList<>());
        } catch (IOException e) {
            e.printStackTrace();
            return new CopyOnWriteArrayList<>();
        }
    }



    public static void saveItemsMap(JavaPlugin plugin, Map<Integer, ItemKairos> map) {
        File file = new File(getFolder(plugin), "itemskairos.json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, ItemKairos> loadItemsMap(JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "itemskairos.json");
        if (!file.exists()) return new ConcurrentHashMap<>();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<Integer, ItemKairos>>() {}.getType();
            return new ConcurrentHashMap<>(gson.fromJson(reader, type));
        } catch (IOException e) {
            e.printStackTrace();
            return new ConcurrentHashMap<>();
        }
    }
}

