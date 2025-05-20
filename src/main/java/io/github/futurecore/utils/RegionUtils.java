package io.github.futurecore.utils;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.GsonBuilder;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonSyntaxException;
import org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.delaware.Main;
import org.delaware.tools.General;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for managing WorldGuard regions and player access.
 */
public class RegionUtils {
    private WorldGuardPlugin wgPlugin;
    private static HashMap<UUID, Instant> cooldowns = new HashMap<> ( );
    public static List<String> restrictedRegions = new ArrayList<> ( );
    public static Map<String, Set<String>> regionAccess = new HashMap<> ( );

    public boolean hasAccess ( String playerName, String regionName ) {
        return regionAccess.containsKey ( regionName ) && regionAccess.get ( regionName ).contains ( playerName );
    }


    public static ProtectedRegion findRegionInAnyWorld ( String regionName ) {
        WorldGuardPlugin wg = WorldGuardPlugin.inst ( );
        RegionContainer container = wg.getRegionContainer ( );

        for (World world : Bukkit.getWorlds ( )) {

            if (world == null) continue;

            RegionManager regionManager = container.get ( world );
            if (regionManager == null) continue;

            ProtectedRegion region = regionManager.getRegion ( regionName );
            if (region != null) {
                return region;
            }
        }
        return null;
    }

    /**
     * Grants temporary access to a specified WorldGuard region.
     *
     * @param player     The player to grant access.
     * @param regionName The name of the region.
     */
    public void grantAccess ( Player player, String regionName, World world ) {
        WorldGuardPlugin wg = WorldGuardPlugin.inst ( );
        RegionContainer container = wg.getRegionContainer ( );
        RegionManager regionManager = container.get ( world );

        if (regionManager == null) {
            player.sendMessage ( "§cError: No se pudo acceder al gestor de regiones." );
            return;
        }
        ProtectedRegion region = regionManager.getRegion ( regionName );
        if (region == null) {
            player.sendMessage ( "§cError: La región '" + regionName + "' no fue encontrada." );
            return;
        }

        region.getMembers ( ).addPlayer ( player.getUniqueId ( ) );
        player.sendMessage ( "§aAcceso concedido a la zona por 60 minutos." );
        cooldowns.put ( player.getUniqueId ( ), Instant.now ( ).plus ( Duration.ofHours ( 24 ) ) );

        new BukkitRunnable ( ) {
            @Override
            public void run () {
                region.getMembers ( ).removePlayer ( player.getName ( ) );
                player.sendMessage ( "§cTu acceso ha expirado, puedes volver en 24 horas." );
            }
        }.runTaskLater ( Main.instance, 60 * 60 * 20 );
    }

    public static void getRegionFlags ( Player player, String regionName ) {
        WorldGuardPlugin wg = WorldGuardPlugin.inst ( );
        RegionManager regionManager = wg.getRegionContainer ( ).get ( player.getWorld ( ) );

        if (regionManager == null) {
            player.sendMessage ( "§cNo se pudo obtener el gestor de regiones." );
            return;
        }

        ProtectedRegion region = regionManager.getRegion ( regionName );
        if (region == null) {
            player.sendMessage ( "§cLa región no fue encontrada." );
            return;
        }

        Map<Flag<?>, Object> flags = region.getFlags ( );
        if (flags.isEmpty ( )) {
            player.sendMessage ( "§eNo hay flags establecidas en esta región." );
            return;
        }

        player.sendMessage ( "§aFlags de la región " + regionName + ":" );
        for (Map.Entry<Flag<?>, Object> entry : flags.entrySet ( )) {
        }
    }

    public ProtectedRegion getRegionAtLocation ( Location location ) {
        if (location == null) return null;

        RegionContainer container = getWorldGuard ( ).getRegionContainer ( );
        RegionManager regionManager = container.get ( location.getWorld ( ) );

        if (regionManager == null) return null;

        Location wgLocation = new Location ( location.getWorld ( ), location.getX ( ), location.getY ( ), location.getZ ( ) );

        for (ProtectedRegion region : regionManager.getApplicableRegions ( wgLocation )) {
            return region;
        }

        return null;
    }

