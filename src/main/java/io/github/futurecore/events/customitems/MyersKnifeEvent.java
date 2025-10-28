package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
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
 * Cuchillo de Michael Myers
 * Habilidad: "El Espíritu de Haddonfield"
 * - Modo Sigiloso: Cada golpe acumula "Presencia Oscura"
 * - Cada 4 golpes: Activa "Furia Imparable" - inmune a knock-back y control
 * - Golpes críticos instantáneos si la víctima está de espaldas
 * - Regeneración lenta pero constante basada en golpes
 */
public class MyersKnifeEvent implements Listener {

  private static final int ITEM_ID = 6243;
  private static final long COOLDOWN_FURIA = 6 * 60 * 1000; // 6 minutos
  private static final double PROBABILIDAD_CRITICO = 0.25;

  private final Map<UUID, Integer> presenciaOscura = new HashMap<> ( );
  private final Map<UUID, Long> cooldownsFuria = new HashMap<> ( );
  private final Set<UUID> enFuria = new HashSet<> ( );
  private final Map<UUID, Integer> golpesTotales = new HashMap<> ( );

  @EventHandler
  public void onHit ( EntityDamageByEntityEvent event ) {
    if (!(event.getDamager ( ) instanceof Player)) return;
    if (!(event.getEntity ( ) instanceof Player)) return;

    Player myers = (Player) event.getDamager ( );
    Player victima = (Player) event.getEntity ( );

    ItemStack arma = myers.getItemInHand ( );
    if (arma == null || arma.getTypeId ( ) != ITEM_ID) return;

    UUID myersId = myers.getUniqueId ( );
    IDBCPlayer dbcVictima = General.getDBCPlayer ( victima.getName ( ) );
    IDBCPlayer dbcMyers = General.getDBCPlayer ( myers.getName ( ) );

    // Verificar si es ataque por la espalda
    boolean esporEspalda = verificarAtaqueEspalda ( myers, victima );

    int danoBase = (int) (dbcVictima.getHP ( ) * 0.08);

    // Si está en furia, más daño
    if (enFuria.contains ( myersId )) {
      danoBase = (int) (danoBase * 1.4);
    }

    // Golpe crítico por la espalda
    if (esporEspalda && Math.random ( ) <= PROBABILIDAD_CRITICO) {
      aplicarGolpeCritico ( myers, victima, myersId );
      return;
    }

    dbcVictima.setHP ( Math.max ( 1, dbcVictima.getHP ( ) - danoBase ) );

    // Sonido característico
    myers.getWorld ( ).playSound ( myers.getLocation ( ), Sound.HURT_FLESH, 1f, 0.6f );

    // Acumular presencia oscura
    int presencia = presenciaOscura.getOrDefault ( myersId, 0 ) + 1;
    presenciaOscura.put ( myersId, presencia );

    // Acumular golpes totales para regeneración
    int golpes = golpesTotales.getOrDefault ( myersId, 0 ) + 1;
    golpesTotales.put ( myersId, golpes );

    // Regeneración lenta por golpe
    int regen = 25 + (golpes * 2); // Aumenta con cada golpe
    dbcMyers.setHP ( Math.min ( dbcMyers.getBody ( ), dbcMyers.getHP ( ) + regen ) );

    spawnHologram ( myers, "§8👤 Presencia: " + presencia + "/4 §7[+" + regen + " HP]" );

    // Activar Furia Imparable cada 4 golpes
    if (presencia >= 4) {
      long ahora = System.currentTimeMillis ( );

      if (!cooldownsFuria.containsKey ( myersId ) ||
        (ahora - cooldownsFuria.get ( myersId )) >= COOLDOWN_FURIA) {

        try {
          ItemStack item = myers.getItemInHand ( );
          if (!hasPermanent ( item )) {
            ItemStack updated = CmdItemUsage.hasUses ( item, myers );
            if (updated == null) {
              myers.setItemInHand ( null );
              return;
            } else {
              myers.setItemInHand ( updated );
            }
            myers.updateInventory ( );
          }
        } catch (Exception ignored) {
        }

        activarFuriaImparable ( myers, myersId );
        presenciaOscura.put ( myersId, 0 );
        cooldownsFuria.put ( myersId, ahora );
      } else {
        long restante = COOLDOWN_FURIA - (ahora - cooldownsFuria.get ( myersId ));
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram ( myers, String.format ( "§8⚡ Furia Imparable: %02d:%02d", minutos, segundos ) );
        presenciaOscura.put ( myersId, 0 );
      }
    }
  }

  private boolean verificarAtaqueEspalda ( Player atacante, Player victima ) {
    // Obtener dirección en la que mira la víctima
    double victimaYaw = victima.getLocation ( ).getYaw ( );

    // Obtener dirección del atacante hacia la víctima
    double angulo = Math.toDegrees ( Math.atan2 (
      victima.getLocation ( ).getZ ( ) - atacante.getLocation ( ).getZ ( ),
      victima.getLocation ( ).getX ( ) - atacante.getLocation ( ).getX ( )
    ) );

    // Normalizar ángulos
    double diferencia = Math.abs ( ((angulo - victimaYaw + 180) % 360) - 180 );

    // Si la diferencia es menor a 60 grados, es por la espalda
    return diferencia <= 60;
  }

