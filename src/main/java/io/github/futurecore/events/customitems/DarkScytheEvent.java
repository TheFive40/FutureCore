package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.delaware.tools.CC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class DarkScytheEvent implements Listener {

    private final Map<UUID, Integer> scytheUses = new HashMap<> ( );
    private final Map<UUID, Long> cooldowns = new HashMap<> ( );
    private static final int MAX_USES = 5;
    private static final int COOLDOWN_SECONDS = 300;

    @EventHandler
    public void onHit ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;

        Player player = (Player) event.getDamager ( );
        ItemStack item = player.getItemInHand ( );

        if (item == null || item.getTypeId ( ) != 5014) return;

        UUID playerId = player.getUniqueId ( );
        long currentTime = System.currentTimeMillis ( );

        if (cooldowns.containsKey ( playerId )) {
            long lastCooldown = cooldowns.get ( playerId );
            if ((currentTime - lastCooldown) < (COOLDOWN_SECONDS * 1000)) {
                long millisLeft = (lastCooldown + COOLDOWN_SECONDS * 1000) - currentTime;
                long secondsLeft = millisLeft / 1000;
                long minutes = secondsLeft / 60;
                long seconds = secondsLeft % 60;

                spawnHologram ( player, CC.translate ( "&cDebes esperar &4" + minutes + "m " + seconds + "s &cpara usar nuevamente la &8Guadaña Oscura&c." ) );
                return;
            } else {
                scytheUses.put ( playerId, 0 );
                cooldowns.remove ( playerId );
            }
        }
        scytheUses.putIfAbsent ( playerId, 0 );

            LivingEntity target = (LivingEntity) event.getEntity ( );
            if (target instanceof Player) {
                if (Math.random ( ) <= 0.1) {
                    try {
                         item = player.getItemInHand();

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
                    Player targetPlayer = (Player) target;
                    IDBCPlayer targetDBC = General.getDBCPlayer ( targetPlayer.getName ( ) );
                    target.getWorld ( ).strikeLightningEffect ( target.getLocation ( ) );
                    targetDBC.setHP ( 0 );
                    spawnHologram ( player, CC.translate ( "&8☠ &7Has segado el alma de tu enemigo..." ) );
                    incrementUse ( playerId, player );
                }
            } else {
                if (Math.random ( ) <= 0.02) {
                    IWorld world = NpcAPI.Instance ( ).getPlayer ( ((Player) event.getDamager ()).getName () )
                            .getWorld ();
                    IEntity<?> iEntity = world.getEntityByID ( event.getEntity ( ).getEntityId ( ) );
                    if (iEntity instanceof ICustomNpc<?>) {
                        try {
                            item = player.getItemInHand();

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
                        ICustomNpc<?> npc = (ICustomNpc<?>) world.getEntityByID ( target.getEntityId ( ) );
                        npc.getWorld ( ).thunderStrike ( npc.getPosition ( ) );
                        npc.setHealth ( 0 );
                        spawnHologram ( player, CC.translate ( "&8☠ &7Has segado un alma oscura..." ) );
                        incrementUse ( playerId, player );
                    }
                }
        }
    }

    private void incrementUse ( UUID playerId, Player player ) {
        int uses = scytheUses.getOrDefault ( playerId, 0 ) + 1;
        scytheUses.put ( playerId, uses );

        if (uses >= MAX_USES) {
            cooldowns.put ( playerId, System.currentTimeMillis ( ) );
            spawnHologram ( player, CC.translate ( "&c¡Has usado todas las cargas! La Guadaña Oscura entra en reposo..." ) );
        }
    }
}
