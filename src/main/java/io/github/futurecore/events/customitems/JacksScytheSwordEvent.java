package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.HammerChaosEvent.spawnHologram;

public class JacksScytheSwordEvent implements Listener {

    public static final Set<Location> bloquesTemporales = new HashSet<>();
    public static final ConcurrentHashMap<UUID, Location> froozePlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final int COOLDOWN_MS = 4 * 60 * 1000;

    @EventHandler
    public void onClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;

        Player lanzador = event.getPlayer();
        Player objetivo = (Player) event.getRightClicked();

        if (lanzador.getItemInHand().getTypeId() != 6105) return;

        UUID lanzadorID = lanzador.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(lanzadorID)) {
            long lastUse = cooldowns.get(lanzadorID);
            long elapsed = now - lastUse;
            if (elapsed < COOLDOWN_MS) {
                long remaining = COOLDOWN_MS - elapsed;
                int seconds = (int) (remaining / 1000) % 60;
                int minutes = (int) (remaining / 1000) / 60;
                spawnHologram(lanzador, String.format("§e⌛ Espada en cooldown: %02d:%02d", minutes, seconds));
                return;
            }
        }

        Location loc = objetivo.getLocation();
        World world = objetivo.getWorld();

        int[][] offsets = {
                {-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1},
                {-1, 1, 0}, {1, 1, 0}, {0, 1, -1}, {0, 1, 1},
                {-1, 2, 0}, {1, 2, 0}, {0, 2, -1}, {0, 2, 1},
                {0, 3, 0},
                {0, -1, 0}
        };
        try {
            ItemStack item = lanzador.getItemInHand ( );

            if (!hasPermanent ( item )) {
                ItemStack updated = CmdItemUsage.hasUses ( item, lanzador );
                if (updated == null) {
                    lanzador.setItemInHand ( null );
                } else {
                    lanzador.setItemInHand ( updated );
                }
                lanzador.updateInventory ( );
            }
        } catch (Exception ignored) {
        }
        List<Location> bloquesColocados = new ArrayList<>();

        for (int[] offset : offsets) {
            Location bloqueLoc = loc.clone().add(offset[0], offset[1], offset[2]);
            Block bloque = world.getBlockAt(bloqueLoc);
            bloquesTemporales.add(bloqueLoc);
            if (bloque.getType() == Material.AIR || bloque.isLiquid()) {
                bloque.setType(Material.ICE);
                bloquesColocados.add(bloqueLoc);
            }
        }

        spawnHologram(objetivo, "&b¡Has sido congelado por " + lanzador.getName() + "!");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "congelar " + objetivo.getName());
        spawnHologram(lanzador, "&b¡Has encerrado en hielo a " + objetivo.getName() + "!");
        froozePlayer.put(objetivo.getUniqueId(), objetivo.getLocation());

        cooldowns.put(lanzadorID, now);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Location l : bloquesColocados) {
                    if (l.getBlock().getType() == Material.ICE) {
                        l.getBlock().setType(Material.AIR);
                        bloquesTemporales.remove(l);
                    }
                }
                froozePlayer.remove(objetivo.getUniqueId());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "descongelar " + objetivo.getName());
                spawnHologram(objetivo, "&7El hielo se derrite...");
            }
        }.runTaskLater(Main.instance, 20 * 10);
    }
}
