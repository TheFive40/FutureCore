package io.github.futurecore.events.customitems;

import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class DevilScytheEvent implements Listener {

    private static final Map<String, Long> cooldowns = new HashMap<>();
    private static final List<String> IMMUNE_PLAYERS = Arrays.asList("DelawareX", "TheFive", "jean_sama");
    private static final long COOLDOWN_TIME = 30 * 60 * 1000;

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {

        Player lanzador = event.getPlayer();
        String playerName = lanzador.getName();

        if (lanzador.getItemInHand() == null || lanzador.getItemInHand().getTypeId() != 6091) return;

        if (!IMMUNE_PLAYERS.contains(playerName)) {
            long currentTime = System.currentTimeMillis();
            if (cooldowns.containsKey(playerName)) {
                long lastUsed = cooldowns.get(playerName);
                if (currentTime - lastUsed < COOLDOWN_TIME) {
                    long timeLeftMillis = COOLDOWN_TIME - (currentTime - lastUsed);
                    long seconds = (timeLeftMillis / 1000) % 60;
                    long minutes = (timeLeftMillis / 1000) / 60;
                    String formatted = String.format("%02d:%02d", minutes, seconds);
                    spawnHologram (lanzador,"§c⌛ Debes esperar " + formatted + " minutos para volver a usarla");
                    return;
                }
            }
            cooldowns.put(playerName, currentTime);
        }
        try {
            ItemStack item = lanzador.getItemInHand();

            if (!hasPermanent(item)) {
                ItemStack updated = CmdItemUsage.hasUses(item, lanzador);
                if (updated == null) {
                    lanzador.setItemInHand(null);
                } else {
                    lanzador.setItemInHand(updated);
                }
                lanzador.updateInventory();
            }
        } catch (Exception ignored) {
        }

        for (Entity nearby : lanzador.getNearbyEntities(5, 5, 5)) {
            IWorld world = NpcAPI.Instance().getPlayer(playerName).getWorld();
            IEntity<?> iEntity = world.getEntityByID(nearby.getEntityId());
            if (iEntity instanceof ICustomNpc<?>) {
                ICustomNpc<?> npc = (ICustomNpc<?>) iEntity;
                npc.setHealth(npc.getHealth() - (float) (npc.getHealth() * 0.5));
                npc.getWorld().thunderStrike(npc.getPosition());
                spawnHologram(lanzador, "§4☠ §c¡El alma de tu enemigo fue corrompida por la Guadaña Demoníaca!");
            }
        }
    }
}
