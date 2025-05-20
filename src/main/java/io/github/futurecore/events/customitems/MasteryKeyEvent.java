package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import io.github.futurecore.commands.player.cmdUsages.CmdItemUsage;
import io.github.futurecore.utils.General;
import kamkeel.npcdbc.constants.DBCClass;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.futurecore.commands.player.cmdUsages.CmdItemUsage.hasPermanent;
import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class MasteryKeyEvent implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 10 * 60 * 1000; // 10 minutos

    @EventHandler
    public void onClick(PlayerInteractEntityEvent event) {
        Player lanzador = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (lanzador.getItemInHand().getTypeId() != 6152) return;

        UUID uuid = lanzador.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long lastUse = cooldowns.get(uuid);
            long elapsed = now - lastUse;

            if (elapsed < COOLDOWN_MS) {
                long remaining = COOLDOWN_MS - elapsed;
                int seconds = (int) (remaining / 1000) % 60;
                int minutes = (int) (remaining / 1000) / 60;
                spawnHologram(lanzador, String.format("§c⌛ Debes esperar %02d:%02d para usar la llave otra vez.", minutes, seconds));
                return;
            }
        }
        try {
            ItemStack item = lanzador.getItemInHand ( );

            if (!hasPermanent ( item )) {
                ItemStack updated = CmdItemUsage.hasUses ( item, lanzador );
                if (updated == null) {
                    lanzador.setItemInHand ( null );
                } else {
                    lanzador.setItemInHand ( updated );
                }
                lanzador.updateInventory ( );
            }
        } catch (Exception ignored) {
        }
        cooldowns.put(uuid, now);
        IWorld world = NpcAPI.Instance().getPlayer(lanzador.getName()).getWorld();
        IEntity<?> iEntity = world.getEntityByID(entity.getEntityId());
        IPos position = iEntity.getPosition();

        world.thunderStrike(position.getX() + 2, position.getY(), position.getZ());
        world.thunderStrike(position.getX() - 2, position.getY(), position.getZ());
        world.thunderStrike(position.getX(), position.getY(), position.getZ() + 2);

        ICustomNpc<?> npc1 = (ICustomNpc<?>) world.spawnClone(position.getX() + 2, position.getY(), position.getZ(), 1, "Guardianes");
        ICustomNpc<?> npc2 = (ICustomNpc<?>) world.spawnClone(position.getX() - 2, position.getY(), position.getZ(), 1, "Guardianes");
        ICustomNpc<?> npc3 = (ICustomNpc<?>) world.spawnClone(position.getX(), position.getY(), position.getZ() + 2, 1, "Guardianes");

        IDBCPlayer idbcPlayer = General.getDBCPlayer(lanzador.getName());
        double Melee;
        int hp = idbcPlayer.getBody();
        int dex = idbcPlayer.getNbt().getCompound("PlayerPersisted").getInteger(org.delaware.tools.General.DEX);
        int WIL = idbcPlayer.getNbt().getCompound("PlayerPersisted").getInteger(org.delaware.tools.General.WIL);
        Melee = (idbcPlayer.getDBCClass() != DBCClass.Spiritualist) ? idbcPlayer.getMeleeStrength() : WIL * 10;

        npc1.setMeleeStrength(Melee);
        npc1.setMaxHealth(hp);
        npc1.setCombatRegen(dex);

        npc2.setMeleeStrength(Melee);
        npc2.setMaxHealth(hp);
        npc2.setCombatRegen(dex);

        npc3.setMeleeStrength(Melee);
        npc3.setMaxHealth(hp);
        npc3.setCombatRegen(dex);

        spawnHologram(lanzador, "&e⚔ &6¡Guardianes protectores invocados! &e⚔");

        if (iEntity instanceof ICustomNpc<?>) {
            ICustomNpc<?> entityNpc = (ICustomNpc<?>) iEntity;
            npc1.setAttackFactions(true);
            npc2.setAttackFactions(true);
            npc3.setAttackFactions(true);
            npc1.setAttackTarget(entityNpc);
            npc2.setAttackTarget(entityNpc);
            npc3.setAttackTarget(entityNpc);
        } else if (iEntity instanceof IPlayer<?>) {
            IPlayer<?> iPlayer = (IPlayer<?>) iEntity;
            npc1.setAttackTarget(iPlayer);
            npc2.setAttackTarget(iPlayer);
            npc3.setAttackTarget(iPlayer);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (iEntity instanceof IPlayer<?>) {
                    IPlayer<?> iPlayer = (IPlayer<?>) iEntity;
                    npc1.setAttackTarget(iPlayer);
                    npc2.setAttackTarget(iPlayer);
                    npc3.setAttackTarget(iPlayer);
                } else if (iEntity instanceof ICustomNpc<?>) {
                    ICustomNpc<?> entityNpc = (ICustomNpc<?>) iEntity;
                    npc1.setAttackFactions(true);
                    npc2.setAttackFactions(true);
                    npc3.setAttackFactions(true);
                    npc1.setAttackTarget(entityNpc);
                    npc2.setAttackTarget(entityNpc);
                    npc3.setAttackTarget(entityNpc);
                }
            }
        }.runTaskTimer(Main.instance, 0L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                npc1.kill();
                npc2.kill();
                npc3.kill();
            }
        }.runTaskLater(Main.instance, 200L); // 10 segundos
    }
}
