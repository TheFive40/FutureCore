package io.github.futurecore.events.bukkit;

import io.github.futurecore.Main;
import io.github.futurecore.utils.data.KairosData.PKairos;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.delaware.tools.NbtHandler.NbtHandler;

public class PlayerInteract implements Listener {
    @EventHandler
    public void onPlayerInteractEvent ( PlayerInteractEvent event ) {
        if (event.getAction ( ) != Action.RIGHT_CLICK_AIR) return;

        if (event.getPlayer ( ) == null) return;

        ItemStack item = event.getPlayer ( ).getItemInHand ( );
        if (item == null) return;
        switch (item.getTypeId ( )) {
            case 6200:
                int amount = item.getAmount ( );
                int pay = amount * 50;
                Main.instance.getServer ( ).dispatchCommand ( Main.instance.getServer ( ).getConsoleSender ( ),
                        "eco give " + event.getPlayer ( ).getName ( ) + " " + pay );
                event.getPlayer ().setItemInHand ( null );
                event.getPlayer().playSound ( event.getPlayer ().getLocation (),"random.orb",1.0F,1.0F );
                break;
            case 6086:
                amount = item.getAmount ( );
                pay = amount * 100;
                Main.instance.getServer ( ).dispatchCommand ( Main.instance.getServer ( ).getConsoleSender ( ),
                        "eco give " + event.getPlayer ( ).getName ( ) + " " + pay );
                event.getPlayer ().setItemInHand ( null );
                event.getPlayer().playSound ( event.getPlayer ().getLocation (),"random.orb",1.0F,1.0F );
                break;
            case 4444:
                amount = item.getAmount ( );
                pay = amount * 500;
                Main.instance.getServer ( ).dispatchCommand ( Main.instance.getServer ( ).getConsoleSender ( ),
                        "eco give " + event.getPlayer ( ).getName ( ) + " " + pay );
                event.getPlayer ().setItemInHand ( null );
                event.getPlayer().playSound ( event.getPlayer ().getLocation (),"random.orb",1.0F,1.0F );
                break;
            case 6084:
                amount = item.getAmount ( );
                pay = amount * 1000;
                Main.instance.getServer ( ).dispatchCommand ( Main.instance.getServer ( ).getConsoleSender ( ),
                        "eco give " + event.getPlayer ( ).getName ( ) + " " + pay );
                event.getPlayer ().setItemInHand ( null );
                event.getPlayer().playSound ( event.getPlayer ().getLocation (),"random.orb",1.0F,1.0F );
                break;
            case 6080:
                amount = item.getAmount ( );
                pay = amount * 10000;
                Main.instance.getServer ( ).dispatchCommand ( Main.instance.getServer ( ).getConsoleSender ( ),
                        "eco give " + event.getPlayer ( ).getName ( ) + " " + pay );
                event.getPlayer ().setItemInHand ( null );
                event.getPlayer().playSound ( event.getPlayer ().getLocation (),"random.orb",1.0F,1.0F );
                break;
        }
        try {
            NbtHandler nbtHandler = new NbtHandler ( item );

            if (nbtHandler.containsCompound ( "Kairos" )) {
                int kairosAmount = 0;
                try {
                    kairosAmount = nbtHandler.getInteger ( "kairosAmount" );
                } catch (Exception e) {
                    return;
                }

                if (kairosAmount <= 0) return;

                int amount = kairosAmount * item.getAmount ( );

                PKairos pKairos = new PKairos ( event.getPlayer ( ).getUniqueId ( ) );
                pKairos.pay ( amount );

                event.getPlayer ( ).setItemInHand ( null );

                event.getPlayer ( ).playSound (
                        event.getPlayer ( ).getLocation ( ),
                        "random.orb",
                        1.0f,
                        1.0f
                );
            }
        } catch (Exception ignored) {
        }
    }


}
