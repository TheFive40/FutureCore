package io.github.futurecore.events.customitems;

import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PowerUpEvent implements Listener {
    @EventHandler
    public void onRightClick ( PlayerInteractEvent event ) {
        if (event.getAction ( ) == Action.RIGHT_CLICK_AIR) {
            Player player = event.getPlayer ( );
            ItemStack item = player.getItemInHand ( );
            if (item.getTypeId ( ) != 4943) return;
            player.setItemInHand ( null );
            IDBCPlayer idbcPlayer = General.getDBCPlayer ( player.getName ( ) );
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmcse set KO 0 " + player.getName());
            idbcPlayer.setRelease ( (byte) 50 );
        }
    }
}
