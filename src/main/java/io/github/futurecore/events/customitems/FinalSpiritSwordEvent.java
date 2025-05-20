package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.scripted.NpcAPI;
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
    private static final long COOLDOWN = 6 * 60 * 1000;
    private static final int MAX_USOS_ANTES_COOLDOWN = 3;

    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Integer> usosPrevios = new HashMap<>();
    private final Random random = new Random();

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        ItemStack weapon = attacker.getItemInHand();
        if (weapon == null || weapon.getTypeId() != ITEM_ID) return;

        String attackerName = attacker.getName();
        long now = System.currentTimeMillis();
        boolean isImmune = attackerName.equalsIgnoreCase("DelawareX") ||
                attackerName.equalsIgnoreCase("TheFive") ||
                attackerName.equalsIgnoreCase("jean_sama");

        if (!isImmune && cooldowns.containsKey(attackerName)) {
            long lastUse = cooldowns.get(attackerName);
            if (now - lastUse < COOLDOWN) {
                long remaining = COOLDOWN - (now - lastUse);
                int minutes = (int) (remaining / 1000) / 60;
                int seconds = (int) (remaining / 1000) % 60;
                spawnHologram(attacker, String.format("§e⌛ Espada en cooldown: %02d:%02d", minutes, seconds));
                return;
            }
        }

        if (random.nextDouble() <= PROBABILIDAD) {
            try {
                ItemStack item = attacker.getItemInHand ( );

                if (!hasPermanent ( item )) {
                    ItemStack updated = CmdItemUsage.hasUses ( item, attacker );
                    if (updated == null) {
                        attacker.setItemInHand ( null );
                    } else {
                        attacker.setItemInHand ( updated );
                    }
                    attacker.updateInventory ( );
                }
            } catch (Exception ignored) {
            }
            IWorld world = NpcAPI.Instance().getPlayer(attackerName).getWorld();
            IEntity<?> iEntity = world.getEntityByID(event.getEntity().getEntityId());

            if (iEntity instanceof ICustomNpc<?>) {
                ICustomNpc<?> npc = (ICustomNpc<?>) iEntity;
                npc.setHealth((float) (npc.getHealth() * 0.75));
                attacker.getWorld().playSound(attacker.getLocation (), Sound.WITHER_HURT, 1f, 1f);
                spawnHologram(attacker, "§6⚔ §e¡Golpe de Espíritu Final!");
                registrarUso(attackerName, now, isImmune);
                return;
            }

            Player target = (Player) event.getEntity();
            IDBCPlayer dbcTarget = General.getDBCPlayer(target.getName());

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 10)); // inmoviliza
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 5, 128)); // sin salto

            spawnHologram(target, "§c☠ §6¡Has sido atravesado por la Espada de Espíritu Final!");
            attacker.getWorld().playSound(target.getLocation(), Sound.ENDERMAN_HIT, 1f, 0.5f);

            new BukkitRunnable () {
                int taskTime = 0;

                @Override
                public void run() {
                    if (taskTime >= 5) {
                        this.cancel();
                        return;
                    }
                    if (target.isOnline() && !target.isDead()) {
                        int hp = dbcTarget.getHP();
                        dbcTarget.setHP(hp - (int) (hp * 0.02));
                        target.getWorld().playEffect(target.getLocation(), org.bukkit.Effect.STEP_SOUND, 152); // Partículas sangre
                    }
                    taskTime++;
                }
            }.runTaskTimer(Main.instance, 0L, 20L);


            Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
                dbcTarget.setHP((int) (dbcTarget.getHP() - dbcTarget.getHP() * 0.18));
                dbcTarget.setKi((int) (dbcTarget.getKi() * 0.6));
                dbcTarget.setStamina((int) (dbcTarget.getStamina() * 0.88));
                target.getWorld().createExplosion(target.getLocation(), 0F, false);
                spawnHologram(target, "§e💥 ¡Explosión del Espíritu Final!");
            }, 100);
            registrarUso(attackerName, now, isImmune);
        }
    }

    private void registrarUso(String playerName, long currentTime, boolean isImmune) {
        if (isImmune) return;

        int usos = usosPrevios.getOrDefault(playerName, 0) + 1;
        if (usos >= MAX_USOS_ANTES_COOLDOWN) {
            cooldowns.put(playerName, currentTime);
            usosPrevios.put(playerName, 0);
        } else {
            usosPrevios.put(playerName, usos);
        }
    }
}
