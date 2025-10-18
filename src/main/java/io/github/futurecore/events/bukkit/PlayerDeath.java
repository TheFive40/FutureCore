package io.github.futurecore.events.bukkit;

import com.earth2me.essentials.Essentials;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.futurecore.Main;
import io.github.futurecore.utils.General;
import io.github.futurecore.utils.RegionUtils;
import io.github.futurecore.utils.data.KairosData.PKairos;
import io.github.futurecore.utils.handlers.battle.BattleData;
import io.github.futurecore.utils.handlers.battle.BattleTask;
import kamkeel.npcdbc.api.IDBCAddon;
import kamkeel.npcdbc.constants.DBCForm;
import kamkeel.npcdbc.scripted.DBCAPI;
import kamkeel.npcs.addon.DBCAddon;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.delaware.tools.CC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.utils.General.getStatLossByRank;

public class PlayerDeath implements Listener {
  private static final HashMap<UUID, UUID> damageByPlayer = new HashMap<> ( );
  private static final HashMap<UUID, UUID> damageByPlayerFactions = new HashMap<> ( );

  @EventHandler
  public void onDamage ( EntityDamageByEntityEvent event ) {
    if (!(event.getDamager ( ) instanceof Player)) return;
    if (!(event.getEntity ( ) instanceof Player)) return;
    Player damager = (Player) event.getDamager ( );
    Player victim = (Player) event.getEntity ( );

    UUID damagerId = damager.getUniqueId ( );
    UUID victimId = victim.getUniqueId ( );

    if (BattleData.getOpponent ( victimId ) != null || BattleData.getOpponent ( damagerId ) != null) {
      UUID victimOpponent = BattleData.getOpponent ( victimId );
      UUID damagerOpponent = BattleData.getOpponent ( damagerId );

      if (victimOpponent != null && !victimOpponent.equals ( damagerId )) {
        event.setCancelled ( true );
        damager.sendMessage ( "§c¡No puedes interferir en un duelo!" );
        return;
      }
      if (damagerOpponent != null && !damagerOpponent.equals ( victimId )) {
        event.setCancelled ( true );
        damager.sendMessage ( "§c¡No puedes interferir en un duelo!" );
        return;
      }
    }
    if (RegionUtils.getRegionNameAtLocation ( event.getDamager ( ).getLocation ( ) ).equalsIgnoreCase ( "factions" )
      && event.getDamager ( ).getLocation ( ).getWorld ( ).getName ( ).equalsIgnoreCase ( "factions" )) {
      damageByPlayerFactions.put ( event.getEntity ( ).getUniqueId ( ), event.getDamager ( ).getUniqueId ( ) );
    }
    damageByPlayer.put ( event.getEntity ( ).getUniqueId ( ), event.getDamager ( ).getUniqueId ( ) );
  }

