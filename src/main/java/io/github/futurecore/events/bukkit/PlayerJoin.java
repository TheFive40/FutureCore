package io.github.futurecore.events.bukkit;

import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import static io.github.futurecore.events.customitems.FruitOfPowerEvent.fruitUsers;

public class PlayerJoin implements Listener {
    @EventHandler
    public void onJoinEvent ( PlayerJoinEvent event ) {
        IDBCPlayer idbcPlayer = General.getDBCPlayer ( event.getPlayer ( ).getName ( ) );
        if (!fruitUsers.containsKey ( event.getPlayer ( ).getUniqueId ( ) )) {
            idbcPlayer.removeBonusAttribute ( "Strength", "FruitOfPower" );
        }
    }
}
