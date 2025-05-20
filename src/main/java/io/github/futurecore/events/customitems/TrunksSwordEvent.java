package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import net.minecraft.server.v1_7_R4.PacketPlayOutWorldParticles;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class TrunksSwordEvent implements Listener {
    private static final Map<String, Long> cooldowns = new HashMap<> ( );
    private static final List<String> IMMUNE_PLAYERS = Arrays.asList ( "DelawareX", "TheFive", "jean_sama" );
    private static final long COOLDOWN_TIME = 30 * 60 * 1000;
    private static final Map<UUID, Integer> kiSwordPlayer = new HashMap<> ( );

    @EventHandler
    public void onHit ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;
        if (!(event.getEntity ( ) instanceof Player)) return;
        Player victim = (Player) event.getEntity ( );
        Player lanzador = (Player) event.getDamager ( );
        if (((Player) event.getDamager ( )).isSneaking ( )) {
            if (!(event.getDamager ( ) instanceof Player)) return;
            if (!(event.getEntity ( ) instanceof Player)) return;
            String playerName = lanzador.getName ( );
            int kiCount = 0;
            if (lanzador.getItemInHand ( ) == null || lanzador.getItemInHand ( ).getTypeId ( ) != 5021) return;

            if (!IMMUNE_PLAYERS.contains ( playerName )) {
                long currentTime = System.currentTimeMillis ( );
                if (cooldowns.containsKey ( playerName )) {
                    long lastUsed = cooldowns.get ( playerName );
                    if (currentTime - lastUsed < COOLDOWN_TIME) {
                        long timeLeftMillis = COOLDOWN_TIME - (currentTime - lastUsed);
                        long seconds = (timeLeftMillis / 1000) % 60;
                        long minutes = (timeLeftMillis / 1000) / 60;
                        String formatted = String.format ( "%02d:%02d", minutes, seconds );
                        spawnHologram ( lanzador, "§c⌛ Debes esperar " + formatted + " minutos para volver a usarla" );
                        return;
                    }
                }
                cooldowns.put ( playerName, currentTime );
            }

            for (Entity nearby : lanzador.getNearbyEntities ( 5, 5, 5 )) {
                if (nearby instanceof Player) {
                    IDBCPlayer iPlayer = General.getDBCPlayer ( ((Player) nearby).getName ( ) );
                    kiCount += iPlayer.getKi ( );
                    iPlayer.setKi ( 0 );
                    iPlayer.getWorld ( ).thunderStrike ( iPlayer.getPosition ( ) );
                    spawnHologram ( lanzador, "§4☠ §c¡Haz tomado el ki de tus enemigos! §4☠" );
                }
            }
            kiSwordPlayer.put ( lanzador.getUniqueId ( ), kiCount );
            return;
        }
        if (kiSwordPlayer.containsKey ( event.getDamager ( ).getUniqueId ( ) )) {
            int kiCount = kiSwordPlayer.get ( event.getDamager ( ).getUniqueId ( ) ) * 2;
            try {
                ItemStack item = lanzador.getItemInHand();

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
            for (Entity nearby : event.getDamager ( ).getNearbyEntities ( 10, 10, 10 )) {
                if (nearby instanceof Player) {
                    Location loc = nearby.getLocation ( );
                    IDBCPlayer dbcVictim = General.getDBCPlayer ( ((Player) nearby).getName ( ) );
                    float hp = dbcVictim.getHP ( );
                    dbcVictim.setHP ( (int) (hp - kiCount) );
                    victim.getWorld ( ).playSound ( victim.getLocation ( ), Sound.HURT_FLESH, 1f, 0.6f );
                    spawnHologram ( victim, "§4☠ §c¡Has recibido el ataque de la Genki espada!" );
                    mostrarParticulas (
                            ((Player) nearby).getPlayer ( ), "hugeexplosion",
                            (float) dbcVictim.getX ( ), (float) dbcVictim.getY ( ), (float) dbcVictim.getZ ( ),
                            0.3f, 0.5f, 0.3f, 0.01f, 15
                    );
                    nearby.getWorld ( ).createExplosion ( loc, 0F );
                }
            }
            kiSwordPlayer.remove ( event.getDamager ( ).getUniqueId ( ) );
        }
    }

    public void mostrarParticulas ( Player jugador, String tipo, float x, float y, float z, float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad ) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles (
                tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
        );

        ((CraftPlayer) jugador).getHandle ( ).playerConnection.sendPacket ( packet );
    }
}
