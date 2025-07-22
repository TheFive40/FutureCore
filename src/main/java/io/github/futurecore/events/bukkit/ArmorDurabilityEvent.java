package io.github.futurecore.events.bukkit;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;

public class ArmorDurabilityEvent implements Listener {
    @EventHandler
    public void onEntityDamage ( EntityDamageEvent event ) throws Exception {
        if (event.getEntity ( ) instanceof Player) {
            Player player = (Player) event.getEntity ( );
            try {
                ItemStack helmet = player.getInventory ( ).getHelmet ( );
                if (helmet != null) {
                    if (!hasPermanent ( helmet )) {
                        ItemStack updated = CmdItemUsage.hasUses ( helmet, player );
                        if (updated == null) {
                            player.getInventory ( ).setHelmet ( null );
                        } else {
                            player.getInventory ( ).setHelmet ( updated );
                        }
                        player.updateInventory ( );
                    }
                }
                ItemStack chestplate = player.getInventory ( ).getChestplate ( );
                if (chestplate != null) {
                    if (!hasPermanent ( chestplate )) {
                        ItemStack updated = CmdItemUsage.hasUses ( chestplate, player );
                        if (updated == null) {
                            player.getInventory ( ).setChestplate ( null );
                        } else {
                            player.getInventory ( ).setChestplate ( updated );
                        }
                        player.updateInventory ( );
                    }

                }
                ItemStack leggings = player.getInventory ( ).getLeggings ( );
                if (leggings != null) {
                    if (!hasPermanent ( leggings )) {
                        ItemStack updated = CmdItemUsage.hasUses ( leggings, player );
                        if (updated == null) {
                            player.getInventory ( ).setLeggings ( null );
                        } else {
                            player.getInventory ( ).setLeggings ( updated );
                        }
                        player.updateInventory ( );
                    }

                }
                ItemStack boots = player.getInventory ( ).getBoots ( );
                if (boots != null) {
                    if (!hasPermanent ( boots )) {
                        ItemStack updated = CmdItemUsage.hasUses ( boots, player );
                        if (updated == null) {
                            player.getInventory ( ).setBoots ( null );
                        } else {
                            player.getInventory ( ).setBoots ( updated );
                        }
                        player.updateInventory ( );
                    }
                }

            } catch (Exception ignored) {

            }
        }
    }
}
