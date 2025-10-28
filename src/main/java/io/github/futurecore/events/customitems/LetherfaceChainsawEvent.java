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

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

/**
 * Motosierra de Leatherface
 * Habilidad: Click Derecho activa "Frenesí de la Motosierra" que aumenta daño
 * y velocidad, causando sangrado masivo en área. Cada golpe acumula "Sed de Sangre"
 * que regenera al usuario por cada golpe.
 */
public class LetherfaceChainsawEvent implements Listener {

  private static final int ITEM_ID = 6248;
  private static final long COOLDOWN_FRENESI = 10 * 60 * 1000; // 10 minutos
  private static final double RADIO_FRENESI = 4.0;

  private final Map<UUID, Long> cooldownsFrenesi = new HashMap<> ( );
  private final Map<UUID, Integer> sedDeSangre = new HashMap<> ( );
  private final Set<UUID> enFrenesi = new HashSet<> ( );
  private final Set<UUID> victimsSangrando = new HashSet<> ( );

  @EventHandler
  public void onHit ( EntityDamageByEntityEvent event ) {
    if (!(event.getDamager ( ) instanceof Player)) return;
    if (!(event.getEntity ( ) instanceof Player)) return;

    Player atacante = (Player) event.getDamager ( );
    Player victima = (Player) event.getEntity ( );

    ItemStack arma = atacante.getItemInHand ( );
    if (arma == null || arma.getTypeId ( ) != ITEM_ID) return;

    UUID atacanteId = atacante.getUniqueId ( );
    IDBCPlayer dbcVictima = General.getDBCPlayer ( victima.getName ( ) );
    IDBCPlayer dbcAtacante = General.getDBCPlayer ( atacante.getName ( ) );

    // Daño base de motosierra
    int danoBase = (int) (dbcVictima.getHP ( ) * 0.06);

    // Si está en frenesí, más daño
    if (enFrenesi.contains ( atacanteId )) {
      danoBase = (int) (danoBase * 1.5);
    }

    dbcVictima.setHP (  dbcVictima.getHP ( ) - danoBase );

    // Sonido de motosierra
    atacante.getWorld ( ).playSound ( atacante.getLocation ( ), Sound.IRONGOLEM_HIT, 1f, 0.5f );
    victima.getWorld ( ).playSound ( victima.getLocation ( ), Sound.HURT_FLESH, 1f, 0.8f );

    // Sistema de Sed de Sangre
    int sed = sedDeSangre.getOrDefault ( atacanteId, 0 ) + 1;
    sedDeSangre.put ( atacanteId, sed );

    // Curación por golpe
    int curacion = (int) (danoBase * 0.04);
    dbcAtacante.setHP ( dbcAtacante.getHP ( ) + curacion );
    // Sangrado
    if (!victimsSangrando.contains ( victima.getUniqueId ( ) )) {
      aplicarSangrado ( victima );
    }

    spawnHologram ( atacante, "§c⛓ Sed de Sangre: " + sed + " §4[+" + curacion + " HP]" );
  }

  @EventHandler
  public void onRightClick ( PlayerInteractEvent event ) {
    if (event.getAction ( ) != Action.RIGHT_CLICK_AIR &&
      event.getAction ( ) != Action.RIGHT_CLICK_BLOCK) return;

    Player player = event.getPlayer ( );
    ItemStack item = player.getItemInHand ( );

    if (item == null || item.getTypeId ( ) != ITEM_ID) return;

    UUID playerId = player.getUniqueId ( );
    long ahora = System.currentTimeMillis ( );

    if (cooldownsFrenesi.containsKey ( playerId )) {
      long restante = COOLDOWN_FRENESI - (ahora - cooldownsFrenesi.get ( playerId ));
      if (restante > 0) {
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram ( player, String.format ( "§c⛓ Frenesí: %02d:%02d", minutos, segundos ) );
        return;
      }
    }

    try {
      if (!hasPermanent ( item )) {
        ItemStack updated = CmdItemUsage.hasUses ( item, player );
        if (updated == null) {
          player.setItemInHand ( null );
          return;
        } else {
          player.setItemInHand ( updated );
        }
        player.updateInventory ( );
      }
    } catch (Exception ignored) {
    }

    cooldownsFrenesi.put ( playerId, ahora );
    activarFrenesi ( player, playerId );
  }

