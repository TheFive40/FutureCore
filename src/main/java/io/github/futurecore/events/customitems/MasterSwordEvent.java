package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import net.minecraft.server.v1_7_R4.PacketPlayOutWorldParticles;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.utils.General.spawnHologram;

public class MasterSwordEvent implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 20 * 60 * 1000; // 20 minutos
    private static final Set<String> IMMUNE_PLAYERS = new HashSet<String> () {{
        add("DelawareX");
        add("TheFive");
        add("jean_sama");
    }};

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Player target = (Player) event.getEntity();
        ItemStack itemInHand = attacker.getItemInHand();

        if (itemInHand == null || itemInHand.getTypeId() != 6134) return;

        UUID attackerId = attacker.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (!IMMUNE_PLAYERS.contains(attacker.getName())) {
            if (cooldowns.containsKey(attackerId)) {
                long lastUse = cooldowns.get(attackerId);
                if ((currentTime - lastUse) < COOLDOWN_TIME) {
                    long millisLeft = (lastUse + COOLDOWN_TIME) - currentTime;
                    long secondsLeft = millisLeft / 1000;
                    long minutes = secondsLeft / 60;
                    long seconds = secondsLeft % 60;

                    spawnHologram(attacker, "&7✦ &cJuicio Redentor en cooldown: &4" + minutes + "m " + seconds + "s &7✦");
                    return;
                }
            }
            cooldowns.put(attackerId, currentTime);
        }
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
        IDBCPlayer idbcTarget = General.getDBCPlayer(target.getName());
        byte alignment = idbcTarget.getNbt().getCompound ( "PlayerPersisted" ).getByte("jrmcAlign");
        Location loc = target.getLocation();
        if (alignment >= 32 && alignment < 67) {
            idbcTarget.setHP((int) (idbcTarget.getHP() - idbcTarget.getHP() * 0.50));
            mostrarParticulas(target, "crit", (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 0.3f, 0.5f, 0.3f, 0.01f, 15);
            loc.getWorld().playSound(loc, Sound.ANVIL_BREAK, 1f, 0.6f);
        } else if (alignment < 32) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmcse set Majin 0 " + target.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmcse set legendary 0 " + target.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "jrmcse set divine 0 " + target.getName());
            idbcTarget.setHP((int) (idbcTarget.getHP() - idbcTarget.getHP() * 0.75));
            loc.getWorld().strikeLightningEffect(loc);
            loc.getWorld().playSound(loc, Sound.ENDERDRAGON_GROWL, 1f, 0.6f);
            mostrarParticulas(target, "lava", (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 0.3f, 0.5f, 0.3f, 0.01f, 15);
        } else {
            idbcTarget.setHP((int) (idbcTarget.getHP() - idbcTarget.getHP() * 0.25));
            loc.getWorld().playSound(loc, Sound.NOTE_BASS, 1f, 0.8f);
            mostrarParticulas(target, "flame", (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 0.3f, 0.5f, 0.3f, 0.01f, 15);
        }

        spawnHologram(attacker, "&b✧ &9¡Juicio del Redentor ha sido ejecutado! &b✧");
    }
    public void mostrarParticulas(Player jugador, String tipo, float x, float y, float z, float offsetX, float offsetY, float offsetZ, float velocidad, int cantidad) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(
                tipo, x, y, z, offsetX, offsetY, offsetZ, velocidad, cantidad
        );
        ((CraftPlayer) jugador).getHandle().playerConnection.sendPacket(packet);
    }
}
