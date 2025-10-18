package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class StormSwordEvent implements Listener {
    private static final Map<String, Integer> usosRestantes = new HashMap<> ( );
    private static final Map<String, Long> cooldowns = new HashMap<> ( );
    private static final List<String> IMMUNE_PLAYERS = Arrays.asList ( "DelawareX", "TheFive", "jean_sama" );
    private static final int USOS_MAXIMOS = 3;
    private static final long COOLDOWN_FINAL = 15 * 60 * 1000;

    @EventHandler
    public void onHit ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;
        IWorld world = NpcAPI.Instance ( ).getPlayer ( ((Player) event.getDamager ( )).getName ( ) )
                .getWorld ( );
        IEntity<?> iEntity = world.getEntityByID ( event.getEntity ( ).getEntityId ( ) );
        Player atacante = (Player) event.getDamager ( );
        String atacanteName = atacante.getName ( );
        long currentTime = System.currentTimeMillis ( );
        if (!IMMUNE_PLAYERS.contains ( atacanteName )) {
            if (cooldowns.containsKey ( atacanteName )) {
                long lastFinal = cooldowns.get ( atacanteName );
                if (currentTime - lastFinal < COOLDOWN_FINAL) {
                    long timeLeft = COOLDOWN_FINAL - (currentTime - lastFinal);
                    int minutes = (int) (timeLeft / 1000) / 60;
                    int seconds = (int) (timeLeft / 1000) % 60;
                    spawnHologram ( atacante, "§c⌛ Cooldown (15m): " + String.format ( "%02d:%02d", minutes, seconds ) );
                    return;
                } else {
                    usosRestantes.put ( atacanteName, USOS_MAXIMOS );
                    cooldowns.remove ( atacanteName );
                }
            }
        }
        usosRestantes.putIfAbsent ( atacanteName, USOS_MAXIMOS );
        int usos = usosRestantes.get ( atacanteName );
        if (usos <= 0 && !IMMUNE_PLAYERS.contains ( atacanteName )) {
            cooldowns.put ( atacanteName, currentTime );
            spawnHologram ( atacante, "§c⛔ Usos agotados. Espada en cooldown de 15 minutos." );
            return;
        }
        if (atacante.getItemInHand ( ).getTypeId ( ) != 5003) return;
        if (iEntity instanceof ICustomNpc<?>) {
            if (Math.random ( ) <= 0.4) {
                try {
                    ItemStack item = atacante.getItemInHand ( );

                    if (!hasPermanent ( item )) {
                        ItemStack updated = CmdItemUsage.hasUses ( item, atacante );
                        if (updated == null) {
                            atacante.setItemInHand ( null );
                        } else {
                            atacante.setItemInHand ( updated );
                        }
                        atacante.updateInventory ( );
                    }
                } catch (Exception ignored) {
                }
                ICustomNpc<?> npc = (ICustomNpc<?>) world
                        .getEntityByID ( event.getEntity ( ).getEntityId ( ) );
                npc.setHealth ( npc.getHealth ( ) - (float) (npc.getHealth ( ) * 0.08) );
                npc.getWorld ( ).thunderStrike ( npc.getPosition ( ) );
                usosRestantes.put ( atacanteName, usos - 1 );
            }
            return;
        }
        Player objetivo = (Player) event.getEntity ( );
        if (atacante.getItemInHand ( ) == null) return;
        if (Math.random ( ) <= 0.4) {
            try {
                ItemStack item = atacante.getItemInHand ( );

                if (!hasPermanent ( item )) {
                    ItemStack updated = CmdItemUsage.hasUses ( item, atacante );
                    if (updated == null) {
                        atacante.setItemInHand ( null );
                    } else {
                        atacante.setItemInHand ( updated );
                    }
                    atacante.updateInventory ( );
                }
            } catch (Exception ignored) {
            }
            objetivo.getWorld ( ).strikeLightningEffect ( objetivo.getLocation ( ) );
            IDBCPlayer player = NpcAPI.Instance ( ).getPlayer ( objetivo.getName ( ) ).getDBCPlayer ( );
            player.setHP ( player.getHP ( ) - (int) (player.getHP ( ) * 0.1) );
            objetivo.getWorld ( ).strikeLightning ( objetivo.getLocation ( ) );
            usosRestantes.put ( atacanteName, usos - 1 );
        }
    }

}