    public void grantAccess ( Player player, String regionName, ProtectedRegion region ) {
        if (region == null) {
            player.sendMessage ( "§cError: La región '" + regionName + "' no fue encontrada." );
            return;
        }
        region.getMembers ( ).addPlayer ( player.getName ( ) );
        cooldowns.put ( player.getUniqueId ( ), Instant.now ( ).plus ( Duration.ofHours ( 24 ) ) );
        if (!regionAccess.containsKey ( regionName )) {
            HashSet<String> players = new HashSet<> ( );
            players.add ( player.getName ( ) );
            regionAccess.put ( regionName, players );
        }
        Set<String> players = regionAccess.get ( regionName );
        players.add ( player.getName ( ) );
        regionAccess.put ( regionName, players );
        new BukkitRunnable ( ) {
            @Override
            public void run () {
                Set<String> players = regionAccess.get ( regionName );
                if (players != null) {
                    players.remove ( player.getName ( ) );
                }
                regionAccess.put ( regionName, players );
                player.sendMessage ( "§cTu acceso ha expirado, puedes volver en 24 horas." );
                region.getMembers ( ).removePlayer ( player.getName ( ) );
            }
        }.runTaskLater ( Main.instance, 60 * 60 * 20 );
    }

    /**
     * Checks if a given location is inside a specified region.
     *
     * @param location   The location to check.
     * @param regionName The name of the region.
     * @return True if the location is within the region, false otherwise.
     */
    public static boolean isLocationInRegion ( Location location, String regionName ) {
        WorldGuardPlugin wgPlugin = getWorldGuard ( );
        if (wgPlugin == null) return false;

        World world = location.getWorld ( );
        RegionContainer container = wgPlugin.getRegionContainer ( );
        RegionManager regionManager = container.get ( world );
        if (regionManager == null) return false;

        ProtectedRegion region = regionManager.getRegion ( regionName );
        if (region == null) return false;

        ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions ( location );

        return applicableRegions.getRegions ( ).contains ( region );
    }

    public boolean isInRestrictedRegion ( Player player ) {
        for (String region : restrictedRegions) {
            if (isPlayerInRegion ( player, region ) && !isMemberOfRegion ( player, region )) {
                return true;
            }
        }
        return false;
    }

    public boolean isMemberOfRegion ( Player player, String regionName ) {
        WorldGuardPlugin wgPlugin = getWorldGuard ( );
        if (wgPlugin == null) return false;

        RegionContainer container = wgPlugin.getRegionContainer ( );
        RegionManager regionManager = container.get ( player.getWorld ( ) );
        if (regionManager == null) return false;

        ProtectedRegion region = regionManager.getRegion ( regionName );
        if (region == null) return false;

        return region.getMembers ( ).contains ( player.getName ( ) );
    }

    /**
     * Retrieves a list of region names at a specific location.
     *
     * @param location The location to check.
     * @return A list of region names at the location.
     */
    public List<String> getRegionsAtLocation ( Location location ) {
        WorldGuardPlugin wgPlugin = getWorldGuard ( );
        if (wgPlugin == null) return Collections.emptyList ( );

        RegionContainer container = wgPlugin.getRegionContainer ( );
        RegionManager regionManager = container.get ( location.getWorld ( ) );
        if (regionManager == null) return Collections.emptyList ( );

        ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions ( location );
        return applicableRegions.getRegions ( ).stream ( ).map ( ProtectedRegion::getId ).collect ( Collectors.toList ( ) );
    }

    /**
     * Retrieves a list of regions where a player has access (either as a member or owner).
     *
     * @param player The player to check.
     * @return A list of region names the player has access to.
     */
    public List<String> listPlayerRegions ( Player player ) {
        WorldGuardPlugin wgPlugin = getWorldGuard ( );
        if (wgPlugin == null) return Collections.emptyList ( );

        RegionContainer container = wgPlugin.getRegionContainer ( );
        RegionManager regionManager = container.get ( player.getWorld ( ) );
        if (regionManager == null) return Collections.emptyList ( );

        return regionManager.getRegions ( ).values ( ).stream ( )
                .filter ( region -> region.getMembers ( ).contains ( player.getName ( ) ) || region.getOwners ( ).contains ( player.getName ( ) ) )
                .map ( ProtectedRegion::getId )
                .collect ( Collectors.toList ( ) );
    }

    /**
     * Checks if a player is currently inside a specified region.
     *
     * @param player     The player to check.
     * @param regionName The name of the region.
     * @return True if the player is inside the region, false otherwise.
     */
    public boolean isPlayerInRegion ( Player player, String regionName ) {
        return isLocationInRegion ( player.getLocation ( ), regionName );
    }

    /**
     * Revokes a player's access to a specified region.
     *
     * @param player     The player to remove access from.
     * @param regionName The name of the region.
     */
    public void removeAccess ( Player player, String regionName ) {
        if (wgPlugin == null) return;

        RegionManager regionManager = wgPlugin.getRegionManager ( player.getWorld ( ) );
        ProtectedRegion region = regionManager.getRegion ( regionName );
        if (region == null) return;

        region.getMembers ( ).removePlayer ( player.getName ( ) );
        player.sendMessage ( "§cTu acceso a " + regionName + " ha sido revocado." );
    }
    public static void setPlayerEntry(String regionName, Player player, boolean allowed) {
        WorldGuardPlugin wg = getWorldGuard();
        RegionManager regionManager = wg.getRegionContainer().get(player.getWorld());
        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) return;

