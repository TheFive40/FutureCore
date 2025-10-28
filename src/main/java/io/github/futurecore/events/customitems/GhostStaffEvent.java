package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import noppes.npcs.api.entity.IDBCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

/**
 * Báculo Espectral
 * Habilidad (Click Derecho): Invoca un ejército de almas fantasmales que drenan
 * energía de todos los enemigos cercanos y te vuelve intangible por 5 segundos
 */
public class GhostStaffEvent implements Listener {

  private static final int ITEM_ID = 6252;
  private static final long COOLDOWN = 12 * 60 * 1000; // 12 minutos
  private static final double RADIO = 8.0;

  private final Map<UUID, Long> cooldowns = new HashMap<>();
  private final Set<UUID> jugadoresIntangibles = new HashSet<>();

  @EventHandler
  public void onRightClick(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR &&
      event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Player invocador = event.getPlayer();
    ItemStack item = invocador.getItemInHand();

    if (item == null || item.getTypeId() != ITEM_ID) return;

    UUID invocadorId = invocador.getUniqueId();
    long ahora = System.currentTimeMillis();

    if (cooldowns.containsKey(invocadorId)) {
      long restante = COOLDOWN - (ahora - cooldowns.get(invocadorId));
      if (restante > 0) {
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram(invocador, String.format("§f👻 Las almas descansan: %02d:%02d", minutos, segundos));
        return;
      }
    }

    try {
      if (!hasPermanent(item)) {
        ItemStack updated = CmdItemUsage.hasUses(item, invocador);
        if (updated == null) {
          invocador.setItemInHand(null);
          return;
        } else {
          invocador.setItemInHand(updated);
        }
        invocador.updateInventory();
      }
    } catch (Exception ignored) {
    }

    cooldowns.put(invocadorId, ahora);
    invocarEjercitoEspectral(invocador, invocadorId);
  }

  private void invocarEjercitoEspectral(Player invocador, UUID invocadorId) {
    jugadoresIntangibles.add(invocadorId);

    // Efectos del invocador (forma espectral)
    invocador.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 5, 0));
    invocador.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 5, 2));
    invocador.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 1));

    invocador.getWorld().playSound(invocador.getLocation(), Sound.GHAST_MOAN, 1.5f, 0.5f);
    spawnHologram(invocador, "§f👻 §7¡EJÉRCITO ESPECTRAL INVOCADO!");

    IDBCPlayer dbcInvocador = General.getDBCPlayer(invocador.getName());
    int energiaTotal = 0;

    // Drenar energía de todos los enemigos cercanos
    for (Entity entidad : invocador.getNearbyEntities(RADIO, RADIO, RADIO)) {
      if (entidad instanceof Player && !entidad.equals(invocador)) {
        Player enemigo = (Player) entidad;
        IDBCPlayer dbcEnemigo = General.getDBCPlayer(enemigo.getName());

        // Drenar HP, Ki y Stamina
        int hpDrenado = (int) (dbcEnemigo.getHP() * 0.08);
        int kiDrenado = (int) (dbcEnemigo.getKi() * 0.15);
        int staminaDrenada = (int) (dbcEnemigo.getStamina() * 0.12);

        dbcEnemigo.setHP(Math.max(1, dbcEnemigo.getHP() - hpDrenado));
        dbcEnemigo.setKi(Math.max(0, dbcEnemigo.getKi() - kiDrenado));
        dbcEnemigo.setStamina(Math.max(0, dbcEnemigo.getStamina() - staminaDrenada));

        energiaTotal += (hpDrenado + kiDrenado / 2);

        // Efectos en enemigos
        enemigo.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1));
        enemigo.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 5, 0));
        enemigo.getWorld().playSound(enemigo.getLocation(), Sound.GHAST_SCREAM, 1f, 1.5f);

        spawnHologram(enemigo, "§f👻 §7¡Almas espectrales te drenan!");
      }
    }

    // El invocador recupera parte de la energía drenada
    if (energiaTotal > 0) {
      int vidaRecuperada = (int) (energiaTotal * 0.6);
      dbcInvocador.setHP(Math.min(dbcInvocador.getBody(), dbcInvocador.getHP() + vidaRecuperada));
      spawnHologram(invocador, "§a+ " + vidaRecuperada + " HP de energía espectral");
    }

    // Efecto visual continuo
    new BukkitRunnable() {
      int ticks = 0;

      @Override
      public void run() {
        if (ticks >= 5 || !invocador.isOnline()) {
          jugadoresIntangibles.remove(invocadorId);
          if (invocador.isOnline()) {
            spawnHologram(invocador, "§7Las almas regresan al más allá...");
          }
          this.cancel();
          return;
        }

        invocador.getWorld().playEffect(invocador.getLocation(),
          org.bukkit.Effect.ENDER_SIGNAL, 0);

        ticks++;
      }
    }.runTaskTimer(Main.instance, 0L, 20L);
  }
}
