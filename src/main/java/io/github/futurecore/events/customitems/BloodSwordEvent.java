package io.github.futurecore.events.customitems;
import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import java.util.HashSet;
import java.util.Set;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class BloodSwordEvent implements Listener {

    private final Set<Player> sangrando = new HashSet<>();

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player atacante = (Player) event.getDamager();
        Player victima = (Player) event.getEntity();

        if (atacante.getItemInHand() == null) return;
        if (atacante.getItemInHand().getTypeId() != 6154) return;
        if (sangrando.contains(victima)) {
            return;
        }
        try {
            ItemStack item = atacante.getItemInHand();

            if (!hasPermanent(item)) {
                ItemStack updated = CmdItemUsage.hasUses(item, atacante);
                if (updated == null) {
                    atacante.setItemInHand(null);
                } else {
                    atacante.setItemInHand(updated);
                }
                atacante.updateInventory();
            }
        } catch (Exception ignored) {
        }
        spawnHologram(atacante, "&4 rovocaste sangrado en &c" + victima.getName() + "!");
        victima.sendMessage(ChatColor.RED + "¡Estás sangrando!");
        startBleeding(victima, atacante);
    }

    private void startBleeding(Player victima, Player atacante) {
        sangrando.add(victima);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 2) {
                    sangrando.remove(victima);
                    this.cancel();
                    return;
                }

                if (!victima.isOnline() || victima.isDead()) {
                    sangrando.remove(victima);
                    this.cancel();
                    return;
                }
                IDBCPlayer idbcPlayer = General.getDBCPlayer ( victima.getName () );
                double vidaActual = idbcPlayer.getHP ();
                double dano = vidaActual * 0.04;
                int nuevaVida = (int) Math.max(0, vidaActual - dano);
                idbcPlayer.setHP (nuevaVida);
                ticks++;
            }
        }.runTaskTimer(Main.instance, 0L, 20L);
    }
}


