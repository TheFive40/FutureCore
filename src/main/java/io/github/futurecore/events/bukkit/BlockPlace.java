package io.github.futurecore.events.bukkit;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockPlace implements Listener {
    @EventHandler
    public void onBlockPlace ( BlockPlaceEvent event ) {
        if (event.getItemInHand ( ).getTypeId ( ) == 4478){
            ItemStack item = event.getPlayer ().getItemInHand();
            if (!item.hasItemMeta()) {
                return;
            }
            ItemMeta meta = item.getItemMeta();
            if (!meta.hasLore()) {
                return;
            }
            boolean tieneLoreDragon = false;
            for (String line : meta.getLore()) {
                if (ChatColor.stripColor(line).equalsIgnoreCase("㊝ ITEM DRAGÓN")) {
                    tieneLoreDragon = true;
                    break;
                }
            }
            if (tieneLoreDragon) {
                event.setCancelled ( true );
            }
        }
    }
}
