package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

/**
 * Guante de Pesadilla
 * Habilidad: Cada golpe tiene probabilidad de causar "Pesadilla Viviente"
 * que drena vida gradualmente mientras causa efectos de terror.
 * Cada 3 golpes activa "Entrada al Sueño" con efectos devastadores.
 */
public class NightmareGloveEvent implements Listener {

  private static final int ITEM_ID = 4281;
  private static final double PROBABILIDAD_PESADILLA = 0.30;
  private static final long COOLDOWN = 7 * 60 * 1000; // 7 minutos

  private final Map<UUID, Integer> golpesAcumulados = new HashMap<>();
  private final Map<UUID, Long> cooldowns = new HashMap<>();
  private final Set<UUID> enPesadilla = new HashSet<>();

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player)) return;
    if (!(event.getEntity() instanceof Player)) return;

    Player atacante = (Player) event.getDamager();
    Player victima = (Player) event.getEntity();

    ItemStack arma = atacante.getItemInHand();
    if (arma == null || arma.getTypeId() != ITEM_ID) return;

    UUID atacanteId = atacante.getUniqueId();

    // Sistema de golpes acumulados
    int golpes = golpesAcumulados.getOrDefault(atacanteId, 0) + 1;
    golpesAcumulados.put(atacanteId, golpes);

    // Efecto de pesadilla básico en cada golpe
    if (Math.random() <= PROBABILIDAD_PESADILLA) {
      try {
        ItemStack item = atacante.getItemInHand();
        if (!hasPermanent(item)) {
          ItemStack updated = CmdItemUsage.hasUses(item, atacante);
          if (updated == null) {
            atacante.setItemInHand(null);
            return;
          } else {
            atacante.setItemInHand(updated);
          }
          atacante.updateInventory();
        }
      } catch (Exception ignored) {
      }

      aplicarPesadilla(victima);
    }

    // Cada 3 golpes: "Entrada al Sueño"
    if (golpes >= 3) {
      long ahora = System.currentTimeMillis();

      if (!cooldowns.containsKey(atacanteId) ||
        (ahora - cooldowns.get(atacanteId)) >= COOLDOWN) {

        activarEntradaSueno(atacante, victima, atacanteId);
        golpesAcumulados.put(atacanteId, 0);
        cooldowns.put(atacanteId, ahora);
      } else {
        long restante = COOLDOWN - (ahora - cooldowns.get(atacanteId));
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram(atacante, String.format("§4😈 Entrada al Sueño: %02d:%02d", minutos, segundos));
        golpesAcumulados.put(atacanteId, 0);
      }
    } else {
      spawnHologram(atacante, "§c🔪 Cortes acumulados: " + golpes + "/3");
    }
  }

  private void aplicarPesadilla(Player victima) {
    if (enPesadilla.contains(victima.getUniqueId())) return;

    enPesadilla.add(victima.getUniqueId());

    // Efectos de terror
    victima.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 6, 1));
    victima.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 3, 0));
    victima.getWorld().playSound(victima.getLocation(), Sound.GHAST_SCREAM, 1.5f, 0.5f);

    spawnHologram(victima, "§4😈 §c¡Entrando en pesadilla!");

    // Drenar vida gradualmente
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks >= 6 || !victima.isOnline() || victima.isDead()) {
          enPesadilla.remove(victima.getUniqueId());
          this.cancel();
          return;
        }

        IDBCPlayer dbc = General.getDBCPlayer(victima.getName());
        int dano = (int) (dbc.getHP() * 0.02); // 2% cada segundo
        dbc.setHP(Math.max(1, dbc.getHP() - dano));

        victima.getWorld().playEffect(victima.getLocation(),
          org.bukkit.Effect.STEP_SOUND, 173); // Chest

        ticks++;
      }
    }.runTaskTimer(Main.instance, 0L, 20L);
  }

  private void activarEntradaSueno(Player atacante, Player victima, UUID atacanteId) {
    IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());
    IDBCPlayer dbcAtacante = General.getDBCPlayer(atacante.getName());

    // Efectos devastadores
    victima.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 1));
    victima.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 10, 2));
    victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 10, 3));
    victima.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 10, 2));

    // Daño masivo
    int danoHP = (int) (dbcVictima.getHP() * 0.25);
    int danoKi = (int) (dbcVictima.getKi() * 0.30);
    int danoStamina = (int) (dbcVictima.getStamina() * 0.35);

    dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - danoHP));
    dbcVictima.setKi(Math.max(0, dbcVictima.getKi() - danoKi));
    dbcVictima.setStamina(Math.max(0, dbcVictima.getStamina() - danoStamina));

    // El atacante se cura
    int curacion = (int) (danoHP * 0.5);
    dbcAtacante.setHP(Math.min(dbcAtacante.getBody(), dbcAtacante.getHP() + curacion));

    // Efectos
    victima.getWorld().playSound(victima.getLocation(), Sound.WITHER_SPAWN, 1.5f, 0.5f);
    victima.getWorld().createExplosion(victima.getLocation(), 0F, false);

    spawnHologram(victima, "§4😈 §c¡BIENVENIDO A TU PESADILLA!");
    spawnHologram(atacante, "§c🔪 §4¡Entrada al Sueño activada! +" + curacion + " HP");

    // Sangrado continuo
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks >= 10 || !victima.isOnline() || victima.isDead()) {
          spawnHologram(victima, "§7Despertas de la pesadilla...");
          this.cancel();
          return;
        }

        IDBCPlayer dbc = General.getDBCPlayer(victima.getName());
        int dano = (int) (dbc.getHP() * 0.015);
        dbc.setHP(Math.max(1, dbc.getHP() - dano));

        if (ticks % 2 == 0) {
          victima.getWorld().playSound(victima.getLocation(), Sound.HURT_FLESH, 0.8f, 0.6f);
        }

        ticks++;
      }
    }.runTaskTimer(Main.instance, 0L, 20L);
  }
}