  private void aplicarGolpeCritico ( Player myers, Player victima, UUID myersId ) {
    IDBCPlayer dbcVictima = General.getDBCPlayer ( victima.getName ( ) );
    IDBCPlayer dbcMyers = General.getDBCPlayer ( myers.getName ( ) );

    // Daño crítico devastador
    int danoCritico = (int) (dbcVictima.getHP ( ) * 0.30);
    int danoKi = (int) (dbcVictima.getKi ( ) * 0.35);

    dbcVictima.setHP ( dbcVictima.getHP ( ) - danoCritico );
    dbcVictima.setKi ( dbcVictima.getKi ( ) - danoKi );

    // Myers se cura más con crítico
    int curacion = (int) (danoCritico * 0.6);
    dbcMyers.setHP ( dbcMyers.getHP ( ) + curacion );

    // Efectos del golpe crítico
    victima.addPotionEffect ( new PotionEffect ( PotionEffectType.SLOW, 20 * 6, 3 ) );
    victima.addPotionEffect ( new PotionEffect ( PotionEffectType.WEAKNESS, 20 * 6, 2 ) );
    victima.addPotionEffect ( new PotionEffect ( PotionEffectType.CONFUSION, 20 * 4, 1 ) );

    victima.getWorld ( ).playSound ( victima.getLocation ( ), Sound.ZOMBIE_WOODBREAK, 1.5f, 0.5f );
    victima.getWorld ( ).playSound ( victima.getLocation ( ), Sound.AMBIENCE_CAVE, 1f, 0.5f );
    victima.getWorld ( ).createExplosion ( victima.getLocation ( ), 0F, false );

    spawnHologram ( victima, "§8💀 §4¡GOLPE CRÍTICO POR LA ESPALDA!" );
    spawnHologram ( myers, "§8🔪 §4¡Ataque sigiloso! +" + curacion + " HP" );

    // Resetear presencia por crítico
    presenciaOscura.put ( myersId, 0 );
  }

  private void activarFuriaImparable ( Player myers, UUID myersId ) {
    enFuria.add ( myersId );
    IDBCPlayer dbcMyers = General.getDBCPlayer ( myers.getName ( ) );

    // Buffs de la furia
    myers.addPotionEffect ( new PotionEffect ( PotionEffectType.SPEED, 20 * 20, 1 ) );
    myers.addPotionEffect ( new PotionEffect ( PotionEffectType.DAMAGE_RESISTANCE, 20 * 20, 3 ) );
    myers.addPotionEffect ( new PotionEffect ( PotionEffectType.INCREASE_DAMAGE, 20 * 20, 1 ) );
    myers.addPotionEffect ( new PotionEffect ( PotionEffectType.REGENERATION, 20 * 20, 1 ) );

    // Boost de stats
    dbcMyers.addBonusAttribute ( "Strength", "MyersFury", "*", 1.35, false );
    dbcMyers.addBonusAttribute ( "Constitution", "MyersFury", "*", 1.20, false );

    myers.getWorld ( ).playSound ( myers.getLocation ( ), Sound.WITHER_SPAWN, 1f, 0.5f );
    myers.getWorld ( ).playSound ( myers.getLocation ( ), Sound.AMBIENCE_CAVE, 1.5f, 0.5f );

    spawnHologram ( myers, "§8⚡ §4¡FURIA IMPARABLE ACTIVADA!" );

    Bukkit.broadcastMessage ( "§8§l[§4§l!§8§l] §7El espíritu de §8Michael Myers §7despierta..." );

    // Efectos durante la furia
    new BukkitRunnable ( ) {
      int ticks = 0;

      @Override
      public void run () {
        if (ticks >= 20 || !myers.isOnline ( ) || myers.isDead ( )) {
          finalizarFuria ( myers, myersId );
          this.cancel ( );
          return;
        }

        // Efectos visuales cada 2 segundos
        if (ticks % 2 == 0) {
          myers.getWorld ( ).playEffect ( myers.getLocation ( ),
            org.bukkit.Effect.STEP_SOUND, 87 );
          myers.getWorld ( ).playSound ( myers.getLocation ( ),
            Sound.NOTE_BASS, 0.5f, 0.5f );
        }

        // Inmunidad a efectos negativos
        myers.removePotionEffect ( PotionEffectType.SLOW );
        myers.removePotionEffect ( PotionEffectType.WEAKNESS );
        myers.removePotionEffect ( PotionEffectType.POISON );
        myers.removePotionEffect ( PotionEffectType.WITHER );
        myers.removePotionEffect ( PotionEffectType.CONFUSION );
        myers.removePotionEffect ( PotionEffectType.BLINDNESS );

        ticks++;
      }
    }.runTaskTimer ( Main.instance, 0L, 20L );
  }

  private void finalizarFuria ( Player myers, UUID myersId ) {
    enFuria.remove ( myersId );

    if (myers.isOnline ( )) {
      IDBCPlayer dbc = General.getDBCPlayer ( myers.getName ( ) );
      dbc.removeBonusAttribute ( "Strength", "MyersFury" );
      dbc.removeBonusAttribute ( "Constitution", "MyersFury" );

      // Bonus final basado en golpes acumulados
      int golpes = golpesTotales.getOrDefault ( myersId, 0 );
      if (golpes > 0) {
        int bonusFinal = Math.min ( golpes * 30, 1000 ); // Máximo 1000 HP
        dbc.setHP ( Math.min ( dbc.getBody ( ), dbc.getHP ( ) + bonusFinal ) );
        spawnHologram ( myers, "§a+ " + bonusFinal + " HP (Presencia acumulada)" );
      }

      spawnHologram ( myers, "§7El espíritu descansa... por ahora." );
      myers.getWorld ( ).playSound ( myers.getLocation ( ), Sound.AMBIENCE_CAVE, 1f, 1.5f );
    }
  }
}
