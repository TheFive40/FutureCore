package io.github.futurecore.events.customitems;

import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class SpellEvent implements Listener {
    private static final Map<String, Long> cooldowns = new HashMap<> ( );
    private static final long COOLDOWN_TIME = 15 * 60 * 1000;
    private int spell = 4143;

    @EventHandler
    public void onRightClick ( PlayerInteractEvent event ) {
        ItemStack item = event.getPlayer ( ).getItemInHand ( );
        if (item == null) return;
        if (item.getTypeId ( ) != spell) return;
        if (event.getAction ( ) == Action.RIGHT_CLICK_AIR) {
            long currentTime = System.currentTimeMillis ( );
            if (cooldowns.containsKey ( event.getPlayer ( ).getName ( ) )) {
                long lastUsed = cooldowns.get ( event.getPlayer ( ).getName ( ) );
                if (currentTime - lastUsed < COOLDOWN_TIME) {
                    long timeLeftMillis = COOLDOWN_TIME - (currentTime - lastUsed);
                    long seconds = (timeLeftMillis / 1000) % 60;
                    long minutes = (timeLeftMillis / 1000) / 60;
                    String formatted = String.format ( "%02d:%02d", minutes, seconds );
                    spawnHologram ( event.getPlayer ( ), "§c⌛ Debes esperar " + formatted + " minutos para volver a usarla" );
                    return;
                }
            }
            cooldowns.put ( event.getPlayer ( ).getName ( ), System.currentTimeMillis ( ) );
            IDBCPlayer player = General.getDBCPlayer ( event.getPlayer ( ).getName ( ) );
            player.setRelease ( (byte) 125 );
        }
    }
}
