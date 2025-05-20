package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;

public class LightsaberEvent implements Listener {
    private static final int ITEM_ID = 4978;
    private static final int USOS_MAXIMOS = 10;
    private static final long COOLDOWN = 10 * 60 * 1000;

    private final Map<String, Integer> usosRestantes = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player defender = (Player) event.getEntity ();
        Player attacker = (event.getDamager() instanceof Player) ? (Player) event.getDamager() : null;
        String name = defender.getName();

        if (defender.getItemInHand() == null || defender.getItemInHand().getTypeId() != ITEM_ID) return;

        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(name)) {
            long lastUse = cooldowns.get(name);
            if (currentTime - lastUse < COOLDOWN) {
                int seconds = (int) ((COOLDOWN - (currentTime - lastUse)) / 1000) % 60;
                int minutes = (int) ((COOLDOWN - (currentTime - lastUse)) / 1000) / 60;
                defender.sendMessage("§c⌛ Sable Láser en cooldown: " + String.format("%02d:%02d", minutes, seconds));
                return;
            } else {
                usosRestantes.put(name, USOS_MAXIMOS);
                cooldowns.remove(name);
            }
        }

        usosRestantes.putIfAbsent(name, USOS_MAXIMOS);
        int usos = usosRestantes.get(name);

        if (usos <= 0) {
            cooldowns.put(name, currentTime);
            defender.sendMessage("§c⚠ Has agotado los 10 bloqueos. Cooldown de 10 minutos activado.");
            return;
        }

        if (Math.random() <= 0.3) {
            try {
                ItemStack item = defender.getItemInHand ( );

                if (!hasPermanent ( item )) {
                    ItemStack updated = CmdItemUsage.hasUses ( item, defender );
                    if (updated == null) {
                        defender.setItemInHand ( null );
                    } else {
                        defender.setItemInHand ( updated );
                    }
                    defender.updateInventory ( );
                }
            } catch (Exception ignored) {
            }
            event.setCancelled(true);
            usosRestantes.put(name, usos - 1);
            defender.sendMessage("§a🛡 ¡Sable Láser bloqueó el ataque! Usos restantes: §f" + (usos - 1));
            defender.getWorld().playEffect(defender.getLocation(), Effect.BLAZE_SHOOT, 0);

            if (attacker != null && attacker.isOnline() && !attacker.isDead()) {
                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (attacker.isOnline() && !attacker.isDead()) {
                            IDBCPlayer player = General.getDBCPlayer ( attacker.getName () );
                            double damage = player.getHP () * 0.05;
                            player.setHP ( (int) (player.getHP () - damage) );
                            attacker.getWorld().playEffect(attacker.getLocation(), Effect.STEP_SOUND, 152);
                        }
                        if (++ticks >= 3) cancel();
                    }
                }.runTaskTimer( Main.instance, 0L, 20L);
            }

            if (usos - 1 == 0) {
                cooldowns.put(name, currentTime);
                defender.sendMessage("§c⚠ Has agotado los 10 bloqueos. Cooldown de 10 minutos activado.");
            }
        }
    }
}
