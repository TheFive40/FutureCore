package io.github.futurecore.utils.data.KairosDataHandler;

import io.github.futurecore.utils.data.KairosData.ItemKairos;
import io.github.futurecore.utils.data.KairosData.PKairos;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.futurecore.utils.handlers.kairos.KairosStorage.getFolder;

public class KairosDataHandler implements Serializable {
    public static final CopyOnWriteArrayList<PKairos> pKairos = new CopyOnWriteArrayList<> ( );
    public static final ConcurrentHashMap<Integer,ItemKairos> itemsKairos = new ConcurrentHashMap (  );
    public static PKairos findPKairos ( UUID playerUUID ) {
        return pKairos.stream ( ).filter ( e -> e.getPlayerUUID ( ).equals ( playerUUID ) )
                .findFirst ( ).get ( );
    }

    public static void refreshPKairos ( PKairos pKairos ) {
        KairosDataHandler.pKairos.remove ( pKairos );
        KairosDataHandler.pKairos.add ( pKairos );
    }
    private static final Gson gson = new Gson();

    // El mapa con claves como String (serializadas)
    public static ConcurrentHashMap<String, String> itemsByID = new ConcurrentHashMap<>();

    // 🔹 Guardar el mapa en JSON
    public static void guardarMapa(JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "itemsByID.json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(itemsByID, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 🔹 Cargar el mapa desde JSON
    public static void cargarMapa(JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "itemsByID.json");
        if (!file.exists()) {
            itemsByID.clear();
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<ConcurrentHashMap<String, String>>() {}.getType();
            ConcurrentHashMap<String, String> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                itemsByID.clear();
                itemsByID.putAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
            itemsByID.clear();
        }
    }

    // 🔹 Helper para guardar usando ArrayList<String> como clave
    public static void putItem(ArrayList<String> keyList, String customID) {
        String key = gson.toJson(keyList); // convierte la lista a string JSON
        itemsByID.put(key, customID);
    }

    // 🔹 Helper para obtener ArrayList<String> desde la clave guardada
    public static ArrayList<String> getKeyAsList(String key) {
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(key, type);
    }

    // 🔹 Helper para buscar por ArrayList<String>
    public static String getCustomId(ArrayList<String> keyList) {
        String key = gson.toJson(keyList);
        return itemsByID.get(key);
    }
}
