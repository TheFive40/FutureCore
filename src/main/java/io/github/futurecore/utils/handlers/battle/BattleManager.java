package io.github.futurecore.utils.handlers.battle;

import io.github.futurecore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

import static io.github.futurecore.utils.handlers.battle.BattleTask.startBattleCountdown;

public class BattleManager {

    private static final Map<String, List<String>> arenasByRank = new HashMap<> ( );

    private static final Set<String> arenasInUse = new HashSet<> ( );

    static {
        arenasByRank.put ( "F", Arrays.asList ( "arenaF1", "arenaF2", "arenaF3" ) );
        arenasByRank.put ( "E", Arrays.asList ( "arenaE1", "arenaE2", "arenaE3" ) );
        arenasByRank.put ( "D", Arrays.asList ( "arenaD1", "arenaD2", "arenaD3" ) );
        arenasByRank.put ( "C", Arrays.asList ( "arenaC1", "arenaC2", "arenaC3" ) );
        arenasByRank.put ( "B", Arrays.asList ( "arenaB1", "arenaB2", "arenaB3" ) );
        arenasByRank.put ( "A", Arrays.asList ( "arenaA1", "arenaA2", "arenaA3" ) );
        arenasByRank.put ( "A+", Arrays.asList ( "arenaA+1", "arenaA+2", "arenaA+3" ) );
        arenasByRank.put ( "S", Arrays.asList ( "arenaS1", "arenaS2", "arenaS3" ) );
        arenasByRank.put ( "S+", Arrays.asList ( "arenaS+1", "arenaS+2", "arenaS+3" ) );
        arenasByRank.put ( "Z", Arrays.asList ( "arenaZ1", "arenaZ2", "arenaZ3" ) );
        arenasByRank.put ( "Z+", Arrays.asList ( "arenaZ+1", "arenaZ+2", "arenaZ+3" ) );
    }

    /**
     * Busca una arena libre del rango y teletransporta a ambos jugadores mediante /warp.
     */
    public static void teleportToArena ( Player challenger, Player target, String rank ) {
        List<String> arenas = arenasByRank.get ( rank );

        if (arenas == null || arenas.isEmpty ( )) {
            challenger.sendMessage ( CC.translate ( "&cNo existen arenas configuradas para el rango &4" + rank ) );
            return;
        }

        for (String arena : arenas) {
            if (!isArenaInUse ( arena )) {
                arenasInUse.add ( arena );

                Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp " + arena + " " + challenger.getName ( ) );

                Bukkit.getScheduler ( ).runTaskLater ( Bukkit.getPluginManager ( ).getPlugin ( "FutureCore" ), () -> {
                    Location challengerLoc = challenger.getLocation ( );
                    Vector direction = challengerLoc.getDirection ( ).normalize ( );
                    Location targetLoc = challengerLoc.clone ( ).add ( direction.multiply ( 30 ) );
                    target.teleport ( targetLoc );

                    challenger.sendMessage ( CC.translate ( "&aHas sido enviado a la arena &2" + arena ) );
                    target.sendMessage ( CC.translate ( "&aHas sido enviado a la arena &2" + arena + " &a(frente a tu oponente)" ) );
                }, 20L );
                challenger.sendMessage ( CC.translate ( "&aHas sido enviado a la arena &2" + arena ) );
                target.sendMessage ( CC.translate ( "&aHas sido enviado a la arena &2" + arena ) );
                startBattleCountdown ( challenger, target );
                return;
            }
        }

        challenger.sendMessage ( CC.translate ( "&cTodas las arenas del rango &4" + rank + " &cestán ocupadas." ) );
    }

    /**
     * Libera una arena cuando termina el combate.
     */
    public static void freeArena ( String arenaName ) {
        arenasInUse.remove ( arenaName );
    }

    /**
     * Verifica si una arena está ocupada.
     */
    public static boolean isArenaInUse ( String arenaName ) {
        return arenasInUse.contains ( arenaName );
    }
}
