package io.github.futurecore.minigames;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class WaterDrop implements Listener {
    private static int seconds = 10;

    private final String prefix = ChatColor.AQUA + "[" + ChatColor.GOLD + "MiniGames" + ChatColor.AQUA + "] ";
    private static final String REGION_NAME = "pozoMiniGame";
    private static CopyOnWriteArrayList<UUID> players = new CopyOnWriteArrayList<> ( );
    private static final Map<UUID, Integer> playerPoints = new HashMap<> ( );
    private static final Set<Block> usedLocations = new HashSet<> ( );
    private static boolean gameRunning = false;
    public CopyOnWriteArrayList<Player> playerEnterAfter = new CopyOnWriteArrayList<> ( );
    private boolean countdownRunning = false;

    public WaterDrop () {
        startMonitor ( );
    }

    private void startMonitor () {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning && !countdownRunning) {
                    int count = 0;
                    for (Player player : Main.instance.getServer ().getOnlinePlayers ()) {
                        if (isInMiniGameZone(player)) {
                            count++;
                            if (!players.contains(player.getUniqueId())) {
                                players.add(player.getUniqueId());
                            }
                        }
                    }

                    if (count >= 5) {
                        countdownRunning = true;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (seconds > 0) {
                                    for (Player player :  Main.instance.getServer ().getOnlinePlayers ()) {
                                        if (isInMiniGameZone(player)) {
                                            player.sendMessage(prefix + ChatColor.WHITE + "El juego de " + ChatColor.AQUA + "Water Drop " +
                                                    ChatColor.WHITE + "comenzará en " + ChatColor.RED + seconds + ChatColor.WHITE + " segundo" + (seconds == 1 ? "" : "s") + "...");
                                        }
                                    }
                                    seconds--;
                                } else {
                                    for (Player player :  Main.instance.getServer ().getOnlinePlayers ()) {
                                        if (isInMiniGameZone(player)) {
                                            player.sendMessage(prefix + ChatColor.GREEN + ChatColor.BOLD + "¡El minijuego Water Drop ha comenzado!" + ChatColor.GRAY + " ¡Buena suerte!");
                                        }
                                    }
                                    gameRunning = true;
                                    countdownRunning = false;
                                    startGameTask();
                                    seconds = 10;
                                    cancel();
                                }
                            }
                        }.runTaskTimer(Main.instance, 0L, 20L);
                    }
                }
            }
        }.runTaskTimer(Main.instance, 0L, 100L);
    }

    @EventHandler
    public void onPlayerFallDamage( EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK &&
        isInMiniGameZone ( (Player) event.getEntity () )) {
            Player player = (Player) event.getEntity();
            updatePoints ( player, -5 );
            player.sendMessage ( CC.translate ( "§c¡Caíste en un bloque ya usado! Pierdes 5 puntos." ) );
            Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spm " + player.getName ( ) );
        }
    }


    @EventHandler
    public void onPlayerMove ( PlayerMoveEvent event ) {
        Player player = event.getPlayer ( );
        if (gameRunning && isInMiniGameZone ( player ) && !players.contains ( player.getUniqueId ( ) )) {
            event.getPlayer ( ).performCommand ( "warp spawn" );
            event.getPlayer ( ).sendMessage ( CC.translate ( "&cEl juego ya inicio no puedes entrar" ) );
        }
        if (!gameRunning && isInMiniGameZone ( player ) && !playerEnterAfter.contains ( event.getPlayer ( ) )) {
            playerEnterAfter.add ( player );
            String message = prefix + ChatColor.GOLD + player.getName ( ) + ChatColor.YELLOW + " ha entrado a la sala";
            for (Player p : Main.instance.getServer ( ).getOnlinePlayers ( )) {
                if (isInMiniGameZone ( p )) {
                    p.sendMessage ( message );

                }
            }
        }
        if (!gameRunning || !isInMiniGameZone ( player )) return;
        Location loc = player.getLocation ( );
        Block blockBelow = player.getWorld ( ).getBlockAt ( loc );
        Location location = loc.getBlock ( ).getLocation ( );
        location.setY ( location.getY ( ) - 1 );
        if (usedLocations.contains ( location.getBlock ( ) )) {
            updatePoints ( player, -5 );
            player.sendMessage ( CC.translate ( "§c¡Caíste en un bloque ya usado! Pierdes 5 puntos." ) );
            Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spm " + player.getName ( ) );
        } else if (blockBelow.getType ( ) == Material.WATER || blockBelow.getType ( ) == Material.STATIONARY_WATER) {
            usedLocations.add ( loc.getBlock ( ) );
            colocarLanaColorida ( loc );
            updatePoints ( player, 10 );
            player.sendMessage ( CC.translate ( "§a¡Buena caída! Ganas 10 puntos." ) );
            Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spm " + player.getName ( ) );
        }
    }

    private void startGameTask () {
        new BukkitRunnable ( ) {
            @Override
            public void run () {
                List<Map.Entry<UUID, Integer>> sorted = new ArrayList<> ( playerPoints.entrySet ( ) );
                sorted.sort ( ( a, b ) -> Integer.compare ( b.getValue ( ), a.getValue ( ) ) );

                String line = ChatColor.YELLOW + "" + ChatColor.STRIKETHROUGH + "-----------------------------------------";

                for (Player player : Main.instance.getServer ( ).getOnlinePlayers ( )) {
                    if (isInMiniGameZone ( player )) {
                        player.sendMessage ( "" );
                        player.sendMessage ( prefix + ChatColor.AQUA + "" + ChatColor.BOLD + "¡El minijuego " + ChatColor.GOLD + "Water Drop" + ChatColor.AQUA + " ha terminado!" );
                        player.sendMessage ( line );

                        for (int i = 0; i < Math.min ( 3, sorted.size ( ) ); i++) {
                            UUID uuid = sorted.get ( i ).getKey ( );
                            int points = sorted.get ( i ).getValue ( );
                            Player rankedPlayer = Bukkit.getPlayer ( uuid );
                            if (rankedPlayer != null) {
                                player.sendMessage ( prefix + ChatColor.GOLD + "" + ChatColor.BOLD + "#" + (i + 1) + " " +
                                        ChatColor.YELLOW + rankedPlayer.getName ( ) + ChatColor.GRAY + " - " +
                                        ChatColor.GREEN + points + " puntos" );
                            }
                        }

                        player.sendMessage ( line );
                        player.sendMessage ( prefix + ChatColor.BLUE + "¡Gracias por jugar!" );
                        player.sendMessage ( "" );
                        if (isInDropZone ( player )) {
                            Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spawn " + player.getName ( ) );
                            player.sendMessage ( prefix + ChatColor.AQUA + "Fuiste enviado al spawn." );
                        }
                    }
                }

                for (Block block : usedLocations) {
                    block.setType ( Material.STATIONARY_WATER );
                }

                usedLocations.clear ( );
                playerEnterAfter.clear ();

                for (Player player : Main.instance.getServer ( ).getOnlinePlayers ( )) {
                    if (isInDropZone ( player )) {
                        Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spm " + player.getName ( ) );
                        player.sendMessage ( CC.translate ( "&b&l¡Fuiste enviado al spawn!" ) );
                    }
                }

                playerPoints.clear ( );
                gameRunning = false;
            }
        }.runTaskLater ( Main.instance, 20L * 60 * 5 );
    }

    private void updatePoints ( Player player, int delta ) {
        playerPoints.put ( player.getUniqueId ( ), playerPoints.getOrDefault ( player.getUniqueId ( ), 0 ) + delta );
    }

    private void colocarLanaColorida ( Location loc ) {
        Block block = loc.getBlock ( );
        block.setType ( Material.WOOL );
        byte color = (byte) new Random ( ).nextInt ( 16 );
        block.setData ( color );
    }


    private boolean isInDropZone ( Player player ) {
        WorldGuardPlugin wg = getWorldGuard ( );
        if (wg == null) return false;

        RegionManager regionManager = wg.getRegionManager ( player.getWorld ( ) );
        if (regionManager == null) return false;

        ApplicableRegionSet regions = regionManager.getApplicableRegions ( player.getLocation ( ) );
        for (ProtectedRegion region : regions) {
            if (region.getId ( ).equalsIgnoreCase ( REGION_NAME )) {
                return true;
            }
        }
        return false;
    }

    private boolean isInMiniGameZone ( Player player ) {
        WorldGuardPlugin wg = getWorldGuard ( );
        if (wg == null) return false;

        RegionManager regionManager = wg.getRegionManager ( player.getWorld ( ) );
        if (regionManager == null) return false;

        ApplicableRegionSet regions = regionManager.getApplicableRegions ( player.getLocation ( ) );
        for (ProtectedRegion region : regions) {
            if (region.getId ( ).equalsIgnoreCase ( "minigames2" )) {
                return true;
            }
        }
        return false;
    }

    private WorldGuardPlugin getWorldGuard () {
        Plugin plugin = Bukkit.getPluginManager ( ).getPlugin ( "WorldGuard" );
        return (plugin instanceof WorldGuardPlugin) ? (WorldGuardPlugin) plugin : null;
    }
}

