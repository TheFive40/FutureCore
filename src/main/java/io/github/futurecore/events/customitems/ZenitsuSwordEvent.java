package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.utils.General.spawnHologram;

public class ZenitsuSwordEvent implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final int COOLDOWN_TIME = 30;

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getItemInHand();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (itemInHand == null) return;
        if (itemInHand.getTypeId() != 6132) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            if ((currentTime - lastUse) < (COOLDOWN_TIME * 1000)) {
                long millisLeft = (lastUse + COOLDOWN_TIME * 1000) - currentTime;
                long secondsLeft = millisLeft / 1000;
                long minutes = secondsLeft / 60;
                long seconds = secondsLeft % 60;

                String cooldownMessage = "&cDebes esperar &4" + minutes + "m " + seconds + "s ";
                spawnHologram(player, cooldownMessage);
                return;
            }
        }
        try {
            ItemStack item = player.getItemInHand();

            if (!hasPermanent(item)) {
                ItemStack updated = CmdItemUsage.hasUses(item, player);
                if (updated == null) {
                    player.setItemInHand(null);
                } else {
                    player.setItemInHand(updated);
                }
                player.updateInventory();
            }
        } catch (Exception ignored) {
        }
        cooldowns.put(playerId, currentTime);
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 10);

        for (Entity entity : player.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity target = (LivingEntity) entity;
                Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                target.setVelocity(direction.multiply(3.5));
                target.setFireTicks(160);

                if (target instanceof Player) {
                    Player playerTarget = (Player) target;
                    IDBCPlayer idbcPlayer = General.getDBCPlayer(playerTarget.getName());
                    if (idbcPlayer != null) {
                        idbcPlayer.setKi((int) (idbcPlayer.getKi() - idbcPlayer.getKi() * 0.5));
                    }
                }
            }
        }
        spawnHologram(player, "&6¡Lanzaste una &eLlamarada Solar&6!");
    }
}