        DefaultDomain domain = region.getMembers();
        if (allowed) {
            domain.addPlayer(player.getUniqueId());
        } else {
            domain.removePlayer(player.getUniqueId());
        }
        region.setMembers(domain);
    }

    public boolean hasCooldown ( Player player ) {
        return cooldowns.containsKey ( player.getUniqueId ( ) ) && Instant.now ( ).isBefore ( cooldowns.get ( player.getUniqueId ( ) ) );
    }

    public String getRemainingCooldown ( Player player ) {
        if (!hasCooldown ( player )) {
            return "00h 00m 00s";
        }
        Duration remaining = Duration.between ( Instant.now ( ), cooldowns.get ( player.getUniqueId ( ) ) );
        return General.formatDuration ( remaining );
    }

    /**
     * Retrieves the WorldGuard plugin instance from Bukkit's plugin manager.
     *
     * @return The WorldGuard plugin instance, or null if not found.
     */
    private static WorldGuardPlugin getWorldGuard () {
        return (WorldGuardPlugin) Bukkit.getServer ( ).getPluginManager ( ).getPlugin ( "WorldGuard" );
    }

    private static final Gson GSON = new GsonBuilder ( ).setPrettyPrinting ( ).create ( );
    private static final File rootDir = new File ( Main.instance.getDataFolder ( ), "DTools" );
    private static final File dataDir = new File ( rootDir, "data" );
    private static final File RESTRICTED_REGION_FILE = new File ( dataDir, "restrictedRegions.json" );
    private static final File COLDOWN_FILE = new File ( dataDir, "coldowns.json" );

    public static void saveData () {
        if (!dataDir.exists ( )) dataDir.mkdirs ( );
        if (cooldowns.isEmpty ( ) || restrictedRegions.isEmpty ( )) return;
        try (FileWriter writer = new FileWriter ( RESTRICTED_REGION_FILE )) {
            GSON.toJson ( restrictedRegions, writer );
        } catch (IOException e) {
            e.printStackTrace ( );
        }

        try (FileWriter writer = new FileWriter ( COLDOWN_FILE )) {
            GSON.toJson ( cooldowns, writer );
        } catch (IOException e) {
            e.printStackTrace ( );
        }
    }

    public static void loadData () {
        if (!dataDir.exists ( )) dataDir.mkdirs ( );

        if (COLDOWN_FILE.exists ( )) {
            try (FileReader reader = new FileReader ( COLDOWN_FILE )) {
                Type type = new TypeToken<HashMap<UUID, Instant>> ( ) {
                }.getType ( );
                HashMap<UUID, Instant> loadedData = GSON.fromJson ( reader, type );
                if (loadedData != null) cooldowns = loadedData;
            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace ( );
            }
        }

        if (RESTRICTED_REGION_FILE.exists ( )) {
            try (FileReader reader = new FileReader ( RESTRICTED_REGION_FILE )) {
                Type type = new TypeToken<ArrayList<String>> ( ) {
                }.getType ( );
                List<String> loadedBoosters = GSON.fromJson ( reader, type );
                if (loadedBoosters != null) restrictedRegions = loadedBoosters;
            } catch (IOException | JsonSyntaxException e) {
                e.printStackTrace ( );
            }
        }
    }

    public static void setRegionTimeLimit ( Player player, ProtectedRegion region, String time ) {
        long delayTicks = parseTimeToTicks ( time );
        if (delayTicks <= 0) {
            player.sendMessage ( "&cFormato de tiempo inválido. Usa '10m', '10s' o '10h'." );
            return;
        }

        new BukkitRunnable ( ) {
            @Override
            public void run () {
                region.getMembers ( ).removePlayer ( player.getName ( ) );
                player.sendMessage ( "&cTu acceso ha expirado, puedes volver en 24 horas." );
            }
        }.runTaskLater ( Main.instance, delayTicks );
    }

    private static long parseTimeToTicks ( String time ) {
        if (time == null || time.isEmpty ( )) return -1;
        time = time.toLowerCase ( ).trim ( );
        Pattern pattern = Pattern.compile ( "(\\d+)([smh])" );
        Matcher matcher = pattern.matcher ( time );

        if (matcher.matches ( )) {
            int value = Integer.parseInt ( matcher.group ( 1 ) );
            String unit = matcher.group ( 2 );

            switch (unit) {
                case "s":
                    return value * 20L;
                case "m":
                    return value * 60L * 20L;
                case "h":
                    return value * 3600L * 20L;
            }
        }
        return -1;
    }

}
