package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class JanembaSwordEvent implements Listener {

    private static final int ITEM_ID = 6141;
    private static final double PROBABILIDAD_EFECTO = 0.15;
    private static final int USOS_MAXIMOS = 5;
    private static final long COOLDOWN_FINAL = 15 * 60 * 1000;
    private static final Map<String, Integer> usosRestantes = new HashMap<>();
    private static final Map<String, Long> cooldowns = new HashMap<> ( );
    private static final List<String> IMMUNE_PLAYERS = Arrays.asList ( "DelawareX", "TheFive", "jean_sama" );

    @EventHandler
    public void onHit ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;

        Player atacante = (Player) event.getDamager ( );
        ItemStack arma = atacante.getItemInHand ( );
        if (arma == null || arma.getTypeId ( ) != ITEM_ID) return;

        String atacanteName = atacante.getName ( );
        long currentTime = System.currentTimeMillis ( );

        if (!IMMUNE_PLAYERS.contains(atacanteName)) {
            if (cooldowns.containsKey(atacanteName)) {
                long lastFinal = cooldowns.get(atacanteName);
                if (currentTime - lastFinal < COOLDOWN_FINAL) {
                    long timeLeft = COOLDOWN_FINAL - (currentTime - lastFinal);
                    int minutes = (int) (timeLeft / 1000) / 60;
                    int seconds = (int) (timeLeft / 1000) % 60;
                    spawnHologram(atacante, "§c⌛ Cooldown (15m): " + String.format("%02d:%02d", minutes, seconds));
                    return;
                } else {
                    usosRestantes.put(atacanteName, USOS_MAXIMOS);
                    cooldowns.remove(atacanteName);
                }
            }

            usosRestantes.putIfAbsent(atacanteName, USOS_MAXIMOS);

        }

        int usos = usosRestantes.get(atacanteName);
        if (usos <= 0 && !IMMUNE_PLAYERS.contains(atacanteName)) {
            cooldowns.put(atacanteName, currentTime);
            spawnHologram(atacante, "§c⛔ Usos agotados. Espada en cooldown de 15 minutos.");
            return;
        }
        Entity objetivo = event.getEntity ( );
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
        if (objetivo instanceof Player) {

            Player objetivoPlayer = (Player) objetivo;
            objetivoPlayer.addPotionEffect ( new PotionEffect ( PotionEffectType.BLINDNESS, 20 * 5, 0 ) );
            objetivoPlayer.addPotionEffect ( new PotionEffect ( PotionEffectType.WITHER, 20 * 5, 0 ) );
        }
        if (Math.random ( ) <= PROBABILIDAD_EFECTO) {
            if (objetivo instanceof Player) {
                Player objetivoPlayer = (Player) objetivo;
                IDBCPlayer dbc = General.getDBCPlayer ( objetivoPlayer.getName ( ) );

                int dañoVida = (int) (dbc.getHP ( ) * 0.20f);
                int reducciónStamina = (int) (dbc.getStamina ( ) * 0.12f);
                int reducciónKi = (int) (dbc.getKi ( ) * 0.40f);

                dbc.setHP ( dbc.getHP ( ) - dañoVida );
                dbc.setStamina ( dbc.getStamina ( ) - reducciónStamina );
                dbc.setKi ( dbc.getKi ( ) - reducciónKi );
                usosRestantes.put(atacanteName, usos - 1);
                spawnHologram(atacante, "§5⚔ Usos restantes de la Espada de Janemba: §f" + (usos - 1));

                objetivoPlayer.playSound ( objetivoPlayer.getLocation ( ), Sound.WITHER_HURT, 1f, 0.5f );
                spawnHologram ( objetivoPlayer, "§5☠ §c¡Has sido cortado por la Espada de Janemba!" );
            } else {
                IWorld world = NpcAPI.Instance ( ).getPlayer ( ((Player) event.getDamager ( )).getName ( ) )
                        .getWorld ( );
                IEntity<?> iEntity = world.getEntityByID ( event.getEntity ( ).getEntityId ( ) );
                if (iEntity instanceof ICustomNpc<?>) {
                    ICustomNpc<?> npc = (ICustomNpc<?>) world.getEntityByID ( event.getEntity ( ).getEntityId ( ) );
                    npc.getWorld ( ).thunderStrike ( npc.getPosition ( ) );
                    int dañoVida = (int) (npc.getHealth ( ) * 0.12f);
                    npc.setHealth ( npc.getHealth ( ) - dañoVida );
                    usosRestantes.put(atacanteName, usos - 1);
                    spawnHologram(atacante, "§5⚔ Usos restantes de la Espada de Janemba: §f" + (usos - 1));

                }

            }

        }
    }
}
