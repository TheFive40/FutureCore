package io.github.futurecore.utils;
import com.gmail.filoghost.holograms.api.Hologram;
import com.gmail.filoghost.holograms.api.HolographicDisplaysAPI;
import io.github.facuu16.gohan.dbc.model.DbcPlayer;
import io.github.facuu16.gohan.dbc.model.Stat;
import io.github.futurecore.Main;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.entity.player.EntityPlayerMP;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class General {
    public static double LOSS_KAIROS = 20;
    public static double BOOSTER_MULTIPLIER = 0.0;

    public static final String[] ranks = {"founder", "developer", "manager", "admin", "moderador", "quester", "helper", "constructor", "programador", "dev"};
    public static String DEX = "jrmcDexI";
    public static String SPI = "jrmcCncI";
    public static String CON = "jrmcCnsI";
    public static String STR = "jrmcStrI";
    public static String WIL = "jrmcWill";
    public static String MND = "jrmcIntI";
    public static HashMap<String, String> STATS_MAP = new HashMap<> ( );

    static {
        STATS_MAP.put ( "STR", STR );
        STATS_MAP.put ( "DEX", DEX );
        STATS_MAP.put ( "CON", CON );
        STATS_MAP.put ( "WIL", WIL );
        STATS_MAP.put ( "MND", MND );
        STATS_MAP.put ( "SPI", SPI );
    }

    public static String joinText ( String[] args, int start ) {
        StringBuilder reason = new StringBuilder ( );
        for (int i = start; i < args.length; i++) {
            reason.append ( CC.translate ( args[i] + " " ) );
        }
        return reason.toString ( );
    }

    public static int getPlayerTps ( Player player ) {
        return NpcAPI.Instance ( ).getPlayer ( player.getName ( ) ).getDBCPlayer ( ).getTP ( );
    }
    public static IDBCPlayer getDBCPlayer(String playerName){
        return NpcAPI.Instance ().getPlayer ( playerName ).getDBCPlayer ();
    }
    public static void spawnHologram ( Player player, String text ) {
        Location loc = player.getLocation ( );
        loc.setY ( loc.getY ( ) + 1.5 );
        loc.setZ ( loc.getZ () + 1.0 );
        Hologram hologram = HolographicDisplaysAPI.createHologram ( Main.instance, loc, org.delaware.tools.CC.translate ( text ) );
        Bukkit.getScheduler ( ).runTaskLater ( Main.instance, hologram::delete, 20L );
    }
    public static int getSTAT ( Stat stat, Player entity ) {
        DbcPlayer<EntityPlayerMP> jugador = new DbcPlayer<> ( entity.getUniqueId ( ) );
        return jugador.stat ( stat );
    }

    public static int getLVL ( Player player ) {
        DbcPlayer<EntityPlayerMP> jugador = new DbcPlayer<> ( player.getUniqueId ( ) );
        return jugador.level ( );
    }

    public static boolean hasStaffParent ( Player player ) {
        LuckPerms luckPerms = LuckPermsProvider.get ( );
        User user = luckPerms.getUserManager ( ).getUser ( player.getUniqueId ( ) );

        if (user == null) return false;

        return user.getPrimaryGroup ( ).equalsIgnoreCase ( "staff" ) ||
                user.getInheritedGroups ( user.getQueryOptions ( ) ).stream ( )
                        .anyMatch ( group -> group.getName ( ).equalsIgnoreCase ( "staff" ) );
    }

    public static boolean isHakaishin ( Player player ) {
        LuckPerms luckPerms = LuckPermsProvider.get ( );
        User user = luckPerms.getUserManager ( ).getUser ( player.getUniqueId ( ) );

        if (user == null) return false;

        return user.getPrimaryGroup ( ).equalsIgnoreCase ( "hakaishin" ) ||
                user.getInheritedGroups ( user.getQueryOptions ( ) ).stream ( )
                        .anyMatch ( group -> group.getName ( ).equalsIgnoreCase ( "hakaishin" ) );
    }

    public static String getGroup(UUID playerUUID) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        if (luckPerms == null) {
            System.out.println("[PVBooster] Error: LuckPerms no está disponible.");
            return "default";
        }

        User user = luckPerms.getUserManager().getUser(playerUUID);
        if (user == null) {
            System.out.println("[PVBooster] Advertencia: No se encontró el usuario en LuckPerms para UUID " + playerUUID);
            return "default";
        }

        return user.getPrimaryGroup().toLowerCase();
    }

    public static void setPlayerTps ( Player player, int amount ) {
        NpcAPI.Instance ( ).getPlayer ( player.getName ( ) ).getDBCPlayer ( ).setTP ( getPlayerTps ( player ) + amount );
    }

    public static String formatDuration ( Duration duration ) {
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
    public static List<Player> getStaffs(){
       ArrayList<Player> staffs = new ArrayList<> (  );
        for(Player player : Main.instance.getServer ().getOnlinePlayers () ){
          if (hasStaffParent(player)){
              staffs.add ( player );
          }
       }
        return staffs;
    }
    public static boolean isConvertibleToInt ( String text ) {
        try {
            Integer.parseInt ( text );
            return true;
        } catch (NumberFormatException error) {
            return false;
        }
    }
    public static int getRandomNumber(int min, int max) { return (int)(Math.random()*((max-min)+1))+min; }
}
