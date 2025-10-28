package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import net.minecraft.server.v1_7_R4.PacketPlayOutWorldParticles;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

/**
 * Machete de Crystal Lake
 * Habilidad: "Persecución Implacable" - Cada golpe marca a la víctima.
 * El usuario se vuelve más fuerte y rápido cuanto más cerca está de su víctima marcada.
 * Golpe crítico devastador si la víctima intenta huir.
 */
public class JasonMacheteEvent implements Listener {

  private static final int ITEM_ID = 6237;
  private static final double PROBABILIDAD_MARCA = 0.40;
  private static final long COOLDOWN = 8 * 60 * 1000; // 8 minutos
  private static final double DISTANCIA_CRITICO = 15.0;

  private final Map<UUID, UUID> victimasMarcadas = new HashMap<>(); // Atacante -> Víctima
  private final Map<UUID, Long> cooldowns = new HashMap<>();
  private final Map<UUID, Double> distanciasAnteriores = new HashMap<>();
  private final Set<UUID> enPersecucion = new HashSet<>();

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player)) return;
    if (!(event.getEntity() instanceof Player)) return;

    Player jason = (Player) event.getDamager();
    Player victima = (Player) event.getEntity();

    ItemStack arma = jason.getItemInHand();
    if (arma == null || arma.getTypeId() != ITEM_ID) return;

    UUID jasonId = jason.getUniqueId();
    UUID victimaId = victima.getUniqueId();

    // Verificar si la víctima está marcada y huyendo
    if (victimasMarcadas.get(jasonId) != null &&
      victimasMarcadas.get(jasonId).equals(victimaId)) {

      double distanciaActual = jason.getLocation().distance(victima.getLocation());
      Double distanciaAnterior = distanciasAnteriores.get(victimaId);

      // Si la víctima está huyendo (aumentando distancia)
      if (distanciaAnterior != null && distanciaActual > distanciaAnterior + 2) {
        aplicarGolpeCritico(jason, victima);
        return;
      }
    }

    // Marcar nueva víctima
    if (Math.random() <= PROBABILIDAD_MARCA) {
      long ahora = System.currentTimeMillis();

      if (!cooldowns.containsKey(jasonId) ||
        (ahora - cooldowns.get(jasonId)) >= COOLDOWN) {

        try {
          ItemStack item = jason.getItemInHand();
          if (!hasPermanent(item)) {
            ItemStack updated = CmdItemUsage.hasUses(item, jason);
            if (updated == null) {
              jason.setItemInHand(null);
              return;
            } else {
              jason.setItemInHand(updated);
            }
            jason.updateInventory();
          }
        } catch (Exception ignored) {
        }

        marcarVictima(jason, victima, jasonId, victimaId);
        cooldowns.put(jasonId, ahora);
      } else {
        long restante = COOLDOWN - (ahora - cooldowns.get(jasonId));
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram(jason, String.format("§4🔪 Persecución: %02d:%02d", minutos, segundos));
      }
    }

    // Daño base aumentado
    IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());
    int danoExtra = (int) (dbcVictima.getHP() * 0.05);
    dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - danoExtra));
  }

  private void marcarVictima(Player jason, Player victima, UUID jasonId, UUID victimaId) {
    victimasMarcadas.put(jasonId, victimaId);
    enPersecucion.add(jasonId);
    distanciasAnteriores.put(victimaId, jason.getLocation().distance(victima.getLocation()));

    victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 0));
    victima.getWorld().playSound(victima.getLocation(), Sound.AMBIENCE_CAVE, 1.5f, 0.5f);

    spawnHologram(victima, "§4🔪 §c¡Has sido marcado por Jason!");
    spawnHologram(jason, "§c⚠ §4¡Víctima marcada! Persecución implacable activada.");

    IDBCPlayer dbcJason = General.getDBCPlayer(jason.getName());

    // Efecto de persecución durante 30 segundos
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks >= 30 || !jason.isOnline() || !victima.isOnline() ||
          jason.isDead() || victima.isDead()) {

          finalizarPersecucion(jason, victima, jasonId, victimaId);
          this.cancel();
          return;
        }

        double distancia = jason.getLocation().distance(victima.getLocation());
        distanciasAnteriores.put(victimaId, distancia);

        // Bonos según proximidad
        if (distancia <= 5) {
          // Muy cerca: boost máximo
          dbcJason.addBonusAttribute("Strength", "JasonHunt", "*", 1.25, false);
          jason.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 2));
          jason.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 25, 1));
        } else if (distancia <= 10) {
          // Cerca: boost moderado
          dbcJason.addBonusAttribute("Strength", "JasonHunt", "*", 1.15, false);
          jason.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 1));
        } else if (distancia <= DISTANCIA_CRITICO) {
          // Media distancia: boost mínimo
          dbcJason.addBonusAttribute("Strength", "JasonHunt", "*", 1.08, false);
          jason.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 0));
        }

        // Efectos visuales
        if (ticks % 3 == 0) {
          Location loc = victima.getLocation();
          mostrarParticulas(jason, "reddust",
            (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
            0.3f, 0.5f, 0.3f, 0.01f, 10);

          victima.getWorld().playSound(victima.getLocation(), Sound.NOTE_BASS, 0.5f, 0.5f);
        }

        ticks++;
      }
    }.runTaskTimer(Main.instance, 0L, 20L);
  }

  private void aplicarGolpeCritico(Player jason, Player victima) {
    IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());
    IDBCPlayer dbcJason = General.getDBCPlayer(jason.getName());

    // Daño crítico devastador
    int danoCritico = (int) (dbcVictima.getHP() * 0.35);
    int danoStamina = (int) (dbcVictima.getStamina() * 0.40);

    dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - danoCritico));
    dbcVictima.setStamina(Math.max(0, dbcVictima.getStamina() - danoStamina));

    // Efectos de impacto
    victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 4));
    victima.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 3, 0));

    Location loc = victima.getLocation();
    loc.getWorld().playSound(loc, Sound.ZOMBIE_WOODBREAK, 1.5f, 0.5f);
    loc.getWorld().playSound(loc, Sound.HURT_FLESH, 1.5f, 0.5f);
    loc.getWorld().createExplosion(loc, 0F, false);

    mostrarParticulas(jason, "reddust",
      (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
      0.5f, 1f, 0.5f, 0.1f, 40);

    spawnHologram(victima, "§4💀 §c¡GOLPE CRÍTICO DE JASON! §4-" + danoCritico + " HP");
    spawnHologram(jason, "§c🔪 §4¡NO PUEDES ESCAPAR!");

    // Knock back
    victima.setVelocity(victima.getLocation().getDirection().multiply(-1.5).setY(0.5));
  }

  private void finalizarPersecucion(Player jason, Player victima, UUID jasonId, UUID victimaId) {
    victimasMarcadas.remove(jasonId);
    enPersecucion.remove(jasonId);
    distanciasAnteriores.remove(victimaId);

    if (jason.isOnline()) {
      IDBCPlayer dbcJason = General.getDBCPlayer(jason.getName());
      dbcJason.removeBonusAttribute("Strength", "JasonHunt");
      spawnHologram(jason, "§7La persecución ha terminado...");
    }

    if (victima.isOnline()) {
      spawnHologram(victima, "§7Ya no estás marcado.");
    }
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    // Actualizar distancias de víctimas marcadas
    for (Map.Entry<UUID, UUID> entry : victimasMarcadas.entrySet()) {
      if (entry.getValue().equals(playerId)) {
        Player jason = Bukkit.getPlayer(entry.getKey());
        if (jason != null && jason.isOnline()) {
          double distancia = jason.getLocation().distance(player.getLocation());
          distanciasAnteriores.put(playerId, distancia);
        }
      }
    }
  }

  private void mostrarParticulas(Player jugador, String tipo, float x, float y, float z,
                                 float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad) {
    PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
      tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
    );
    ((CraftPlayer) jugador).getHandle().playerConnection.sendPacket(packet);
  }
}