  @EventHandler
  public void onDeath ( PlayerDeathEvent event ) {
    Player dead = event.getEntity ( );
    if (RegionUtils.getRegionNameAtLocation ( dead.getLocation ( ) ).equalsIgnoreCase ( "factions" )
      && dead.getLocation ( ).getWorld ( ).getName ( ).equalsIgnoreCase ( "factions" ) &&
      damageByPlayerFactions.containsKey ( dead.getUniqueId ( ) )) {
      UUID opponentUUID = damageByPlayerFactions.get ( dead.getUniqueId ( ) );
      Player opponent = Main.instance.getServer ( ).getPlayer ( opponentUUID );
      IDBCAddon opponentDBC = (IDBCAddon) General.getDBCPlayer ( opponent.getName ( ) );
      int lvldead = General.getLVL ( dead );
      if (lvldead < 1000)
        return;
      if (opponentDBC.getCurrentForm ( ) != null) {
        opponentDBC.setCustomMastery ( opponentDBC.getCurrentForm ( ), 5F );
        opponent.sendMessage ( CC.translate ( "&8[&c&lFactions&8] &aHas ganado &2+5 &aNiveles de &2maestria" ) );
        opponent.playSound ( opponent.getLocation ( ), Sound.ORB_PICKUP, 1.0F, 1.0F );
      } else {
        Main.instance.getServer ( ).dispatchCommand ( Main.instance.getServer ( ).getConsoleSender ( ),
          "jrmcformmastery " + opponent.getName ( ) + " add current 5" );
        opponent.sendMessage ( CC.translate ( "&8[&c&lFactions&8] &aHas ganado &2+5 &aNiveles de &2maestria" ) );
        opponent.playSound ( opponent.getLocation ( ), Sound.ORB_PICKUP, 1.0F, 1.0F );
      }
      dead.sendMessage ( CC.translate ( "&8[&c&lFactions&8] &c¡Pierdes &45 &cNiveles de &4" + General.getHighestStat ( dead ) ) );
      Main.instance.getServer ( ).dispatchCommand ( Main.instance.getServer ( ).getConsoleSender ( ),
        "jrmca add " + General.getHighestStat ( dead ) + " -5 " + dead.getName ( ) );
      opponent.playSound ( opponent.getLocation ( ), Sound.ANVIL_BREAK, 1.0F, 1.0F );

    }
    UUID deadId = dead.getUniqueId ( );
    UUID opponentId = BattleData.getOpponent ( deadId );
    if (opponentId != null) {
      Player winner = Bukkit.getPlayer ( opponentId );
      if (winner != null && winner.isOnline ( )) {
        // Finaliza el combate
        BattleData.endBattle ( deadId, opponentId );
        BattleTask.cancelBattleTimer ( deadId, opponentId );
        //Recompensa para el ganador
        winner.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &a¡Has ganado el duelo contra &c" + dead.getName ( ) + "&a!" ) );
        awaitingStatChoice.put ( winner.getUniqueId ( ), true );
        winner.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &eEscribe en el chat el nombre de la estadística que quieres mejorar:" ) );
        winner.sendMessage ( CC.translate ( "&7Opciones: &aSTR, DEX, CON, WIL, MND, SPI" ) );
        //Castigo para el perdedor
        String stat = General.getHighestStat ( dead );
        Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "jrmca add " + stat + " " + (-1 * (getStatLossByRank ( dead ))) + " " + dead.getName ( ) );
        dead.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &cHas sido derrotado por &a" + winner.getName ( ) + "&c." ) );
        dead.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &c¡Pierdes &4" + (-1 * getStatLossByRank ( dead )) + " &cNiveles de &4" + stat.toUpperCase ( ) ) );
        // Teletransportar a ambos al spawn o a donde desees
        Bukkit.getScheduler ( ).runTaskLater ( Main.instance, () -> {
          Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "spawn " + winner.getName ( ) );
          Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "spawn " + dead.getName ( ) );
        }, 40L );
      }
      damageByPlayer.remove ( deadId );
      return;
    }
    RegionUtils regionUtils = new RegionUtils ( );
    ProtectedRegion protectedRegion = regionUtils.getRegionAtLocation ( event.getEntity ( ).getLocation ( ) );
    if (protectedRegion == null) return;
    Location loc = event.getEntity ( ).getLocation ( );
    if (!(RegionUtils.isLocationInRegion ( loc, "caospvp" ))) return;
    if (damageByPlayer.containsKey ( event.getEntity ( ).getUniqueId ( ) )) {
      UUID playerUUID = damageByPlayer.get ( event.getEntity ( ).getUniqueId ( ) );
      Player player = Main.instance.getServer ( ).getPlayer ( playerUUID );
      PKairos pkairosDamager = new PKairos ( player.getUniqueId ( ) );
      PKairos pKairos = new PKairos ( event.getEntity ( ).getPlayer ( ).getUniqueId ( ) );
      if (pKairos.getAmount ( ) >= 100) {
        pKairos.pay ( -100 );
        pkairosDamager.pay ( 100 );
      } else if (pKairos.getAmount ( ) >= 10) {
        pKairos.pay ( -10 );
        pkairosDamager.pay ( 10 );
      } else if (pKairos.getAmount ( ) >= 2) {
        pKairos.pay ( -2 );
        pkairosDamager.pay ( 2 );
      }
    }
  }

  private static final Map<UUID, Boolean> awaitingStatChoice = new HashMap<> ( );
  private static final HashMap<UUID, Integer> statAttempts = new HashMap<> ( );

  @EventHandler
  public void onPlayerChat ( org.bukkit.event.player.AsyncPlayerChatEvent event ) {
    Player player = event.getPlayer ( );
    UUID id = player.getUniqueId ( );

    if (!awaitingStatChoice.containsKey ( id )) return;

    event.setCancelled ( true );

    String stat = event.getMessage ( ).trim ( ).toUpperCase ( );

    if (!General.STATS_MAP.containsKey ( stat )) {
      int attempts = statAttempts.getOrDefault ( id, 0 ) + 1;
      statAttempts.put ( id, attempts );

      if (attempts >= 3) {
        awaitingStatChoice.remove ( id );
        statAttempts.remove ( id );
        player.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &cHas fallado 3 veces. &4Perdiste la oportunidad de elegir la estadística." ) );
      } else {
        player.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &cEsa estadística no existe. Intento &e" + attempts + "&c/3. Usa: &eSTR, DEX, CON, WIL, MND, SPI" ) );
      }
      return;
    }
    int gain = getStatLossByRank ( player ) / 2;
    int current = General.getSTAT ( General.STATS_MAP.get ( stat ), player );
    Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "jrmca add " + stat + " " + (current + gain) + " " + player.getName ( ) );
    player.sendMessage ( CC.translate ( "&8[&c&lBattle&8] &aHas ganado &2+" + gain + " &aNiveles en &2" + stat + "&a!" ) );
    awaitingStatChoice.remove ( id );
    statAttempts.remove ( id );
  }

  @EventHandler
  public void onRespawn ( PlayerRespawnEvent event ) {
    Player player = event.getPlayer ( );

    Essentials ess = (Essentials) Bukkit.getPluginManager ( ).getPlugin ( "Essentials" );
    if (ess != null) {
      try {
        Location warp = ess.getWarps ( ).getWarp ( "enma" );
        event.setRespawnLocation ( warp );
        return;
      } catch (Exception ignored) {
      }

      Bukkit.getScheduler ( ).runTask ( Main.instance,
        () -> player.performCommand ( "warp enma" ) );
    }

  }
}
