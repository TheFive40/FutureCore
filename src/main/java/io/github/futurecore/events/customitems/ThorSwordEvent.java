package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static io.github.futurecore.utils.General.spawnHologram;

public class ThorSwordEvent implements Listener {

    private final Random random = new Random();
    private final int itemId = 6170;

    private final long cooldown = 15 * 60 * 1000; // 15 minutos
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Player target = (Player) event.getEntity();

        if (attacker.getItemInHand() == null || attacker.getItemInHand().getTypeId() != itemId) return;

        UUID attackerId = attacker.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(attackerId)) {
            long lastUse = cooldowns.get(attackerId);
            if (now - lastUse < cooldown) {
                long timeLeft = (cooldown - (now - lastUse)) / 1000;
                long minutes = timeLeft / 60;
                long seconds = timeLeft % 60;
                spawnHologram(attacker, "§c⚡ La furia de Thor aún se recarga (" + minutes + "m " + seconds + "s)");
                return;
            }
        }

        if (random.nextDouble() >= 0.5) return;

        cooldowns.put(attackerId, now);

        target.setVelocity(new Vector(0, 0, 0));
        target.setWalkSpeed(0f);
        spawnHologram(target, "§b⚡ ¡Has sido alcanzado por la furia de Thor!");
        spawnHologram(attacker, "§e⚡ Invocaste el juicio del trueno sobre " + target.getName() + "!");

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 20 || target.isDead()) {
                    target.setWalkSpeed(0.2f);
                    cancel();
                    return;
                }

                Location loc = target.getLocation();
                loc.getWorld().strikeLightning(loc);

                IDBCPlayer targetDBC = General.getDBCPlayer(target.getName());
                targetDBC.setHP((int) (targetDBC.getHP() - (targetDBC.getHP() * 0.1)));

                count++;
            }
        }.runTaskTimer(Main.instance, 0L, 10L);
    }
}
