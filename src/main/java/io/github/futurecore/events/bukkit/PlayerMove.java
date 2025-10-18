package io.github.futurecore.events.bukkit;

import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.General;
import io.github.futurecore.utils.RegionUtils;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;

import static io.github.futurecore.events.customitems.JacksScytheSwordEvent.bloquesTemporales;
import static io.github.futurecore.events.customitems.JacksScytheSwordEvent.froozePlayer;

public class PlayerMove implements Listener {


    @EventHandler
    public void onBlockBreak ( org.bukkit.event.block.BlockBreakEvent event ) {
        Player player = event.getPlayer ( );
        Block block = event.getBlock ( );
      String regionName = RegionUtils.getRegionNameAtLocation(player.getLocation());
      if (regionName != null && regionName.equalsIgnoreCase("TrainingHalloween")) {
        IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());
        if (!idbcPlayer.hasFinishedQuest(860)) {
          player.sendMessage(CC.translate("&c¡Debes completar la misión de reunir todas las calabazas!"));
          event.setCancelled(true);
        }
      }

      if (block.getType ( ) == Material.ICE) {
            for (Location loc : bloquesTemporales) {
                if (loc.getBlockX ( ) == block.getX ( ) &&
                        loc.getBlockY ( ) == block.getY ( ) &&
                        loc.getBlockZ ( ) == block.getZ ( ) &&
                        loc.getWorld ( ).equals ( block.getWorld ( ) )) {
                    event.setCancelled ( true );
                    player.sendMessage ( "§cNo puedes romper este hielo mágico." );
                    return;
                }
            }

        }
    }

    @EventHandler
    public void onPlayerMoveEvent ( PlayerMoveEvent event ) {
        Player player = event.getPlayer ( );
        if (RegionUtils.isLocationInRegion ( player.getLocation ( ), "sumo" )) {
            IDBCPlayer idbcPlayer = General.getDBCPlayer ( player.getName ( ) );
            idbcPlayer.setKi ( 0 );
            return;
        }
        if (froozePlayer.containsKey ( player.getUniqueId ( ) )) {
            Location location = froozePlayer.get ( player.getUniqueId ( ) );
            player.teleport ( location );
            event.setCancelled ( true );
        }
    }
}
