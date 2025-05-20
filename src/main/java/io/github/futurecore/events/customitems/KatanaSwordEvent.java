package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.utils.General.spawnHologram;

public class KatanaSwordEvent implements Listener {
    private final Map<UUID, Integer> regenCounters = new HashMap<> ( );
    private final Map<UUID, Long> cooldowns = new HashMap<> ( );
    private final long COOLDOWN_TIME = 15 * 60 * 1000;

    @EventHandler
    public void onPlayerDamage ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;

        Player player = (Player) event.getDamager ( );
        UUID uuid = player.getUniqueId ( );
        ItemStack item = player.getItemInHand ( );

        if (item == null || item.getTypeId ( ) != 6118) return;

        long now = System.currentTimeMillis ( );
        if (cooldowns.containsKey ( uuid )) {
            long since = now - cooldowns.get ( uuid );
            if (since < COOLDOWN_TIME) {
                long rem = COOLDOWN_TIME - since;
                int seconds = (int) (rem / 1000) % 60;
                int minutes = (int) (rem / 1000) / 60;
                spawnHologram ( player, String.format ( "§c⌛ Debes esperar %02d:%02d para reactivar la Katana.", minutes, seconds ) );
                return;
            } else {
                cooldowns.remove ( uuid );
                regenCounters.put ( uuid, 0 );
            }
        }

        if (Math.random ( ) > 0.4) return;
        try {
            item = player.getItemInHand ( );

            if (!hasPermanent ( item )) {
                ItemStack updated = CmdItemUsage.hasUses ( item, player );
                if (updated == null) {
                    player.setItemInHand ( null );
                } else {
                    player.setItemInHand ( updated );
                }
                player.updateInventory ( );
            }
        } catch (Exception ignored) {
        }
        IDBCPlayer dbc = General.getDBCPlayer ( player.getName ( ) );
        if (dbc == null) return;
        int newStamina = (int) (dbc.getStamina ( ) * 0.3 + dbc.getStamina ( ));
        dbc.setStamina ( newStamina );
        spawnHologram ( player, "§4❤ §c¡Estamina restaurada por la Katana! §4❤" );

        int count = regenCounters.getOrDefault ( uuid, 0 ) + 1;
        regenCounters.put ( uuid, count );
        if (count >= 80) {
            cooldowns.put ( uuid, now );
            regenCounters.put ( uuid, 0 );
            spawnHologram ( player, "§e⏳ Se han usado 5 cargas. Katana en cooldown de 15 min." );
        }
    }
}
