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
 * Daga Vampírica
 * Habilidad: Cada golpe drena vida y otorga regeneración. Cada 5 golpes acumulados,
 * activa "Sed de Sangre" que aumenta velocidad y fuerza por 10 segundos
 */
public class VampireDaggerEvent implements Listener {

  private static final int ITEM_ID = 4218;
  private static final long COOLDOWN_SED = 8 * 60 * 1000; // 8 minutos para Sed de Sangre

  private final Map<UUID, Integer> golpesAcumulados = new HashMap<> ( );
  private final Map<UUID, Long> cooldownsSed = new HashMap<> ( );
  private final Set<UUID> enSedDeSangre = new HashSet<> ( );

  @EventHandler
  public void onHit ( EntityDamageByEntityEvent event ) {
    if (!(event.getDamager ( ) instanceof Player)) return;
    if (!(event.getEntity ( ) instanceof Player)) return;

    Player vampiro = (Player) event.getDamager ( );
    Player victima = (Player) event.getEntity ( );

    ItemStack arma = vampiro.getItemInHand ( );
    if (arma == null || arma.getTypeId ( ) != ITEM_ID) return;

    try {
      ItemStack item = vampiro.getItemInHand ( );
      if (!hasPermanent ( item )) {
        ItemStack updated = CmdItemUsage.hasUses ( item, vampiro );
        if (updated == null) {
          vampiro.setItemInHand ( null );
          return;
        } else {
          vampiro.setItemInHand ( updated );
        }
        vampiro.updateInventory ( );
      }
    } catch (Exception ignored) {
    }

    UUID vampiroId = vampiro.getUniqueId ( );
    IDBCPlayer dbcVictima = General.getDBCPlayer ( victima.getName ( ) );
    IDBCPlayer dbcVampiro = General.getDBCPlayer ( vampiro.getName ( ) );

    // Drenar vida (3% de HP de la víctima)
    int vidaDrenada = (int) (dbcVictima.getHP ( ) * 0.03);
    dbcVictima.setHP ( Math.max ( 1, dbcVictima.getHP ( ) - vidaDrenada ) );
    dbcVampiro.setHP ( dbcVampiro.getHP ( ) + vidaDrenada );

    // Efectos vampíricos
    vampiro.addPotionEffect ( new PotionEffect ( PotionEffectType.REGENERATION, 20 * 3, 0 ) );
    victima.getWorld ( ).playSound ( victima.getLocation ( ), Sound.DRINK, 1f, 0.8f );

    // Acumular golpes para Sed de Sangre
    int golpes = golpesAcumulados.getOrDefault ( vampiroId, 0 ) + 1;
    golpesAcumulados.put ( vampiroId, golpes );

    if (golpes >= 5) {
      long ahora = System.currentTimeMillis ( );

      if (!cooldownsSed.containsKey ( vampiroId ) ||
        (ahora - cooldownsSed.get ( vampiroId )) >= COOLDOWN_SED) {

        activarSedDeSangre ( vampiro, vampiroId );
        golpesAcumulados.put ( vampiroId, 0 );
        cooldownsSed.put ( vampiroId, ahora );
      } else {
        long restante = COOLDOWN_SED - (ahora - cooldownsSed.get ( vampiroId ));
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram ( vampiro, String.format ( "§c🦇 Sed de Sangre: %02d:%02d", minutos, segundos ) );
        golpesAcumulados.put ( vampiroId, 0 );
      }
    } else {
      spawnHologram ( vampiro, "§4🩸 Sangre drenada: " + golpes + "/5" );
    }
  }

  private void activarSedDeSangre ( Player vampiro, UUID vampiroId ) {
    enSedDeSangre.add ( vampiroId );
    IDBCPlayer dbc = General.getDBCPlayer ( vampiro.getName ( ) );

    vampiro.addPotionEffect ( new PotionEffect ( PotionEffectType.SPEED, 20 * 10, 2 ) );
    vampiro.addPotionEffect ( new PotionEffect ( PotionEffectType.INCREASE_DAMAGE, 20 * 10, 1 ) );
    vampiro.addPotionEffect ( new PotionEffect ( PotionEffectType.NIGHT_VISION, 20 * 10, 0 ) );

    vampiro.getWorld ( ).playSound ( vampiro.getLocation ( ), Sound.BAT_TAKEOFF, 1f, 0.5f );
    spawnHologram ( vampiro, "§c🦇 §4¡SED DE SANGRE ACTIVADA!" );

    // Boost temporal de stats
    dbc.addBonusAttribute ( "Strength", "VampireBloodlust", "*", 1.15, false );

    new BukkitRunnable ( ) {
      @Override
      public void run () {
        if (vampiro.isOnline ( )) {
          dbc.removeBonusAttribute ( "Strength", "VampireBloodlust" );
          spawnHologram ( vampiro, "§7La sed de sangre se desvanece..." );
        }
        enSedDeSangre.remove ( vampiroId );
      }
    }.runTaskLater ( Main.instance, 20L * 10 );

    new BukkitRunnable ( ) {
      @Override
      public void run () {
        if (vampiro.isOnline ( )) {
          dbc.setHP ( (int) (dbc.getHP ( ) + (dbc.getHP ( ) * 0.02)) );
          spawnHologram ( vampiro, "§7La sed de sangre se desvanece..." );
        }
        enSedDeSangre.remove ( vampiroId );
      }
    }.runTaskLater ( Main.instance, 20L * 3 );
  }
}
