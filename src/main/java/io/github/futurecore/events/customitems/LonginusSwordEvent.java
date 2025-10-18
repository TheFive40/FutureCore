package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LonginusSwordEvent {
    public static Set<Player> playerLonginus = new HashSet<> ( );

    public static void onTask () {
        BukkitRunnable runnable = new BukkitRunnable ( ) {
            @Override
            public void run () {
                for (Player e : Main.instance.getServer ( ).getOnlinePlayers ( )) {
                    IDBCPlayer idbcPlayer = NpcAPI.Instance ( ).getPlayer ( e.getName ( ) ).getDBCPlayer ( );
                    if (e.getItemInHand ( ).getTypeId ( ) == 5012) {
                        idbcPlayer.addBonusAttribute ( "Strength", "Longinus", "*", 1.25, false );
                        idbcPlayer.addBonusAttribute ( "Constitution", "Longinus", "*", .80, false );
                        playerLonginus.add ( e );
                    }
                    if (e.getItemInHand ( ).getTypeId ( ) != 5012) {
                        idbcPlayer.removeBonusAttribute ( "Strength", "Longinus" );
                        idbcPlayer.removeBonusAttribute ( "Constitution", "Longinus" );
                        if (playerLonginus.contains ( e ))
                            playerLonginus.remove ( e );
                    }
                }
            }
        };
        runnable.runTaskTimer ( Main.instance, 20L, 20 );

    }
}
