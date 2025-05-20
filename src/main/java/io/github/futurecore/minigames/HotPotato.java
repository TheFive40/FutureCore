package io.github.futurecore.minigames;
import io.github.facuu16.gohan.dbc.model.DbcPlayer;
import io.github.futurecore.Main;
import io.github.futurecore.utils.RegionUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class HotPotato implements Listener {
    private final String prefix = ChatColor.AQUA + "[" + ChatColor.GOLD + "MiniGames" + ChatColor.AQUA + "] ";
    private final String regionName = "minigameHotpotato";
    private final Set<UUID> eliminated = new HashSet<>();
    private boolean gameRunning = false;
    private UUID potatoHolder;
    private BukkitRunnable countdownTask;
    private BukkitRunnable eliminationTask;

    private void tryStart() {
        if (gameRunning) return;

        List<Player> players = getPlayersInRegion();
        if (players.size() < 5) return;

        gameRunning = true;

        sendMessageToRegionPlayers(prefix + ChatColor.GREEN + "El juego comienza en 10 segundos...");

        countdownTask = new BukkitRunnable() {
            int count = 10;

            @Override
            public void run() {
                if (count == 0) {
                    this.cancel();
                    startGame();
                    return;
                }
                sendMessageToRegionPlayers(prefix + ChatColor.YELLOW + "Comienza en " + count + " segundos...");
                count--;
            }
        };
        countdownTask.runTaskTimer(Main.instance, 0L, 20L);
    }

    private void startGame() {
        List<Player> players = getPlayersInRegion();
        Player selected = players.get(new Random().nextInt(players.size()));
        potatoHolder = selected.getUniqueId();
        selected.setItemInHand(createHotPotatoItem());
        sendMessageToRegionPlayers(prefix + ChatColor.GOLD + selected.getName() + ChatColor.YELLOW + " tiene la " + ChatColor.RED + "Patata Caliente!");
        startEliminationTimer();
    }

    private void startEliminationTimer() {
        if (eliminationTask != null) eliminationTask.cancel();

        eliminationTask = new BukkitRunnable() {
            @Override
            public void run() {
                Player holder = Bukkit.getPlayer(potatoHolder);
                if (holder == null || !RegionUtils.isLocationInRegion(holder.getLocation(), regionName)) {
                    checkEndGame();
                    return;
                }

                eliminated.add(holder.getUniqueId());
                holder.getWorld().createExplosion(holder.getLocation(), 2F);
                holder.sendMessage(prefix + ChatColor.RED + "¡La patata explotó en tus manos! Has sido eliminado.");
                holder.performCommand("warp spawn");
                holder.setItemInHand(null);
                sendMessageToRegionPlayers(prefix + ChatColor.RED + holder.getName() + " ha sido eliminado.");

                potatoHolder = null;
                checkEndGame();
            }
        };
        eliminationTask.runTaskLater(Main.instance, 20 * 20L); // 20 segundos
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player player = (Player) e.getWhoClicked();
        if (!gameRunning || potatoHolder == null) return;

        if (e.getCurrentItem() != null && e.getCurrentItem().getTypeId() == 393) {
            e.setCancelled(true);
            player.sendMessage(prefix + ChatColor.RED + "No puedes mover la Patata Caliente en tu inventario.");
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        if (!gameRunning || !player.getUniqueId().equals(potatoHolder)) return;
        e.setCancelled(true);
        player.sendMessage(prefix + ChatColor.RED + "¡No puedes cambiar de slot mientras tengas la Patata Caliente!");
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!gameRunning || potatoHolder == null) return;
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) return;
        Player damager = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();
        if (damager.getInventory().getItemInHand().getType().getId() != 393) return;
        if (!damager.getUniqueId().equals(potatoHolder)) return;
        if (!RegionUtils.isLocationInRegion(victim.getLocation(), regionName)) return;
        if (eliminated.contains(victim.getUniqueId())) return;
        IDBCPlayer idbcPlayer = NpcAPI.Instance ().getPlayer ( victim.getName () ).getDBCPlayer ();
        idbcPlayer.setHP ( idbcPlayer.getBody () );
        e.setCancelled ( true );
        potatoHolder = victim.getUniqueId();
        damager.setItemInHand(null);
        victim.setItemInHand(createHotPotatoItem());
        sendMessageToRegionPlayers(prefix + ChatColor.GOLD + victim.getName() + ChatColor.YELLOW + " tiene la " + ChatColor.RED + "Patata Caliente!");
        startEliminationTimer();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        if (!gameRunning && RegionUtils.isLocationInRegion(e.getTo(), regionName)) {
            tryStart();
        }

        if (!gameRunning || !eliminated.contains(p.getUniqueId())) return;

        if (RegionUtils.isLocationInRegion(e.getPlayer().getLocation(), regionName)) {
            e.setCancelled(true);
            p.sendMessage(prefix + ChatColor.RED + "Has sido eliminado. No puedes volver a la región.");
            p.performCommand("warp spawn");
        }
    }

    private ItemStack createHotPotatoItem() {
        ItemStack hotPotato = new ItemStack(Material.BAKED_POTATO);
        ItemMeta meta = hotPotato.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Patata Caliente");
            hotPotato.setItemMeta(meta);
        }
        return hotPotato;
    }

    private void checkEndGame() {
        List<Player> players = getPlayersInRegion();
        List<Player> remaining = new ArrayList<>();
        for (Player p : players) {
            if (!eliminated.contains(p.getUniqueId())) remaining.add(p);
        }

        if (remaining.size() <= 1) {
            gameRunning = false;
            if (countdownTask != null) countdownTask.cancel();
            if (eliminationTask != null) eliminationTask.cancel();

            Player winner = remaining.isEmpty() ? null : remaining.get(0);
            if (winner != null) {
                Main.instance.getServer ().broadcastMessage (prefix + ChatColor.GOLD + winner.getName() + ChatColor.GREEN + " ha ganado!");
                winner.performCommand ( "warp spawn" );
                DbcPlayer<EntityPlayerMP> jugador = new DbcPlayer<> ( winner.getUniqueId ( ) );
                int lvl = jugador.level ( );
                int tpGain = lvl * 60;
                jugador.addTp ( tpGain );
            } else {
                sendMessageToRegionPlayers(prefix + ChatColor.GRAY + "Todos los jugadores han sido eliminados.");
            }

            eliminated.clear();
            potatoHolder = null;
        } else {
            Player next = remaining.get(new Random().nextInt(remaining.size()));
            potatoHolder = next.getUniqueId();
            sendMessageToRegionPlayers(prefix + ChatColor.LIGHT_PURPLE + next.getName() + ChatColor.YELLOW + " ahora tiene la " + ChatColor.RED + "Patata Caliente!");
            Player player = Bukkit.getServer ().getPlayer ( potatoHolder );
            player.setItemInHand ( createHotPotatoItem () );
            startEliminationTimer();
        }
    }

    private List<Player> getPlayersInRegion() {
        List<Player> list = new ArrayList<>();
        for (Player p : Main.instance.getServer ( ).getOnlinePlayers ( )) {
            if (RegionUtils.isLocationInRegion(p.getLocation(), regionName)) {
                list.add(p);
            }
        }
        return list;
    }

    private void sendMessageToRegionPlayers(String message) {
        for (Player p : Main.instance.getServer ( ).getOnlinePlayers ( )) {
            if (RegionUtils.isLocationInRegion(p.getLocation(), regionName)) {
                p.sendMessage(message);
            }
        }
    }
}