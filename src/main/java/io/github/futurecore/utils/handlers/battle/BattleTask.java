package io.github.futurecore.utils.handlers.battle;

import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.utils.handlers.battle.BattleData.battlePending;

public class BattleTask {

    // Tasks separadas por tipo para evitar colisiones
    private static final Map<UUID, Integer> requestTasks = new HashMap<>();    // expiracion de solicitud (60s)
    private static final Map<String, Integer> countdownTasks = new HashMap<>(); // cuenta regresiva antes del combate
    private static final Map<String, Integer> battleTimerTasks = new HashMap<>(); // timer max del combate (5m)

    // ---------- Solicitud (expiracion 60s) ----------
    public static void scheduleExpiration(final Player source, final Player target) {
        // Cancelar si ya existía una tarea para este target
        if (requestTasks.containsKey(target.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(requestTasks.remove(target.getUniqueId()));
        }

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(Main.instance, new Runnable() {
            @Override
            public void run() {
                if (battlePending.containsKey(target.getUniqueId())
                        && battlePending.get(target.getUniqueId()).equals(source.getUniqueId())) {

                    battlePending.remove(target.getUniqueId());
                    source.sendMessage(CC.translate("&8[&c&lBattle&8] &eTu solicitud de duelo a &6" + target.getName() + " &eha expirado."));
                    target.sendMessage(CC.translate("&8[&c&lBattle&8] &eLa solicitud de duelo de &6" + source.getName() + " &eha expirado."));
                }
                requestTasks.remove(target.getUniqueId());
            }
        }, 20L * 60); // 60s
        requestTasks.put(target.getUniqueId(), taskId);
    }

    public static void cancelExpiration(UUID targetId) {
        if (requestTasks.containsKey(targetId)) {
            Bukkit.getScheduler().cancelTask(requestTasks.remove(targetId));
        }
    }

    // ---------- Cuenta regresiva antes del combate (ej. 5s) ----------
    public static void startBattleCountdown(final Player p1, final Player p2) {
        // cancelar expiracion de solicitud del target por si sigue activa
        cancelExpiration(p2.getUniqueId());

        final String key = pairKey(p1.getUniqueId(), p2.getUniqueId());

        // Si ya hay una cuenta regresiva para ese par, cancelar y reemplazar
        if (countdownTasks.containsKey(key)) {
            Bukkit.getScheduler().cancelTask(countdownTasks.remove(key));
        }

        final int[] seconds = {5};
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.instance, new Runnable() {
            @Override
            public void run() {
                if (seconds[0] > 0) {
                    p1.sendMessage(CC.translate("&8[&c&lBattle&8] &eEl combate comienza en &c" + seconds[0] + "s"));
                    p2.sendMessage(CC.translate("&8[&c&lBattle&8] &eEl combate comienza en &c" + seconds[0] + "s"));
                    seconds[0]--;
                } else {
                    // iniciar combate
                    p1.sendMessage(CC.translate("&8[&c&lBattle&8] &a¡El duelo ha comenzado!"));
                    p2.sendMessage(CC.translate("&8[&c&lBattle&8] &a¡El duelo ha comenzado!"));

                    // registrar como duelo activo
                    BattleData.startBattle(p1.getUniqueId(), p2.getUniqueId());

                    // cancelar esta cuenta regresiva y quitar del mapa
                    Integer id = countdownTasks.remove(key);
                    if (id != null) Bukkit.getScheduler().cancelTask(id);

                    // arrancar timer max (5 minutos)
                    startBattleTimer(p1, p2);
                }
            }
        }, 0L, 20L); // 1s
        countdownTasks.put(key, taskId);
    }

    public static void cancelCountdown(UUID p1, UUID p2) {
        String key = pairKey(p1, p2);
        if (countdownTasks.containsKey(key)) {
            Bukkit.getScheduler().cancelTask(countdownTasks.remove(key));
        }
    }

    // ---------- Timer máximo del combate (5 minutos) ----------
    public static void startBattleTimer(final Player p1, final Player p2) {
        final String key = pairKey(p1.getUniqueId(), p2.getUniqueId());

        // cancelar timer previo si existiera
        if (battleTimerTasks.containsKey(key)) {
            Bukkit.getScheduler().cancelTask(battleTimerTasks.remove(key));
        }

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(Main.instance, new Runnable() {
            @Override
            public void run() {
                if (BattleData.inBattle(p1.getUniqueId(), p2.getUniqueId())) {
                    // finalizar por empate tecnico
                    BattleData.endBattle(p1.getUniqueId(), p2.getUniqueId());

                    p1.sendMessage(CC.translate("&8[&c&lBattle&8] &eEl combate ha durado demasiado y se declara un &6empate técnico&e."));
                    p2.sendMessage(CC.translate("&8[&c&lBattle&8] &eEl combate ha durado demasiado y se declara un &6empate técnico&e."));

                    // devolver a spawn (o usa warp si prefieres)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p1.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p2.getName());
                }
                battleTimerTasks.remove(key);
            }
        }, 20L * 300); // 300s = 5min

        battleTimerTasks.put(key, taskId);
    }

    public static void cancelBattleTimer(UUID p1, UUID p2) {
        String key = pairKey(p1, p2);
        if (battleTimerTasks.containsKey(key)) {
            Bukkit.getScheduler().cancelTask(battleTimerTasks.remove(key));
        }
    }

    // ---------- Utilities ----------
    private static String pairKey(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a.toString() + ":" + b.toString() : b.toString() + ":" + a.toString();
    }

    /** Cancela todas las tareas (útil en onDisable) */
    public static void clearAllTasks() {
        for (Integer id : requestTasks.values()) Bukkit.getScheduler().cancelTask(id);
        for (Integer id : countdownTasks.values()) Bukkit.getScheduler().cancelTask(id);
        for (Integer id : battleTimerTasks.values()) Bukkit.getScheduler().cancelTask(id);
        requestTasks.clear();
        countdownTasks.clear();
        battleTimerTasks.clear();
    }
}
