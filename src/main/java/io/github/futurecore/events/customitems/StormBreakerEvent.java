package io.github.futurecore.events.customitems;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.entity.Entity;
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

public class StormBreakerEvent implements Listener {
    private static final Map<String, Long> cooldowns = new HashMap<> ();
    private static final List<String> IMMUNE_PLAYERS = Arrays.asList("DelawareX", "TheFive", "jean_sama");
    private static final long COOLDOWN_TIME = 30 * 60 * 1000;
    @EventHandler
    public void onRightClick( EntityDamageByEntityEvent event) {
        if(!(event.getDamager () instanceof  Player)) return;
        if(!(event.getEntity () instanceof  Player)) return;
        Player lanzador = (Player) event.getDamager ();
        String playerName = lanzador.getName();

        if (lanzador.getItemInHand() == null || lanzador.getItemInHand().getTypeId() != 6153) return;

        if (!IMMUNE_PLAYERS.contains(playerName)) {
            long currentTime = System.currentTimeMillis();
            if (cooldowns.containsKey(playerName)) {
                long lastUsed = cooldowns.get(playerName);
                if (currentTime - lastUsed < COOLDOWN_TIME) {
                    long timeLeftMillis = COOLDOWN_TIME - (currentTime - lastUsed);
                    long seconds = (timeLeftMillis / 1000) % 60;
                    long minutes = (timeLeftMillis / 1000) / 60;
                    String formatted = String.format("%02d:%02d", minutes, seconds);
                    spawnHologram (lanzador,"§c⌛ Debes esperar " + formatted + " minutos para volver a usarla");
                    return;
                }
            }
            cooldowns.put(playerName, currentTime);
        }
        try {
            ItemStack item = lanzador.getItemInHand ( );

            if (!hasPermanent ( item )) {
                ItemStack updated = CmdItemUsage.hasUses ( item, lanzador );
                if (updated == null) {
                    lanzador.setItemInHand ( null );
                } else {
                    lanzador.setItemInHand ( updated );
                }
                lanzador.updateInventory ( );
            }
        } catch (Exception ignored) {
        }
        for (Entity nearby : lanzador.getNearbyEntities(5, 5, 5)) {
            if (nearby instanceof Player){
                IDBCPlayer iPlayer = General.getDBCPlayer ( ((Player) nearby).getName ());
                iPlayer.setHP ( (int) (iPlayer.getHP () -  (iPlayer.getHP () * 0.5)) );
                iPlayer.getWorld().thunderStrike(iPlayer.getPosition());
                spawnHologram(lanzador, "§4☠ §c¡El alma de tu enemigo ha sido devastada por la Storm Breaker! §4☠");
            }
        }

    }
}
