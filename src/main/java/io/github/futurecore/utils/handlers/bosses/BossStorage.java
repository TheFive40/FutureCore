package io.github.futurecore.utils.handlers.bosses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.futurecore.commands.player.cmdBosses.CmdBosses.npcNames;
import static io.github.futurecore.commands.player.cmdBosses.CmdBosses.npcRarity;
import static io.github.futurecore.utils.handlers.kairos.KairosStorage.getFolder;

public class BossStorage {
    private static JavaPlugin plugin;
    private static final Gson gson = new GsonBuilder ().setPrettyPrinting().create();

    public static void saveNpcRarity( JavaPlugin plugin) {
        BossStorage.plugin = plugin;
        File file = new File(getFolder(plugin), "npc_rarity.json");
        try (Writer writer = new OutputStreamWriter (new FileOutputStream (file), StandardCharsets.UTF_8)) {
            gson.toJson(npcRarity, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadNpcRarity(JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "npc_rarity.json");
        if (!file.exists()) {
            npcRarity = new HashMap<>();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<HashMap<String, Integer>> () {}.getType();
            HashMap<String, Integer> loaded = gson.fromJson(reader, type);
            npcRarity = loaded != null ? loaded : new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            npcRarity = new HashMap<>();
        }
    }
    public static void saveNpcNames(JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "npc_names.json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(npcNames, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadNpcNames(JavaPlugin plugin) {
        File file = new File(getFolder(plugin), "npc_names.json");
        if (!file.exists()) {
            npcNames = new CopyOnWriteArrayList<>();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<CopyOnWriteArrayList<String>>() {}.getType();
            CopyOnWriteArrayList<String> loaded = gson.fromJson(reader, type);
            npcNames = loaded != null ? loaded : new CopyOnWriteArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            npcNames = new CopyOnWriteArrayList<>();
        }
    }

}
