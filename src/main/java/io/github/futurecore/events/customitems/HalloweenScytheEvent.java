package io.github.futurecore.events.customitems;
import io.github.futurecore.Main;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

/**
 * Guadaña de la Cosecha Oscura
 * Habilidad: Al golpear, tiene 20% de probabilidad de drenar vida y robar el alma del enemigo,
 * causando ceguera y debilidad mientras recupera HP del atacante
 */
public class HalloweenScytheEvent implements Listener {

  private static final int ITEM_ID = 4303; // ID de la Guadaña de Halloween
  private static final double PROBABILIDAD_COSECHA = 0.20;
  private static final long COOLDOWN = 5 * 60 * 1000; // 5 minutos

  private final Map<UUID, Long> cooldowns = new HashMap<>();
  private final Set<UUID> almasCosechadas = new HashSet<>();

  @EventHandler
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player)) return;
    if (!(event.getEntity() instanceof Player)) return;

    Player atacante = (Player) event.getDamager();
    Player victima = (Player) event.getEntity();

    ItemStack arma = atacante.getItemInHand();
    if (arma == null || arma.getTypeId() != ITEM_ID) return;

    UUID uuid = atacante.getUniqueId();
    long ahora = System.currentTimeMillis();

    if (cooldowns.containsKey(uuid)) {
      long restante = COOLDOWN - (ahora - cooldowns.get(uuid));
      if (restante > 0) {
        int minutos = (int) (restante / 1000) / 60;
        int segundos = (int) (restante / 1000) % 60;
        spawnHologram(atacante, String.format("§6🎃 La Guadaña aún se alimenta: %02d:%02d", minutos, segundos));
        return;
      }
    }

    if (Math.random() <= PROBABILIDAD_COSECHA) {
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

      cooldowns.put(uuid, ahora);
      almasCosechadas.add(victima.getUniqueId());

      IDBCPlayer dbcVictima = General.getDBCPlayer(victima.getName());
      IDBCPlayer dbcAtacante = General.getDBCPlayer(atacante.getName());

      int vidaDrenada = (int) (dbcVictima.getHP() * 0.15);
      dbcVictima.setHP(dbcVictima.getHP() - vidaDrenada);

      int vidaRecuperada = (int) (vidaDrenada * 0.70);
      dbcAtacante.setHP(Math.min(dbcAtacante.getHP() + vidaRecuperada, dbcAtacante.getBody()));

      victima.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 8, 1));
      victima.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 8, 1));
      victima.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 8, 0));

      victima.getWorld().playSound(victima.getLocation(), Sound.GHAST_SCREAM, 1f, 0.5f);
      atacante.getWorld().playSound(atacante.getLocation(), Sound.WITHER_SPAWN, 0.7f, 1.5f);

      spawnHologram(victima, "§4☠ §c¡Tu alma está siendo cosechada!");
      spawnHologram(atacante, "§6🎃 §e¡Has cosechado un alma! +" + vidaRecuperada + " HP");

      new BukkitRunnable() {
        int ticks = 0;

        @Override
        public void run() {
          if (ticks >= 8 || !victima.isOnline() || victima.isDead()) {
            almasCosechadas.remove(victima.getUniqueId());
            this.cancel();
            return;
          }

          IDBCPlayer dbc = General.getDBCPlayer(victima.getName());
          int dano = (int) (dbc.getHP() * 0.01); // 1% cada segundo
          dbc.setHP(dbc.getHP() - dano);

          victima.getWorld().playEffect(victima.getLocation(),
            org.bukkit.Effect.STEP_SOUND, 87); // Soul Sand

          ticks++;
        }
      }.runTaskTimer(Main.instance, 0L, 20L);
    }
  }
}
