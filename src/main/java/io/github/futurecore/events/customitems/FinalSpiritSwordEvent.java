package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.commands.player.cmdcore.cmdItemsAbilities.CmdKiScythe;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class FinalSpiritSwordEvent implements Listener {

    private static final int ITEM_ID = 4951;
    private static final double PROBABILIDAD = 0.15;

    private static final long COOLDOWN_ESPADA = 15 * 60 * 1000; // 15 min
    private static final long COOLDOWN_GUADANA = 5 * 60 * 1000; // 5 min

    private static final int MAX_USOS_ANTES_COOLDOWN = 3;

    private final Map<String, Long> cooldowns = new HashMap<> ( );
    private final Map<String, Integer> usosPrevios = new HashMap<> ( );
    private final Random random = new Random ( );

    @EventHandler
    public void onHit ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;
        if (!(event.getEntity ( ) instanceof Player)) return;
        Player attacker = (Player) event.getDamager ( );
        ItemStack weapon = attacker.getItemInHand ( );
        if (weapon == null) return;

        boolean isGuadana = CmdKiScythe.isGuadana ( weapon );
        boolean isEspadaFinal = weapon.getTypeId ( ) == ITEM_ID && !isGuadana;
        if (!isEspadaFinal && !isGuadana) return;

        String attackerName = attacker.getName ( );
        long now = System.currentTimeMillis ( );
        long cooldown = isGuadana ? COOLDOWN_GUADANA : COOLDOWN_ESPADA;

        boolean isImmune = attackerName.equalsIgnoreCase ( "DelawareX" ) ||
                attackerName.equalsIgnoreCase ( "TheFive" ) ||
                attackerName.equalsIgnoreCase ( "jean_sama" );

        if (!isImmune && cooldowns.containsKey ( attackerName )) {
            long lastUse = cooldowns.get ( attackerName );
            if (now - lastUse < cooldown) {
                long remaining = cooldown - (now - lastUse);
                int minutes = (int) (remaining / 1000) / 60;
                int seconds = (int) (remaining / 1000) % 60;
                spawnHologram ( attacker,
                        String.format ( "§e⌛ %s en cooldown: %02d:%02d",
                                isGuadana ? "Guadaña" : "Espada", minutes, seconds ) );
                return;
            }
        }

        // Probabilidad solo si es espada, la guadaña es 100% de activación
        if (isGuadana || random.nextDouble ( ) <= PROBABILIDAD) {
            try {
                if (!hasPermanent ( weapon )) {
                    ItemStack updated = CmdItemUsage.hasUses ( weapon, attacker );
                    if (updated == null) {
                        attacker.setItemInHand ( null );
                    } else {
                        attacker.setItemInHand ( updated );
                    }
                    attacker.updateInventory ( );
                }
            } catch (Exception ignored) {
            }

            Player target = (Player) event.getEntity ( );
            IDBCPlayer dbcTarget = General.getDBCPlayer ( target.getName ( ) );

            target.addPotionEffect ( new PotionEffect ( PotionEffectType.SLOW, 20 * 5, 10 ) );
            target.addPotionEffect ( new PotionEffect ( PotionEffectType.JUMP, 20 * 5, 128 ) );

            spawnHologram ( target, isGuadana ?
                    "§c☠ §6¡Tu energía ha sido drenada por la Guadaña del Ki!" :
                    "§c☠ §6¡Has sido atravesado por la Espada de Espíritu Final!" );
            attacker.getWorld ( ).playSound ( target.getLocation ( ), Sound.ENDERMAN_HIT, 1f, 0.5f );

            new BukkitRunnable ( ) {
                int taskTime = 0;

                @Override
                public void run () {
                    if (taskTime >= 5) {
                        this.cancel ( );
                        return;
                    }
                    if (target.isOnline ( ) && !target.isDead ( )) {
                        int hp = dbcTarget.getHP ( );
                        dbcTarget.setHP ( hp - (int) (hp * 0.01) ); // 1% cada segundo (total ~5%)
                        target.getWorld ( ).playEffect ( target.getLocation ( ),
                                org.bukkit.Effect.STEP_SOUND, 152 );
                    }
                    taskTime++;
                }
            }.runTaskTimer ( Main.instance, 0L, 20L );

            Bukkit.getScheduler ( ).runTaskLater ( Main.instance, () -> {
                // Drenado total 5% extra en stats
                dbcTarget.setHP ( (int) (dbcTarget.getHP ( ) * 0.95) );
                dbcTarget.setKi ( (int) (dbcTarget.getKi ( ) * 0.95) );
                dbcTarget.setStamina ( (int) (dbcTarget.getStamina ( ) * 0.95) );
                target.getWorld ( ).createExplosion ( target.getLocation ( ), 0F, false );
                spawnHologram ( target, "§e💥 ¡Energía absorbida!" );
            }, 100 );

            registrarUso ( attackerName, now, isImmune, isGuadana );
        }
    }

    private void registrarUso ( String playerName, long currentTime, boolean isImmune, boolean isGuadana ) {
        if (isImmune) return;

        if (isGuadana) {
            cooldowns.put ( playerName, currentTime ); // Cooldown directo de 5 min
            return;
        }

        // Lógica de usos para espada
        int usos = usosPrevios.getOrDefault ( playerName, 0 ) + 1;
        if (usos >= MAX_USOS_ANTES_COOLDOWN) {
            cooldowns.put ( playerName, currentTime );
            usosPrevios.put ( playerName, 0 );
        } else {
            usosPrevios.put ( playerName, usos );
        }
    }
}
