package io.github.futurecore.events.customitems;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.delaware.tools.CustomItems.CustomItems;
import org.delaware.tools.NbtHandler.NbtHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;

public class GoblinSwordEvent implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null) return;
        if(event.getAction () != Action.RIGHT_CLICK_AIR) return;
        try{
            if(CustomItems.getLinkedCustomItem ( itemInHand ) == null) return;
        }catch(Exception e) {return;}
        if (itemInHand.getTypeId() != 4273)  return;
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            if ((currentTime - lastUse) < 60000) {
                long secondsLeft = (60 - ((currentTime - lastUse) / 1000));
                General.spawnHologram(player,"§cDebes esperar " + secondsLeft + " s antes de volver a usar esta habilidad.");
                return;
            }
        }
        try {
            ItemStack item = player.getItemInHand ( );

            if (!hasPermanent ( item )) {
                ItemStack updated = CmdItemUsage.hasUses ( item, player );
                if (updated == null) {
                    player.setItemInHand ( null );
                } else {
                    player.setItemInHand ( updated );
                }
                player.updateInventory ( );
            }
        } catch (Exception ignored) {
        }
        cooldowns.put(playerId, currentTime);
        IDBCPlayer idbcPlayer = General.getDBCPlayer(player.getName());
        int hp = idbcPlayer.getHP();
        int hpBonus = (int) (hp * 0.25) + hp;
        idbcPlayer.setHP(hpBonus);
        General.spawnHologram(player, "&4❤ &aHaz recuperado un 25% de tu salud &4❤");
    }
}
