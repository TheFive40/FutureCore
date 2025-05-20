package io.github.futurecore.events.bukkit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static io.github.futurecore.events.customitems.JacksScytheSwordEvent.bloquesTemporales;
import static io.github.futurecore.events.customitems.JacksScytheSwordEvent.froozePlayer;

public class PlayerMove implements Listener {
    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() == Material.ICE) {
            for (Location loc : bloquesTemporales) {
                if (loc.getBlockX() == block.getX() &&
                        loc.getBlockY() == block.getY() &&
                        loc.getBlockZ() == block.getZ() &&
                        loc.getWorld().equals(block.getWorld())) {
                    event.setCancelled(true);
                    player.sendMessage("§cNo puedes romper este hielo mágico.");
                    return;
                }
            }

        }
    }

    @EventHandler
    public void onPlayerMoveEvent( PlayerMoveEvent event ){
        Player player = event.getPlayer ();
        if(froozePlayer.containsKey ( player.getUniqueId () )){
            Location location = froozePlayer.get ( player.getUniqueId () );
            player.teleport ( location );
            event.setCancelled ( true );
        }
    }
}
