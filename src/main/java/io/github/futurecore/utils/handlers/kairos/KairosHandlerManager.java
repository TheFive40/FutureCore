package io.github.futurecore.utils.handlers.kairos;

import io.github.futurecore.Main;
import io.github.futurecore.utils.data.KairosDataHandler.KairosDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;

public class KairosHandlerManager {

    public static void startAutoSave(JavaPlugin plugin) {
        long delayTicks = TimeUnit.MINUTES.toSeconds(10) * 20; // 10 minutos en ticks

        new BukkitRunnable() {
            @Override
            public void run() {
                KairosStorage.saveKairosList(plugin, KairosDataHandler.pKairos);
                KairosStorage.saveItemsMap(plugin, KairosDataHandler.itemsKairos);
                Bukkit.getLogger().info("[Kairos] Datos guardados automáticamente.");
            }
        }.runTaskTimerAsynchronously(plugin, delayTicks, delayTicks);
    }
}
