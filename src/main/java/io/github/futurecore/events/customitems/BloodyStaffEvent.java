package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.delaware.tools.CustomItems.CustomItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.utils.General.spawnHologram;

public class BloodyStaffEvent implements Listener {

    private final Map<UUID, Integer> storedLife = new HashMap<> ( );


    @EventHandler
    public void onHit ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;
        if (!(event.getEntity ( ) instanceof Player)) return;
        Player attacker = (Player) event.getDamager ( );
        Player target = (Player) event.getEntity ( );
        ItemStack itemInHand = attacker.getItemInHand ( );
        if (itemInHand == null) return;
        if (itemInHand.getTypeId ( ) != 4295) return;
        if (CustomItems.getLinkedCustomItem ( itemInHand ) == null) return;
        IDBCPlayer targetDBC = General.getDBCPlayer ( target.getName ( ) );
        if (targetDBC == null) return;
        int stolenHP = Math.max ( 1, Math.abs ( (int) (targetDBC.getHP ( ) * 0.02) ) );
        if (!storedLife.containsKey ( attacker.getUniqueId ( ) )) {
            storedLife.put ( attacker.getUniqueId ( ), Math.min ( stolenHP, 5000 ) );
            attacker.sendMessage ( CC.translate ( "&cHas robado &4+ " + stolenHP + " &cHP total almacenado &4[" +
                    storedLife.get ( attacker.getUniqueId ( ) ) + "]" ) );
            targetDBC.setHP ( targetDBC.getHP ( ) - stolenHP );
        } else {
            if (storedLife.get ( attacker.getUniqueId ( ) ) >= 5000) return;
            storedLife.put ( attacker.getUniqueId ( ), Math.abs ( Math.min ( stolenHP + storedLife.get ( attacker.getUniqueId ( ) ), 5000 ) ) );
            targetDBC.setHP ( targetDBC.getHP ( ) - stolenHP );
            attacker.sendMessage ( CC.translate ( "&cHas robado &4+ " + stolenHP + " &cHP total almacenado &4[" +
                    storedLife.get ( attacker.getUniqueId ( ) ) + "]" ) );
        }
    }


    @EventHandler
    public void onRightClick ( PlayerInteractEvent event ) {
        Player player = event.getPlayer ( );
        ItemStack itemInHand = player.getItemInHand ( );
        if (event.getAction ( ) != Action.RIGHT_CLICK_AIR) return;
        if (itemInHand == null) return;
        if (itemInHand.getTypeId ( ) != 4295) return;
        if (CustomItems.getLinkedCustomItem ( itemInHand ) == null) return;
        UUID playerId = player.getUniqueId ( );
        if (!storedLife.containsKey ( playerId )) return;

        int lifeToHeal = storedLife.getOrDefault ( playerId, 0 );
        if (lifeToHeal <= 0) {
            spawnHologram ( player, "§cNo tienes vida almacenada en el báculo." );
        } else {
            try {
                ItemStack item = player.getItemInHand();

                if (!hasPermanent(item)) {
                    ItemStack updated = CmdItemUsage.hasUses(item, player);
                    if (updated == null) {
                        player.setItemInHand(null);
                    } else {
                        player.setItemInHand(updated);
                    }
                    player.updateInventory();
                }
            } catch (Exception ignored) {
            }
            IDBCPlayer player1 = General.getDBCPlayer ( player.getName ( ) );
            int regen = storedLife.get ( playerId ) - player1.getHP ( );
            if (regen <= 0) {
                player.sendMessage ( CC.translate ( "&aHas recuperado &2+" + storedLife.get ( playerId ) + "&a de tu vida" ) );
                player1.setHP ( player1.getHP ( ) + storedLife.get ( playerId ) );
                storedLife.put ( playerId, 0 );

            } else {
                player.sendMessage ( CC.translate ( "&aHas recuperado &2+" + storedLife.get ( playerId ) + "&a de tu vida" ) );
                player1.setHP ( player1.getHP ( ) + storedLife.get ( playerId ) );
                storedLife.put ( playerId, (storedLife.get ( playerId ) - regen) );

            }
        }
    }

}
