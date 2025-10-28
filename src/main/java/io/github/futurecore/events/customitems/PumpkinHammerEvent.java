package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import net.minecraft.server.v1_7_R4.PacketPlayOutWorldParticles;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

/**
 * Martillo de Calabaza Explosiva
 * Habilidad (Click Derecho): Golpea el suelo creando una onda expansiva de calabazas explosivas
 * que atrapan y explotan en los enemigos cercanos, causando daño masivo en área
 */
public class PumpkinHammerEvent implements Listener {

  private static final int ITEM_ID = 6255;
  private static final long COOLDOWN = 10 * 60 * 1000; // 10 minutos
  private static final double RADIO_EXPLOSION = 7.0;

  private final Map<UUID, Long> cooldowns = new HashMap<>();
  private final Set<Location> calabazasTemporales = new HashSet<>();

  @EventHandler
  public void onRightClick(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR &&
      event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player jugador = event.getPlayer();
    ItemStack item = jugador.getItemInHand();

    if (item == null || item.getTypeId() != ITEM_ID) return;

    UUID jugadorId = jugador.getUniqueId();
    long ahora = System.currentTimeMillis();

    if (cooldowns.containsKey(jugadorId)) {
      long restante = COOLDOWN - (ahora - cooldowns.get(jugadorId));
      if (restante > 0) {
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram(jugador, String.format("§6🎃 Martillo recargando: %02d:%02d", minutos, segundos));
        return;
      }
    }

    try {
      if (!hasPermanent(item)) {
        ItemStack updated = CmdItemUsage.hasUses(item, jugador);
        if (updated == null) {
          jugador.setItemInHand(null);
          return;
        } else {
          jugador.setItemInHand(updated);
        }
        jugador.updateInventory();
      }
    } catch (Exception ignored) {
    }

    cooldowns.put(jugadorId, ahora);
    activarOndaCalabaza(jugador);
  }

  private void activarOndaCalabaza(Player jugador) {
    Location centro = jugador.getLocation();

    jugador.getWorld().playSound(centro, Sound.EXPLODE, 2f, 0.5f);
    spawnHologram(jugador, "§6🎃 §e¡ONDA EXPLOSIVA DE CALABAZAS!");

    // Crear efecto de onda expansiva con calabazas
    new BukkitRunnable() {
      int radio = 0;
      final int radioMaximo = 7;

      @Override
      public void run() {
        if (radio > radioMaximo) {
          this.cancel();
          return;
        }

        // Crear anillo de calabazas
        for (int angulo = 0; angulo < 360; angulo += 30) {
          double radianes = Math.toRadians(angulo);
          int x = (int) (radio * Math.cos(radianes));
          int z = (int) (radio * Math.sin(radianes));

          Location locCalabaza = centro.clone().add(x, 0, z);
          Block bloque = locCalabaza.getBlock();

          // Colocar calabaza temporal
          if (bloque.getType() == Material.AIR || bloque.isLiquid()) {
            bloque.setType(Material.PUMPKIN);
            calabazasTemporales.add(locCalabaza);

            // Programar explosión de calabaza
            Location finalLoc = locCalabaza.clone();
            Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
              if (finalLoc.getBlock().getType() == Material.PUMPKIN) {
                finalLoc.getBlock().setType(Material.AIR);
                calabazasTemporales.remove(finalLoc);

                // Explosión visual
                finalLoc.getWorld().createExplosion(finalLoc, 0F, false);
                finalLoc.getWorld().playSound(finalLoc, Sound.CHICKEN_EGG_POP, 1f, 0.5f);

                mostrarParticulas(jugador, "flame",
                  (float) finalLoc.getX(), (float) finalLoc.getY() + 0.5f, (float) finalLoc.getZ(),
                  0.5f, 0.5f, 0.5f, 0.1f, 20);
              }
            }, 20L);
          }

          // Sonido de crecimiento
          if (radio % 2 == 0) {
            locCalabaza.getWorld().playSound(locCalabaza, Sound.DIG_GRASS, 0.5f, 1.5f);
          }
        }

        radio++;
      }
    }.runTaskTimer(Main.instance, 0L, 3L);

    // Daño a enemigos en el área después de un delay
    Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
      List<Player> victimas = new ArrayList<>();

      for (Entity entidad : jugador.getNearbyEntities(RADIO_EXPLOSION, RADIO_EXPLOSION, RADIO_EXPLOSION)) {
        if (entidad instanceof Player && !entidad.equals(jugador)) {
          victimas.add((Player) entidad);
        }
      }

      if (!victimas.isEmpty()) {
        for (Player victima : victimas) {
          aplicarExplosionCalabaza(jugador, victima);
        }
      }
    }, 25L);
  }

  private void aplicarExplosionCalabaza(Player atacante, Player victima) {
    IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());

    // Daño basado en HP
    int dañoBase = (int) (dbcVictima.getHP() * 0.15);

    // Daño adicional basado en distancia (más cerca = más daño)
    double distancia = atacante.getLocation().distance(victima.getLocation());
    double multiplicador = 1.0 + (RADIO_EXPLOSION - distancia) / RADIO_EXPLOSION * 0.5;
    int dañoTotal = (int) (dañoBase * multiplicador);

    dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - dañoTotal));

    // Reducir stamina también
    int staminaPerdida = (int) (dbcVictima.getStamina() * 0.20);
    dbcVictima.setStamina(Math.max(0, dbcVictima.getStamina() - staminaPerdida));

    // Efectos en la víctima
    Location locVictima = victima.getLocation();
    locVictima.getWorld().createExplosion(locVictima, 0F, false);
    locVictima.getWorld().playSound(locVictima, Sound.EXPLODE, 1.5f, 0.8f);

    // Calabaza en la cabeza como efecto visual
    ItemStack calabazaCabeza = new ItemStack(Material.PUMPKIN, 1);
    victima.getInventory().setHelmet(calabazaCabeza);

    Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
      if (victima.isOnline() && victima.getInventory().getHelmet() != null &&
        victima.getInventory().getHelmet().getType() == Material.PUMPKIN) {
        victima.getInventory().setHelmet(null);
      }
    }, 40L);

    mostrarParticulas(victima, "flame",
      (float) locVictima.getX(), (float) locVictima.getY() + 1, (float) locVictima.getZ(),
      0.5f, 1f, 0.5f, 0.1f, 30);

    spawnHologram(victima, "§6💥 §c¡Calabaza explosiva! -" + dañoTotal + " HP");

    // Lanzar al aire
    victima.setVelocity(victima.getLocation().getDirection().multiply(-0.5).setY(0.8));
  }

  private void mostrarParticulas(Player jugador, String tipo, float x, float y, float z,
                                 float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad) {
    PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
      tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
    );
    ((CraftPlayer) jugador).getHandle().playerConnection.sendPacket(packet);
  }
}
