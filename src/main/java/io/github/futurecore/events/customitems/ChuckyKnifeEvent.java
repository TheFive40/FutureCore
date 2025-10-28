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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

/**
 * Cuchillo de Chucky
 * Habilidad: "Juego del Muñeco Diabólico"
 * - Golpes rápidos y letales que acumulan "Malicia"
 * - Click derecho: Teleportación sorpresa detrás del enemigo más cercano
 * - Cada 5 golpes: "Risa Maldita" que aturde y causa terror en área
 */
public class ChuckyKnifeEvent implements Listener {

  private static final int ITEM_ID = 6256;
  private static final long COOLDOWN_TELEPORT = 20 * 1000; // 20 segundos
  private static final long COOLDOWN_RISA = 5 * 60 * 1000; // 5 minutos
  private static final double RADIO_RISA = 6.0;

  private final Map<UUID, Integer> maliciaAcumulada = new HashMap<>();
  private final Map<UUID, Long> cooldownsTeleport = new HashMap<>();
  private final Map<UUID, Long> cooldownsRisa = new HashMap<>();
  private final Set<UUID> enRisaMaldita = new HashSet<>();

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player)) return;
    if (!(event.getEntity() instanceof Player)) return;

    Player chucky = (Player) event.getDamager();
    Player victima = (Player) event.getEntity();

    ItemStack arma = chucky.getItemInHand();
    if (arma == null || arma.getTypeId() != ITEM_ID) return;

    UUID chuckyId = chucky.getUniqueId();
    IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());

    // Daño rápido y letal
    int danoBase = (int) (dbcVictima.getHP() * 0.07);
    dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - danoBase));

    // Acumular malicia
    int malicia = maliciaAcumulada.getOrDefault(chuckyId, 0) + 1;
    maliciaAcumulada.put(chuckyId, malicia);

    // Efectos de cuchillo
    victima.getWorld().playSound(victima.getLocation(), Sound.HURT_FLESH, 1f, 1.2f);

    Location loc = victima.getLocation();
    mostrarParticulas(chucky, "reddust",
      (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
      0.2f, 0.3f, 0.2f, 0.01f, 8);

    // Cada golpe tiene probabilidad de causar sangrado
    if (Math.random() <= 0.35) {
      victima.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 4, 0));
    }

    spawnHologram(chucky, "§c🔪 Malicia: " + malicia + "/5");

    // Activar Risa Maldita cada 5 golpes
    if (malicia >= 5) {
      long ahora = System.currentTimeMillis();

      if (!cooldownsRisa.containsKey(chuckyId) ||
        (ahora - cooldownsRisa.get(chuckyId)) >= COOLDOWN_RISA) {

        try {
          ItemStack item = chucky.getItemInHand();
          if (!hasPermanent(item)) {
            ItemStack updated = CmdItemUsage.hasUses(item, chucky);
            if (updated == null) {
              chucky.setItemInHand(null);
              return;
            } else {
              chucky.setItemInHand(updated);
            }
            chucky.updateInventory();
          }
        } catch (Exception ignored) {
        }

        activarRisaMaldita(chucky, chuckyId);
        maliciaAcumulada.put(chuckyId, 0);
        cooldownsRisa.put(chuckyId, ahora);
      } else {
        long restante = COOLDOWN_RISA - (ahora - cooldownsRisa.get(chuckyId));
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram(chucky, String.format("§c😈 Risa Maldita: %02d:%02d", minutos, segundos));
        maliciaAcumulada.put(chuckyId, 0);
      }
    }
  }

  @EventHandler
  public void onRightClick(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR &&
      event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player chucky = event.getPlayer();
    ItemStack item = chucky.getItemInHand();

    if (item == null || item.getTypeId() != ITEM_ID) return;

    UUID chuckyId = chucky.getUniqueId();
    long ahora = System.currentTimeMillis();

    if (cooldownsTeleport.containsKey(chuckyId)) {
      long restante = COOLDOWN_TELEPORT - (ahora - cooldownsTeleport.get(chuckyId));
      if (restante > 0) {
        int segundos = (int) (restante / 1000);
        spawnHologram(chucky, "§c🎭 Teleport: " + segundos + "s");
        return;
      }
    }

    // Buscar enemigo más cercano en el mismo mundo
    Player enemigoCercano = null;
    double distanciaMinima = 15.0;

    for (Player jugador : Main.instance.getServer ().getOnlinePlayers ()) {
      if (jugador.equals(chucky) || jugador.isDead()) continue;

      // Verificar que estén en el mismo mundo
      if (!jugador.getWorld().equals(chucky.getWorld())) continue;

      double distancia = chucky.getLocation().distance(jugador.getLocation());
      if (distancia < distanciaMinima) {
        distanciaMinima = distancia;
        enemigoCercano = jugador;
      }
    }

    if (enemigoCercano == null) {
      spawnHologram(chucky, "§cNo hay enemigos cerca para jugar...");
      return;
    }

    try {
      if (!hasPermanent(item)) {
        ItemStack updated = CmdItemUsage.hasUses(item, chucky);
        if (updated == null) {
          chucky.setItemInHand(null);
          return;
        } else {
          chucky.setItemInHand(updated);
        }
        chucky.updateInventory();
      }
    } catch (Exception ignored) {
    }

    cooldownsTeleport.put(chuckyId, ahora);
    teleportarDetras(chucky, enemigoCercano);
  }

  private void teleportarDetras(Player chucky, Player enemigo) {
    // Calcular posición detrás del enemigo
    Vector direccion = enemigo.getLocation().getDirection().normalize();
    Location detras = enemigo.getLocation().subtract(direccion.multiply(2));
    detras.setYaw(enemigo.getLocation().getYaw());
    detras.setPitch(enemigo.getLocation().getPitch());

    // Efectos antes de teleportar
    Location origenLoc = chucky.getLocation();
    origenLoc.getWorld().playSound(origenLoc, Sound.ENDERMAN_TELEPORT, 1f, 0.5f);
    mostrarParticulas(chucky, "portal",
      (float) origenLoc.getX(), (float) origenLoc.getY() + 1, (float) origenLoc.getZ(),
      0.5f, 1f, 0.5f, 0.1f, 20);

    // Teleportar
    chucky.teleport(detras);

    // Efectos después de teleportar
    chucky.getWorld().playSound(chucky.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, 1.5f);
    chucky.getWorld().playSound(chucky.getLocation(), Sound.VILLAGER_NO, 1f, 0.5f);
    mostrarParticulas(chucky, "portal",
      (float) detras.getX(), (float) detras.getY() + 1, (float) detras.getZ(),
      0.5f, 1f, 0.5f, 0.1f, 20);

    // Golpe sorpresa
    IDBCPlayer dbcEnemigo = General.getDBCPlayer(enemigo.getName());
    int danoSorpresa = (int) (dbcEnemigo.getHP() * 0.12);
    dbcEnemigo.setHP(Math.max(1, dbcEnemigo.getHP() - danoSorpresa));

    enemigo.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 2));
    enemigo.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 3, 1));

    spawnHologram(enemigo, "§c😈 §4¡Chucky apareció detrás de ti!");
    spawnHologram(chucky, "§c🎭 §4¡Sorpresa! -" + danoSorpresa + " HP");
  }

  private void activarRisaMaldita(Player chucky, UUID chuckyId) {
    enRisaMaldita.add(chuckyId);

    // Risa terrorífica
    chucky.getWorld().playSound(chucky.getLocation(), Sound.GHAST_SCREAM, 2f, 0.3f);
    chucky.getWorld().playSound(chucky.getLocation(), Sound.VILLAGER_NO, 1.5f, 0.5f);

    spawnHologram(chucky, "§c😈 §4¡JAJAJA! ¡ES HORA DE JUGAR!");

    List<Player> victimas = new ArrayList<>();

    for (Player jugador : Main.instance.getServer ().getOnlinePlayers ()) {
      if (jugador.equals(chucky) || jugador.isDead()) continue;

      // Verificar que estén en el mismo mundo
      if (!jugador.getWorld().equals(chucky.getWorld())) continue;

      double distancia = chucky.getLocation().distance(jugador.getLocation());
      if (distancia <= RADIO_RISA) {
        victimas.add(jugador);
      }
    }

    if (victimas.isEmpty()) return;

    // Efectos en todas las víctimas
    for (Player victima : victimas) {
      IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());

      // Daño y efectos de terror
      int danoTerror = (int) (dbcVictima.getHP() * 0.15);
      int danoStamina = (int) (dbcVictima.getStamina() * 0.25);

      dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - danoTerror));
      dbcVictima.setStamina(Math.max(0, dbcVictima.getStamina() - danoStamina));

      // Efectos de miedo
      victima.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 0));
      victima.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 8, 1));
      victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 6, 2));
      victima.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 6, 1));

      Location loc = victima.getLocation();
      victima.getWorld().createExplosion(loc, 0F, false);

      mostrarParticulas(chucky, "witchMagic",
        (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
        0.5f, 1f, 0.5f, 0.1f, 30);

      spawnHologram(victima, "§c😈 §4¡La risa de Chucky te aterroriza!");
    }

    // Efecto visual continuo
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks >= 8) {
          enRisaMaldita.remove(chuckyId);
          this.cancel();
          return;
        }

        for (Player victima : victimas) {
          if (victima.isOnline() && !victima.isDead()) {
            victima.getWorld().playSound(victima.getLocation(),
              Sound.VILLAGER_NO, 0.5f, 0.5f);
          }
        }

        ticks++;
      }
    }.runTaskTimer(Main.instance, 0L, 20L);
  }

  private void mostrarParticulas(Player jugador, String tipo, float x, float y, float z,
                                 float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad) {
    PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
      tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
    );
    ((CraftPlayer) jugador).getHandle().playerConnection.sendPacket(packet);
  }
}
