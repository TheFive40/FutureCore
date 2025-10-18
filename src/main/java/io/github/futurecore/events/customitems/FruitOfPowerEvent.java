package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FruitOfPowerEvent implements Listener {

    private final int itemId = 6071;
    public static final ConcurrentHashMap<UUID, LocalDateTime> fruitUsers = new ConcurrentHashMap<>();

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;
        if (item.getTypeId() != itemId) return;

        if (fruitUsers.containsKey(uuid)) {
            player.sendMessage(CC.translate("&cYa estás bajo el efecto de la &4Fruta del Poder&c."));
            return;
        }

        fruitUsers.put(uuid, LocalDateTime.now());
        player.sendMessage(CC.translate("&cEl efecto de la fruta del &4poder &cestá activo... Durante &415 minutos."));
        player.playSound(player.getLocation(), "jinryuudragonbc:1610.sss", 1.0F, 1.0F);

        if (item.getAmount() == 1) {
            player.setItemInHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
            player.setItemInHand(item);
        }
        event.setCancelled ( true );
    }

    public static void fruitOfPowerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Main.instance.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    if (fruitUsers.containsKey(uuid)) {
                        LocalDateTime startTime = fruitUsers.get(uuid);
                        Duration duration = Duration.between(startTime, LocalDateTime.now());
                        IDBCPlayer idbcPlayer = NpcAPI.Instance().getPlayer(player.getName()).getDBCPlayer();

                        if (duration.toMinutes() >= 5) {
                            fruitUsers.remove(uuid);
                            idbcPlayer.removeBonusAttribute ( "Strength", "FruitOfPower" );
                            player.sendMessage(CC.translate("&cEl efecto de la fruta del &4poder &cha expirado."));
                            player.playSound(player.getLocation(), "jinryuudragonbc:1610.sse", 1.0F, 1.0F);
                            idbcPlayer.setRelease ( (byte) 100 );
                            continue;
                        }
                        idbcPlayer.addBonusAttribute ( "Strength", "FruitOfPower", "*", 1.20, false );
                        idbcPlayer.addBonusAttribute ( "Strength", "Willpower", "*", 1.20, false );
                        idbcPlayer.setStamina((int) (idbcPlayer.getStamina() * 0.02 + idbcPlayer.getStamina()));
                    }
                }
            }
        }.runTaskTimer(Main.instance, 0L, 20L);
    }
}
