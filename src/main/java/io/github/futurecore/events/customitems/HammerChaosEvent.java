package io.github.futurecore.events.customitems;

import com.gmail.filoghost.holograms.api.Hologram;
import com.gmail.filoghost.holograms.api.HolographicDisplaysAPI;
import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import net.minecraft.server.v1_7_R4.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.delaware.tools.CC;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;

public class HammerChaosEvent implements Listener {

    private final Map<UUID, Double> jumpStartY = new HashMap<>();
    private final Map<UUID, Double> jumpPeakY = new HashMap<>();
    private final Map<UUID, Instant> cooldowns = new HashMap<>();

    private final Duration COOLDOWN = Duration.ofMinutes(15);

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        double fromY = event.getFrom().getY();
        double toY = event.getTo().getY();
        ItemStack item = player.getItemInHand ();
        if (item == null) return;
        if(item.getTypeId () != 5006) return;
        if (!jumpStartY.containsKey(playerUUID) && toY > fromY) {
            jumpStartY.put(playerUUID, fromY);
        }

        if (jumpStartY.containsKey(playerUUID) && toY > fromY) {
            jumpPeakY.put(playerUUID, toY);
        }

        if (player.isOnGround() && player.isSneaking()
                && jumpPeakY.containsKey(playerUUID) && jumpStartY.containsKey(playerUUID)) {

            double startY = jumpStartY.get(playerUUID);
            double peakY = jumpPeakY.get(playerUUID);

            if ((peakY - startY) >= 3.0) {
                Instant now = Instant.now();
                Instant lastUse = cooldowns.get(playerUUID);

                if (lastUse == null || Duration.between(lastUse, now).compareTo(COOLDOWN) >= 0) {
                    cooldowns.put(playerUUID, now);
                    triggerHammerEffect(player);
                } else {
                    Duration remaining = COOLDOWN.minus(Duration.between(lastUse, now));
                    long minutes = remaining.toMinutes();
                    long seconds = remaining.minusMinutes(minutes).getSeconds();
                    spawnHologram (player,"§cDebes esperar " + minutes + "m " + seconds + "s");
                }
            }

            jumpStartY.remove(playerUUID);
            jumpPeakY.remove(playerUUID);
        }
    }
    public static void spawnHologram ( Player player, String text ) {
        Location loc = player.getLocation ( );
        loc.setY ( loc.getY ( ) + 1.5 );
        loc.setZ ( loc.getZ () + 1.0 );
        Hologram hologram = HolographicDisplaysAPI.createHologram ( Main.instance, loc, CC.translate ( text ) );
        Bukkit.getScheduler ( ).runTaskLater ( Main.instance, hologram::delete, 20L );
    }
    private void triggerHammerEffect(Player player) {
        ItemStack item = player.getItemInHand();
        if (item == null || item.getTypeId() != 5006) return;

        Location loc = player.getLocation();
        double radius = 5.0;
        try {
             item = player.getItemInHand ( );

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
        for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Player && !nearby.equals(player)) {
                Player target = (Player) nearby;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmcse set KO 0.2 " + target.getName());
                target.getWorld().createExplosion(target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ(), 0.5F, true, false);
            }
        }

        mostrarParticulas(
                player, "hugeexplosion",
                (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                0.3f, 0.5f, 0.3f, 0.01f, 15
        );
        player.getWorld().createExplosion(loc, 0F);
    }

    public void mostrarParticulas(Player jugador, String tipo, float x, float y, float z, float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
        );

        ((CraftPlayer) jugador).getHandle().playerConnection.sendPacket(packet);
    }
}
