package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import net.minecraft.server.v1_7_R4.PacketPlayOutWorldParticles;
import noppes.npcs.api.INbt;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class ChainsawEvent implements Listener {
    private static final Map<String, Long> cooldowns = new HashMap<> ( );
    private static final List<String> IMMUNE_PLAYERS = Arrays.asList ( "DelawareX", "TheFive", "jean_sama" );
    private static final long COOLDOWN_TIME = 30 * 60 * 1000;

    @EventHandler
    public void hurt ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;
        if (!(event.getEntity ( ) instanceof Player)) return;

        Player lanzador = (Player) event.getDamager ( );
        if (lanzador.getItemInHand ( ).getTypeId ( ) != 6135) return;

        Player objetivo = (Player) event.getEntity ( );
        String playerName = lanzador.getName ( );

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
            cooldowns.put ( playerName, System.currentTimeMillis ( ) );
        }

        IDBCPlayer idbcPlayer = General.getDBCPlayer ( objetivo.getName ( ) );
        String fuzionPlayers = idbcPlayer.getNbt ( ).getCompound ( "PlayerPersisted" ).getString ( "jrmcFuzion" );

        if (fuzionPlayers == null || !fuzionPlayers.contains ( "," )) return;
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
        String[] namePlayers = fuzionPlayers.split ( "," );
        IDBCPlayer fuse1 = General.getDBCPlayer ( namePlayers[0].trim ( ) );
        IDBCPlayer fuse2 = General.getDBCPlayer ( namePlayers[1].trim ( ) );

        INbt nbtFuse1 = fuse1.getNbt ( ).getCompound ( "PlayerPersisted" );
        INbt nbtFuse2 = fuse2.getNbt ( ).getCompound ( "PlayerPersisted" );
        nbtFuse1.setString ( "jrmcFuzion", "" );
        nbtFuse2.setString ( "jrmcFuzion", "" );
        for (String name : namePlayers) {
            Player p = org.bukkit.Bukkit.getPlayer ( name.trim ( ) );
            if (p != null && p.isOnline ( )) {
                p.getWorld ( ).playSound ( p.getLocation ( ), Sound.ZOMBIE_WOODBREAK, 1f, 0.5f );
                spawnHologram ( p, "§5☯ §d¡La fusión ha sido separada por la motosierra!" );

                mostrarParticulas ( p, "explode",
                        (float) p.getLocation ( ).getX ( ),
                        (float) p.getLocation ( ).getY ( ) + 1.5f,
                        (float) p.getLocation ( ).getZ ( ),
                        0.5f, 0.5f, 0.5f, 0.01f, 30
                );
            }
        }
    }

    public void mostrarParticulas ( Player jugador, String tipo, float x, float y, float z, float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad ) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles (
                tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
        );
        ((CraftPlayer) jugador).getHandle ( ).playerConnection.sendPacket ( packet );
    }
}
