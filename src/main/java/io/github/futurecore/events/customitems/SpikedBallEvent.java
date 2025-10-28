package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import net.minecraft.server.v1_7_R4.PacketPlayOutWorldParticles;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

/**
 * Bola de Púas Infernal
 * Habilidad: "Tormento Giratorio"
 * - Golpes causan sangrado acumulativo y reducen defensa
 * - Click derecho: Lanza un giro devastador que golpea a todos en área
 * - Cada golpe aumenta la velocidad de giro (stacks hasta 5)
 * - Al máximo de stacks: "Vórtice de Púas" causa daño masivo en área
 */
public class SpikedBallEvent implements Listener {

  private static final int ITEM_ID = 6253;
  private static final long COOLDOWN_GIRO = 30 * 1000; // 30 segundos
  private static final long COOLDOWN_VORTICE = 8 * 60 * 1000; // 8 minutos
  private static final double RADIO_GIRO = 5.0;
  private static final double RADIO_VORTICE = 7.0;

  private final Map<UUID, Integer> stacksGiro = new HashMap<>();
  private final Map<UUID, Long> cooldownsGiro = new HashMap<>();
  private final Map<UUID, Long> cooldownsVortice = new HashMap<>();
  private final Set<UUID> victimsSangrando = new HashSet<>();
  private final Map<UUID, Integer> nivelSangrado = new HashMap<>();

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player)) return;
    if (!(event.getEntity() instanceof Player)) return;

    Player atacante = (Player) event.getDamager();
    Player victima = (Player) event.getEntity();

    ItemStack arma = atacante.getItemInHand();
    if (arma == null || arma.getTypeId() != ITEM_ID) return;

    UUID atacanteId = atacante.getUniqueId();
    UUID victimaId = victima.getUniqueId();
    IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());

    // Acumular stacks de giro
    int stacks = stacksGiro.getOrDefault(atacanteId, 0);
    if (stacks < 5) {
      stacks++;
      stacksGiro.put(atacanteId, stacks);
    }

    // Daño base aumenta con stacks
    double multiplicadorDano = 1.0 + (stacks * 0.15); // +15% por stack
    int danoBase = (int) (dbcVictima.getHP() * 0.07 * multiplicadorDano);
    dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - danoBase));

    // Sonidos metálicos de impacto
    atacante.getWorld().playSound(atacante.getLocation(), Sound.ANVIL_USE, 1f, 1.5f);
    victima.getWorld().playSound(victima.getLocation(), Sound.HURT_FLESH, 1f, 0.8f);

    // Reducir defensa de la víctima
    dbcVictima.addBonusAttribute("Constitution", "SpikedBall", "*", 0.90, false);

    // Aplicar/aumentar sangrado
    aplicarSangrado(victima, victimaId);

    // Efectos visuales
    Location loc = victima.getLocation();
    mostrarParticulas(atacante, "reddust",
      (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
      0.3f, 0.5f, 0.3f, 0.01f, 10);

    spawnHologram(atacante, "§c⚙ Velocidad de Giro: " + stacks + "/5");

    // Al llegar a 5 stacks, activar Vórtice si está disponible
    if (stacks >= 5) {
      long ahora = System.currentTimeMillis();

      if (!cooldownsVortice.containsKey(atacanteId) ||
        (ahora - cooldownsVortice.get(atacanteId)) >= COOLDOWN_VORTICE) {

        spawnHologram(atacante, "§4⚡ §c¡Vórtice de Púas disponible! (Click derecho)");
      }
    }
  }

  @EventHandler
  public void onRightClick(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR &&
      event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player jugador = event.getPlayer();
    ItemStack item = jugador.getItemInHand();

    if (item == null || item.getTypeId() != ITEM_ID) return;

    UUID jugadorId = jugador.getUniqueId();
    long ahora = System.currentTimeMillis();
    int stacks = stacksGiro.getOrDefault(jugadorId, 0);

    // Si tiene 5 stacks, activar Vórtice
    if (stacks >= 5) {
      if (!cooldownsVortice.containsKey(jugadorId) ||
        (ahora - cooldownsVortice.get(jugadorId)) >= COOLDOWN_VORTICE) {

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

        activarVortice(jugador, jugadorId);
        stacksGiro.put(jugadorId, 0);
        cooldownsVortice.put(jugadorId, ahora);
        return;
      } else {
        long restante = COOLDOWN_VORTICE - (ahora - cooldownsVortice.get(jugadorId));
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram(jugador, String.format("§4⚡ Vórtice: %02d:%02d", minutos, segundos));
        return;
      }
    }

    // Giro normal
    if (cooldownsGiro.containsKey(jugadorId)) {
      long restante = COOLDOWN_GIRO - (ahora - cooldownsGiro.get(jugadorId));
      if (restante > 0) {
        int segundos = (int) (restante / 1000);
        spawnHologram(jugador, "§c⚙ Giro: " + segundos + "s");
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

    cooldownsGiro.put(jugadorId, ahora);
    activarGiro(jugador, jugadorId);
  }

  private void activarGiro(Player jugador, UUID jugadorId) {
    int stacks = stacksGiro.getOrDefault(jugadorId, 0);
    double multiplicador = 1.0 + (stacks * 0.2);

    jugador.getWorld().playSound(jugador.getLocation(), Sound.IRONGOLEM_HIT, 1.5f, 0.8f);
    spawnHologram(jugador, "§c⚙ §4¡Tormento Giratorio!");

    // Girar al jugador
    jugador.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 3, 2));

    List<Player> victimas = new ArrayList<>();
    for (Entity entidad : jugador.getNearbyEntities(RADIO_GIRO, RADIO_GIRO, RADIO_GIRO)) {
      if (entidad instanceof Player && !entidad.equals(jugador)) {
        victimas.add((Player) entidad);
      }
    }

    if (victimas.isEmpty()) return;

    // Efectos en área
    new BukkitRunnable() {
      int ticks = 0;
      final int duracion = 3;

      @Override
      public void run() {
        if (ticks >= duracion) {
          this.cancel();
          return;
        }

        for (Player victima : victimas) {
          if (!victima.isOnline() || victima.isDead()) continue;

          IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());
          int dano = (int) (dbcVictima.getHP() * 0.05 * multiplicador);
          dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - dano));

          // Lanzar al aire
          Vector direccion = victima.getLocation().toVector()
            .subtract(jugador.getLocation().toVector())
            .normalize()
            .multiply(0.8)
            .setY(0.5);
          victima.setVelocity(direccion);

          Location loc = victima.getLocation();
          mostrarParticulas(jugador, "crit",
            (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
            0.5f, 0.5f, 0.5f, 0.1f, 15);

          victima.getWorld().playSound(loc, Sound.HURT_FLESH, 1f, 1f);
        }

        // Efectos visuales del giro
        Location loc = jugador.getLocation();
        mostrarParticulas(jugador, "smoke",
          (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
          2f, 1f, 2f, 0.1f, 20);

        jugador.getWorld().playSound(loc, Sound.IRONGOLEM_HIT, 0.8f, 1.5f);

        ticks++;
      }
    }.runTaskTimer(Main.instance, 0L, 10L);
  }

  private void activarVortice(Player jugador, UUID jugadorId) {
    jugador.getWorld().playSound(jugador.getLocation(), Sound.WITHER_SPAWN, 2f, 0.5f);
    spawnHologram(jugador, "§4⚡ §c¡VÓRTICE DE PÚAS!");

    // Buffs masivos
    jugador.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 3));
    jugador.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2));

    IDBCPlayer dbcJugador = General.getDBCPlayer(jugador.getName());
    dbcJugador.addBonusAttribute("Strength", "SpikedVortex", "*", 1.40, false);

    List<Player> victimas = new ArrayList<>();
    for (Entity entidad : jugador.getNearbyEntities(RADIO_VORTICE, RADIO_VORTICE, RADIO_VORTICE)) {
      if (entidad instanceof Player && !entidad.equals(jugador)) {
        victimas.add((Player) entidad);
      }
    }

    // Vórtice devastador durante 10 segundos
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks >= 10 || !jugador.isOnline() || jugador.isDead()) {
          if (jugador.isOnline()) {
            dbcJugador.removeBonusAttribute("Strength", "SpikedVortex");
            spawnHologram(jugador, "§7El vórtice se disipa...");
          }
          this.cancel();
          return;
        }

        for (Player victima : victimas) {
          if (!victima.isOnline() || victima.isDead()) continue;

          IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());

          // Daño masivo
          int dano = (int) (dbcVictima.getHP() * 0.08);
          int danoStamina = (int) (dbcVictima.getStamina() * 0.10);

          dbcVictima.setHP(Math.max(1, dbcVictima.getHP() - dano));
          dbcVictima.setStamina(Math.max(0, dbcVictima.getStamina() - danoStamina));

          // Atraer hacia el centro
          Vector direccion = jugador.getLocation().toVector()
            .subtract(victima.getLocation().toVector())
            .normalize()
            .multiply(0.6);
          victima.setVelocity(direccion);

          // Efectos
          victima.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 25, 3));

          Location loc = victima.getLocation();
          mostrarParticulas(jugador, "reddust",
            (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
            0.5f, 1f, 0.5f, 0.1f, 20);

          if (ticks % 2 == 0) {
            victima.getWorld().playSound(loc, Sound.ANVIL_USE, 1f, 0.5f);
            loc.getWorld().createExplosion(loc, 0F, false);
          }
        }

        // Efectos visuales del vórtice
        Location loc = jugador.getLocation();
        mostrarParticulas(jugador, "smoke",
          (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
          3f, 2f, 3f, 0.15f, 30);
        mostrarParticulas(jugador, "crit",
          (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
          3f, 2f, 3f, 0.15f, 30);

        jugador.getWorld().playSound(loc, Sound.IRONGOLEM_HIT, 1f, 0.3f);

        ticks++;
      }
    }.runTaskTimer(Main.instance, 0L, 20L);
  }

  private void aplicarSangrado(Player victima, UUID victimaId) {
    int nivel = nivelSangrado.getOrDefault(victimaId, 0);
    nivel = Math.min(nivel + 1, 5); // Máximo 5 niveles
    nivelSangrado.put(victimaId, nivel);

    if (!victimsSangrando.contains(victimaId)) {
      victimsSangrando.add(victimaId);
      iniciarSangrado(victima, victimaId);
    }

    spawnHologram(victima, "§4🩸 Sangrado nivel " + nivel);
  }

  private void iniciarSangrado(Player victima, UUID victimaId) {
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks >= 10 || !victima.isOnline() || victima.isDead()) {
          victimsSangrando.remove(victimaId);
          nivelSangrado.remove(victimaId);

          if (victima.isOnline()) {
            IDBCPlayer dbc = General.getDBCPlayer(victima.getName());
            dbc.removeBonusAttribute("Constitution", "SpikedBall");
          }
          this.cancel();
          return;
        }

        int nivel = nivelSangrado.getOrDefault(victimaId, 1);
        IDBCPlayer dbc = General.getDBCPlayer(victima.getName());
        int danoSangrado = (int) (dbc.getHP() * 0.01 * nivel); // 1% por nivel
        dbc.setHP(Math.max(1, dbc.getHP() - danoSangrado));

        Location loc = victima.getLocation();
        mostrarParticulas(victima, "reddust",
          (float) loc.getX(), (float) loc.getY() + 1, (float) loc.getZ(),
          0.2f, 0.3f, 0.2f, 0.01f, 5 * nivel);

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