  private void activarFrenesi ( Player player, UUID playerId ) {
    enFrenesi.add ( playerId );
    IDBCPlayer dbcPlayer = General.getDBCPlayer ( player.getName ( ) );

    // Buffs de frenesí
    player.addPotionEffect ( new PotionEffect ( PotionEffectType.SPEED, 20 * 15, 2 ) );
    player.addPotionEffect ( new PotionEffect ( PotionEffectType.INCREASE_DAMAGE, 20 * 15, 2 ) );
    player.addPotionEffect ( new PotionEffect ( PotionEffectType.DAMAGE_RESISTANCE, 20 * 15, 1 ) );

    dbcPlayer.addBonusAttribute ( "Strength", "ChainsawFrenzy", "*", 1.30, false );

    player.getWorld ( ).playSound ( player.getLocation ( ), Sound.IRONGOLEM_HIT, 2f, 0.3f );
    spawnHologram ( player, "§c⛓ §4¡FRENESÍ DE LA MOTOSIERRA ACTIVADO!" );

    // Daño en área cada 2 segundos
    new BukkitRunnable ( ) {
      int ticks = 0;

      @Override
      public void run () {
        if (ticks >= 15 || !player.isOnline ( ) || player.isDead ( )) {
          finalizarFrenesi ( player, playerId );
          this.cancel ( );
          return;
        }

        // Sonido continuo de motosierra
        player.getWorld ( ).playSound ( player.getLocation ( ), Sound.IRONGOLEM_HIT, 1f, 0.5f );

        // Daño en área cada 2 segundos
        if (ticks % 2 == 0) {
          for (Entity entidad : player.getNearbyEntities ( RADIO_FRENESI, RADIO_FRENESI, RADIO_FRENESI )) {
            if (entidad instanceof Player && !entidad.equals ( player )) {
              Player victima = (Player) entidad;
              IDBCPlayer dbcVictima = General.getDBCPlayer ( victima.getName ( ) );

              int danoArea = (int) (dbcVictima.getHP ( ) * 0.04);
              dbcVictima.setHP ( Math.max ( 1, dbcVictima.getHP ( ) - danoArea ) );

              Location loc = victima.getLocation ( );
              mostrarParticulas ( player, "reddust",
                (float) loc.getX ( ), (float) loc.getY ( ) + 1, (float) loc.getZ ( ),
                0.5f, 0.5f, 0.5f, 0.1f, 15 );

              victima.getWorld ( ).playSound ( loc, Sound.HURT_FLESH, 0.8f, 1f );
              aplicarSangrado ( victima );
            }
          }
        }

        // Efectos visuales
        Location loc = player.getLocation ( );
        mostrarParticulas ( player, "smoke",
          (float) loc.getX ( ), (float) loc.getY ( ) + 1, (float) loc.getZ ( ),
          0.5f, 0.5f, 0.5f, 0.05f, 10 );

        ticks++;
      }
    }.runTaskTimer ( Main.instance, 0L, 20L );
  }

  private void aplicarSangrado ( Player victima ) {
    UUID victimaId = victima.getUniqueId ( );
    if (victimsSangrando.contains ( victimaId )) return;

    victimsSangrando.add ( victimaId );

    victima.addPotionEffect ( new PotionEffect ( PotionEffectType.SLOW, 20 * 8, 1 ) );
    spawnHologram ( victima, "§4🩸 §c¡Estás sangrando profusamente!" );

    new BukkitRunnable ( ) {
      int ticks = 0;

      @Override
      public void run () {
        if (ticks >= 8 || !victima.isOnline ( ) || victima.isDead ( )) {
          victimsSangrando.remove ( victimaId );
          this.cancel ( );
          return;
        }

        IDBCPlayer dbc = General.getDBCPlayer ( victima.getName ( ) );
        int danoSangrado = (int) (dbc.getHP ( ) * 0.025); // 2.5% por segundo
        dbc.setHP ( Math.max ( 1, dbc.getHP ( ) - danoSangrado ) );

        Location loc = victima.getLocation ( );
        mostrarParticulas ( victima, "reddust",
          (float) loc.getX ( ), (float) loc.getY ( ) + 1, (float) loc.getZ ( ),
          0.3f, 0.3f, 0.3f, 0.01f, 8 );

        ticks++;
      }
    }.runTaskTimer ( Main.instance, 0L, 20L );
  }

  private void finalizarFrenesi ( Player player, UUID playerId ) {
    enFrenesi.remove ( playerId );

    if (player.isOnline ( )) {
      IDBCPlayer dbc = General.getDBCPlayer ( player.getName ( ) );
      dbc.removeBonusAttribute ( "Strength", "ChainsawFrenzy" );

      // Bonus por sed de sangre acumulada
      int sed = sedDeSangre.getOrDefault ( playerId, 0 );
      if (sed > 0) {
        int bonusHP = sed * 50;
        dbc.setHP ( dbc.getHP ( ) + bonusHP );
        spawnHologram ( player, "§a+ " + bonusHP + " HP por Sed de Sangre (" + sed + " golpes)" );
        sedDeSangre.put ( playerId, 0 );
      }

      spawnHologram ( player, "§7El frenesí termina... La motosierra se enfría." );
    }
  }

  private void mostrarParticulas ( Player jugador, String tipo, float x, float y, float z,
                                   float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad ) {
    PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles (
      tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
    );
    ((CraftPlayer) jugador).getHandle ( ).playerConnection.sendPacket ( packet );
  }
}
