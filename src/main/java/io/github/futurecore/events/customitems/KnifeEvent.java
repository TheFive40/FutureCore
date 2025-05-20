package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class KnifeEvent implements Listener {
    private static final Map<String, Long> cooldowns = new HashMap<> ( );
    private static final long COOLDOWN = 20 * 60 * 1000;
    private static final int INVISIBILITY_DURATION = 30;
    private static final int ITEM_ID = 6098;

    @EventHandler
    public void onKnifeHit ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;
        if (!(event.getEntity ( ) instanceof Player)) return;
        Player victim = (Player) event.getEntity ( );

        if (Math.random ( ) <= 0.05) {
            try {
                ItemStack item = ((Player) event.getDamager ()).getItemInHand ( );

                if (!hasPermanent ( item )) {
                    ItemStack updated = CmdItemUsage.hasUses ( item, (Player) event.getDamager () );
                    if (updated == null) {
                        ((Player) event.getDamager ()).setItemInHand ( null );
                    } else {
                        ((Player) event.getDamager ()).setItemInHand ( updated );
                    }
                    ((Player) event.getDamager ()).updateInventory ( );
                }
            } catch (Exception ignored) {
            }
            IDBCPlayer dbcVictim = General.getDBCPlayer ( victim.getName ( ) );
            float hp = dbcVictim.getHP ( );
            float damage = hp * 0.10f;
            dbcVictim.setHP ( (int) (hp - damage) );

            victim.getWorld ( ).playSound ( victim.getLocation ( ), Sound.HURT_FLESH, 1f, 0.6f );
            spawnHologram ( victim, "§4☠ §c¡Has recibido un corte crítico!" );
        }
    }

    @EventHandler
    public void onInteract ( PlayerInteractEvent event ) {

        Player lanzador = event.getPlayer ( );

        ItemStack item = lanzador.getItemInHand ( );
        if (item == null || item.getTypeId ( ) != ITEM_ID) return;

        String playerName = lanzador.getName ( );
        long now = System.currentTimeMillis ( );

        if (cooldowns.containsKey ( playerName )) {
            long lastUsed = cooldowns.get ( playerName );
            if (now - lastUsed < COOLDOWN) {
                long remaining = COOLDOWN - (now - lastUsed);
                int minutes = (int) (remaining / 1000) / 60;
                int seconds = (int) (remaining / 1000) % 60;
                String mensaje = String.format ( "§c⌛ Knife en enfriamiento: %02d:%02d", minutes, seconds );
                spawnHologram ( lanzador, mensaje );
                return;
            }
        }
        try {
             item = lanzador.getItemInHand();

            if (!hasPermanent(item)) {
                ItemStack updated = CmdItemUsage.hasUses(item, lanzador);
                if (updated == null) {
                    lanzador.setItemInHand(null);
                } else {
                    lanzador.setItemInHand(updated);
                }
                lanzador.updateInventory();
            }
        } catch (Exception ignored) {
        }
        lanzador.playSound ( lanzador.getLocation ( ), Sound.ENDERMAN_TELEPORT, 1f, 0.5f );
        spawnHologram ( lanzador, "§7✧ Te has vuelto §o§lINVISIBLE§r§7 por 30 segundos..." );

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "vanish " + lanzador.getName ());


        Bukkit.getScheduler ( ).runTaskLater ( Main.instance, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "vanish " + lanzador.getName ());
        }, 20 * 30 );
        cooldowns.put ( playerName, now );
    }



}
