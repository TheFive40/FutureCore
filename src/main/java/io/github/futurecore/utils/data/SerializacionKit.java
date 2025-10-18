package io.github.futurecore.utils.data;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.futurecore.events.customitems.SpellEvent.kitGogetta;
import static io.github.futurecore.utils.handlers.kairos.KairosStorage.getFolder;

public class SerializacionKit {
    private static final Gson gson = new Gson();


    public static void guardarLista( JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "kitsGogetta.json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(kitGogetta, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void cargarLista(JavaPlugin plugin) {
        File file = new File ( getFolder ( plugin ), "kitsGogetta.json" );
        if (!file.exists ( )) {
            kitGogetta = new ArrayList<> ( );
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<ArrayList<UUID>> () {}.getType();
            ArrayList<UUID> loaded = gson.fromJson(reader, type);
            kitGogetta = loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            kitGogetta = new CopyOnWriteArrayList<>();
        }
    }
}

