package io.github.futurecore.events.bukkit;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.futurecore.Main;
import io.github.futurecore.utils.RegionUtils;
import io.github.futurecore.utils.data.KairosData.PKairos;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.util.HashMap;
import java.util.UUID;

public class PlayerDeath implements Listener {
    private static final HashMap<UUID, UUID> damageByPlayer = new HashMap<> ( );

    @EventHandler
    public void onDamage ( EntityDamageByEntityEvent event ) {
        if (!(event.getDamager ( ) instanceof Player)) return;
        if (!(event.getEntity ( ) instanceof Player)) return;

        damageByPlayer.put ( event.getEntity ( ).getUniqueId ( ), event.getDamager ( ).getUniqueId ( ) );
    }

    @EventHandler
    public void onDeath ( PlayerDeathEvent event ) {
        RegionUtils regionUtils = new RegionUtils ( );
        ProtectedRegion protectedRegion = regionUtils.getRegionAtLocation ( event.getEntity ( ).getLocation ( ) );
        if (protectedRegion == null) return;
        Location loc = event.getEntity().getLocation();
        if (!(RegionUtils.isLocationInRegion(loc, "dungeon_1")
                || RegionUtils.isLocationInRegion(loc, "dungeon_2")
                || RegionUtils.isLocationInRegion(loc, "dungeon_3")
                || RegionUtils.isLocationInRegion(loc, "dungeon_4")
                || RegionUtils.isLocationInRegion(loc, "dungeon_5"))) return;

        if (damageByPlayer.containsKey ( event.getEntity ( ).getUniqueId ( ) )) {
            UUID playerUUID = damageByPlayer.get ( event.getEntity ( ).getUniqueId ( ) );
            Player player = Main.instance.getServer ( ).getPlayer ( playerUUID );
            PKairos pkairosDamager = new PKairos ( player.getUniqueId ( ) );
            PKairos pKairos = new PKairos ( event.getEntity ( ).getPlayer ( ).getUniqueId ( ) );
            if (pKairos.getAmount ( ) >= 20) {
                pKairos.pay ( -20 );
                pkairosDamager.pay ( 20 );
            } else if (pKairos.getAmount ( ) >= 10) {
                pKairos.pay ( -10 );
                pkairosDamager.pay ( 10 );
            } else if (pKairos.getAmount ( ) >= 2) {
                pKairos.pay ( -2 );
                pkairosDamager.pay ( 2 );
            }
        }
    }

}
