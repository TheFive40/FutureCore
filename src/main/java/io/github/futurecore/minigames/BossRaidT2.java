package io.github.futurecore.minigames;

import io.github.futurecore.utils.General;
import io.github.futurecore.utils.RegionUtils;
import io.github.futurecore.utils.menus.RaidMenuT2;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

import static io.github.futurecore.utils.menus.RaidMenuT2.jugadores;

public class BossRaidT2 implements Listener {
    public static int bossHits = 0;
    private static final String REGION_RAID = "raidt2";

    @EventHandler
    public void onPlayerMove ( PlayerMoveEvent event ) {
        Player player = event.getPlayer ( );
        Location to = event.getTo ( );
        Location from = event.getFrom ( );

        if (to == null || from == null) return;

        if (from.getBlockX ( ) == to.getBlockX ( ) &&
                from.getBlockY ( ) == to.getBlockY ( ) &&
                from.getBlockZ ( ) == to.getBlockZ ( )) return;

        boolean wasIn = RegionUtils.isLocationInRegion ( from, REGION_RAID );
        boolean isNowIn = RegionUtils.isLocationInRegion ( to, REGION_RAID );
        UUID uuid = player.getUniqueId ( );
        if(!isNowIn && RaidMenuT2.getJugadores ( ).contains ( uuid )){
            jugadores.remove ( uuid );
        }
        if (isNowIn && !RaidMenuT2.getJugadores ( ).contains ( uuid ) && !General.hasStaffParent ( player )) {
            player.sendMessage ( ChatColor.RED + "No estás registrado en esta raid. Te regresamos al spawn." );
            Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spawn " + player.getName ( ) );
        }

        if (wasIn && !isNowIn && RaidMenuT2.getJugadores ( ).contains ( uuid )) {
            RaidMenuT2.removeJugador ( uuid );
            player.sendMessage ( ChatColor.RED + "Has salido de la zona de raid. Fuiste eliminado de la raid." );
            if(jugadores.isEmpty ()){
                RaidMenuT2.estado = RaidMenuT2.EstadoRaid.EN_ESPERA;
            }
        }
    }

    @EventHandler
    public void onBossDamaged ( EntityDamageByEntityEvent event ) {
        ICustomNpc<?> boss = RaidMenuT2.getCurrentBoss ( );
        if (!(event.getDamager ( ) instanceof Player)) return;
        if (boss == null || !boss.isAlive ( )) return;
        if (!RegionUtils.isLocationInRegion ( event.getDamager ( ).getLocation ( ), "raidt2" ))
            return;
        int id = event.getEntity ( ).getEntityId ( );
        IDBCPlayer idbcPlayer = General.getDBCPlayer ( ((Player) event.getDamager ( )).getName ( ) );
        ICustomNpc<?> iCustomNpc = (ICustomNpc<?>) idbcPlayer.getWorld ( ).getEntityByID ( id );
        if (!iCustomNpc.equals ( boss )) return;
        UUID uuid = event.getDamager ( ).getUniqueId ( );
        RaidMenuT2.getRankingHits ( ).put ( uuid, RaidMenuT2.getRankingHits ( ).getOrDefault ( uuid, 0 ) + 1 );
        bossHits++;
    }

    @EventHandler
    public void onPlayerDeath ( PlayerDeathEvent event ) {
        Player player = event.getEntity ( );
        UUID uuid = player.getUniqueId ( );

        if (RaidMenuT2.getJugadores ( ).contains ( uuid )) {
            RaidMenuT2.removeJugador ( uuid );
            Bukkit.broadcastMessage ( ChatColor.RED + "[RAID] " + player.getName ( ) + " ha sido eliminado de la raid por morir." );
        }
    }


}
