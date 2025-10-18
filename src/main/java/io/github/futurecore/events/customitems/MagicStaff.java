package io.github.futurecore.events.customitems;

import io.github.futurecore.Main;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.delaware.events.PersonalBoosterEvent;
import org.delaware.tools.CC;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static io.github.futurecore.events.customitems.AttractorSwordEvent.spawnHologram;

public class MagicStaff implements Listener {
    private final int itemId = 4478;

    private final long dashCooldown = 3000;
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();

    private final long boosterCooldown = 86400000;
    private static final Map<UUID, Long> boosterCooldowns = new HashMap<>();

    private static final Set<UUID> itemOwners = new HashSet<>();
    private static File ownersFile;
    private static FileConfiguration ownersConfig;

    private static File boosterFile;
    private static FileConfiguration boosterConfig;

    public MagicStaff() {
        boosterFile = new File(Main.instance.getDataFolder(), "boosters.yml");
        if (!boosterFile.exists()) {
            boosterFile.getParentFile().mkdirs();
        }
        boosterConfig = YamlConfiguration.loadConfiguration(boosterFile);
        loadBoosterCooldowns();

        // itemOwners.yml
        ownersFile = new File(Main.instance.getDataFolder(), "itemOwners.yml");
        if (!ownersFile.exists()) {
            ownersFile.getParentFile().mkdirs();
        }
        ownersConfig = YamlConfiguration.loadConfiguration(ownersFile);
        loadItemOwners();
    }

    private void loadBoosterCooldowns() {
        if (boosterConfig.contains("cooldowns")) {
            for (String uuidStr : boosterConfig.getConfigurationSection("cooldowns").getKeys(false)) {
                long lastUse = boosterConfig.getLong("cooldowns." + uuidStr);
                boosterCooldowns.put(UUID.fromString(uuidStr), lastUse);
            }
        }
    }

    public static void saveBoosterCooldowns() {
        for (Map.Entry<UUID, Long> entry : boosterCooldowns.entrySet()) {
            boosterConfig.set("cooldowns." + entry.getKey().toString(), entry.getValue());
        }
        try {
            boosterConfig.save(boosterFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadItemOwners() {
        if (ownersConfig.contains("owners")) {
            for (String uuidStr : ownersConfig.getStringList("owners")) {
                itemOwners.add(UUID.fromString(uuidStr));
            }
        }
    }

    public static void saveItemOwners() {
        List<String> ownersList = new ArrayList<>();
        for (UUID uuid : itemOwners) {
            ownersList.add(uuid.toString());
        }
        ownersConfig.set("owners", ownersList);
        try {
            ownersConfig.save(ownersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getItemInHand() == null || player.getItemInHand().getTypeId() != itemId) {
            return;
        }
        ItemStack item = player.getItemInHand();
        if (!item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return;
        }
        boolean tieneLoreDragon = false;
        for (String line : meta.getLore()) {
            if (ChatColor.stripColor(line).equalsIgnoreCase("㊝ ITEM DRAGÓN")) {
                tieneLoreDragon = true;
                break;
            }
        }
        if (!tieneLoreDragon) {
            return;
        }

        UUID id = player.getUniqueId();

        if (!itemOwners.contains(id)) {
            itemOwners.add(id);
            saveItemOwners();
            player.sendMessage(CC.translate("&aAhora este bastón mágico está vinculado a ti."));
        }

        if (!itemOwners.contains(id)) {
            player.sendMessage(CC.translate("&cEste bastón ya pertenece a otro jugador y no puedes usarlo."));
            event.setCancelled(true);
            return;
        }

        long now = System.currentTimeMillis();

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (dashCooldowns.containsKey(id)) {
                long lastUse = dashCooldowns.get(id);
                if (now - lastUse < dashCooldown) {
                    long timeLeft = (dashCooldown - (now - lastUse)) / 1000;
                    spawnHologram(player, "§cEspera " + timeLeft + "s para volver a usar la habilidad.");
                    player.playSound(player.getLocation(), Sound.ENDERMAN_HIT, 1.0F, 1.0F);
                    return;
                }
            }

            dashCooldowns.put(id, now);

            Block targetBlock = player.getTargetBlock(null, 100);

            if (targetBlock != null && targetBlock.getType() != Material.AIR) {
                Location targetLoc = targetBlock.getLocation().add(0, 1, 0); // un bloque arriba
                player.teleport(targetLoc);
            } else {
                Location loc = player.getLocation();
                Vector dir = loc.getDirection().normalize().multiply(5);
                Location newLoc = loc.add(dir);
                player.teleport(newLoc);
            }

            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (boosterCooldowns.containsKey(id)) {
                long lastUse = boosterCooldowns.get(id);
                if (now - lastUse < boosterCooldown) {
                    long timeLeftH = (boosterCooldown - (now - lastUse)) / 1000 / 60 / 60;
                    player.sendMessage(CC.translate("&cYa usaste tu booster. Intenta de nuevo en " + timeLeftH + "h."));
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
                    return;
                }
            }
            boosterCooldowns.put(id, now);
            saveBoosterCooldowns();

            PersonalBoosterEvent.timeBoosterPlayer.put(player.getUniqueId(), LocalDateTime.now());
            PersonalBoosterEvent.boosterPlayer.put(player.getUniqueId(), 0.1);
            player.sendMessage(CC.translate("&eBooster Personal del &a(40%) &eActivado por &c15 Minutos."));
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
        }
    }
}
