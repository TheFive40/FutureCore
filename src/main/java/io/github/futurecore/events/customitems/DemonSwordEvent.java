package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.delaware.tools.CustomItems.CustomItems;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class DemonSwordEvent implements Listener {

    private static final int ITEM_ID = 4323;
    private static final double PROBABILIDAD_MALDICION = 0.25;
    private static final long COOLDOWN = 3 * 60 * 1000;

    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final Set<String> JUGADORES_INMUNES = new HashSet<>(Arrays.asList(
            "DelawareX", "TheFive", "jean_sama"
    ));

    private static final PotionEffectType[] MALEDICCIONES = new PotionEffectType[]{
            PotionEffectType.POISON,
            PotionEffectType.SLOW,
            PotionEffectType.BLINDNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.SLOW_DIGGING
    };

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player atacante = (Player) event.getDamager();
        Player objetivo = (Player) event.getEntity();

        ItemStack arma = atacante.getItemInHand();
        if (arma == null || arma.getTypeId() != ITEM_ID) return;
        if(CustomItems.getLinkedCustomItem ( arma ) == null) return;
        boolean esInmune = JUGADORES_INMUNES.contains(atacante.getName());
        UUID uuid = atacante.getUniqueId();
        long ahora = System.currentTimeMillis();

        if (!esInmune) {
            if (cooldowns.containsKey(uuid)) {
                long restante = COOLDOWN - (ahora - cooldowns.get(uuid));
                if (restante > 0) {
                    int segundos = (int) (restante / 1000) % 60;
                    int minutos = (int) (restante / 1000) / 60;
                    String msg = String.format("§c⌛ La espada demoníaca está en enfriamiento: %02d:%02d", minutos, segundos);
                    spawnHologram(atacante, msg);
                    return;
                }
            }
        }

        if (Math.random () <= PROBABILIDAD_MALDICION) {
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
            PotionEffectType maldicion = MALEDICCIONES[random.nextInt(MALEDICCIONES.length)];
            objetivo.addPotionEffect(new PotionEffect(maldicion, 20 * 10, 1));
            IDBCPlayer idbcPlayer = General.getDBCPlayer(objetivo.getName());
            idbcPlayer.setHP((int) (idbcPlayer.getHP() - idbcPlayer.getBody() * 0.04));
            objetivo.getWorld().playSound(objetivo.getLocation(), Sound.AMBIENCE_CAVE, 1f, 0.5f);
            spawnHologram(objetivo, "§5✴ §c¡Has sido maldito por la Espada Demoníaca!");

            if (!esInmune) {
                cooldowns.put(uuid, ahora);
            }
        }
    }
}
