package io.github.futurecore.events.bukkit;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class BlockedCommand implements Listener {
    @EventHandler
    public void onCommand( PlayerCommandPreprocessEvent event){
        String message = event.getMessage().toLowerCase();
        Player player = event.getPlayer();
        if (message.startsWith("/pl") || message.startsWith("/plugins") ||
        message.startsWith ( "//calc") || message.startsWith ( "//eval")) {
            player.sendMessage( ChatColor.RED + "Este comando está bloqueado.");
            event.setCancelled(true);
        }
    }
}
