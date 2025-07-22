package io.github.futurecore.utils.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import io.github.futurecore.Main;
import io.github.futurecore.utils.General;
import io.github.futurecore.utils.RegionUtils;
import lombok.Getter;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

public class RaidMenu implements InventoryProvider, Listener {

    private static final String bossName = "Vegeta";
    private static final int maxJugadores = 1;
    private static final long COOLDOWN_MILLIS = 3 * 60 * 60 * 1000; // 3 horas en milisegundos
    @Getter
    private static final Set<UUID> jugadores = new HashSet<> ( );
    private static final Map<UUID, Long> cooldowns = new HashMap<> ( );
    private static EstadoRaid estado = EstadoRaid.EN_ESPERA;
    @Getter
    private static ICustomNpc<?> currentBoss = null;
    private static ICustomNpc<?> secundaryBoss = null;

    private static final List<ICustomNpc<?>> saibaimans = new ArrayList<> ( );

    public static void removeJugador ( UUID uuid ) {
        jugadores.remove ( uuid );
    }

    enum EstadoRaid {
        EN_ESPERA,
        INICIADA
    }

    @Override
    public void init ( Player player, InventoryContents contents ) {
        ItemStack raidItem = new ItemStack ( Material.getMaterial ( 6202 ) ); // ID personalizado

        ItemMeta meta = raidItem.getItemMeta ( );
        meta.setDisplayName ( ChatColor.GREEN + "Ingresar a la Raid" );

        List<String> lore = new ArrayList<> ( );
        lore.add ( ChatColor.YELLOW + "Jefe: " + ChatColor.RED + bossName );
        lore.add ( ChatColor.YELLOW + "Estado: " + ChatColor.AQUA + (estado == EstadoRaid.EN_ESPERA ? "En espera" : "Iniciada") );
        lore.add ( ChatColor.YELLOW + "Jugadores: " + jugadores.size ( ) + "/" + maxJugadores );

        // Mostrar tiempo restante si está en cooldown
        if (cooldowns.containsKey ( player.getUniqueId ( ) )) {
            long tiempoRestante = cooldowns.get ( player.getUniqueId ( ) ) + COOLDOWN_MILLIS - System.currentTimeMillis ( );
            if (tiempoRestante > 0) {
                long minutos = (tiempoRestante / 1000) / 60;
                lore.add ( ChatColor.RED + "Cooldown: " + minutos + " minutos restantes" );
            }
        }

        meta.setLore ( lore );
        raidItem.setItemMeta ( meta );

        ClickableItem clickable = ClickableItem.of ( raidItem, e -> {
            UUID uuid = player.getUniqueId ( );
            long now = System.currentTimeMillis ( );

            // Verifica si el jugador está en cooldown
            if (cooldowns.containsKey ( uuid )) {
                long cooldownUntil = cooldowns.get ( uuid ) + COOLDOWN_MILLIS;
                if (now < cooldownUntil) {
                    long tiempoRestante = (cooldownUntil - now) / 1000;
                    long minutos = tiempoRestante / 60;
                    long segundos = tiempoRestante % 60;
                    player.sendMessage ( ChatColor.RED + "Debes esperar " + minutos + "m " + segundos + "s para volver a unirte." );
                    return;
                }
            }

            if (estado != EstadoRaid.EN_ESPERA) {
                player.sendMessage ( ChatColor.RED + "La raid ya ha comenzado. No puedes unirte." );
                return;
            }

            if (jugadores.contains ( uuid )) {
                player.sendMessage ( ChatColor.YELLOW + "Ya estás unido a la raid." );
                return;
            }

            if (jugadores.size ( ) >= maxJugadores) {
                player.sendMessage ( ChatColor.RED + "La sala está llena. No puedes unirte." );
                return;
            }

            // Registrar jugador y cooldown
            jugadores.add ( uuid );
            cooldowns.put ( uuid, now );
            player.sendMessage ( ChatColor.GREEN + "Te has unido a la raid contra " + bossName );

            // Teletransportar al warp "raidt1"
            Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp raidt1 " + player.getName ( ) );

            // Iniciar automáticamente si se llena
            if (jugadores.size ( ) == maxJugadores) {
                estado = EstadoRaid.INICIADA;
                startRaidTimer ( );
                startOleadasDeSaibaimans ( );
                UUID uuidpl = jugadores.iterator ( ).next ( );
                IDBCPlayer player1 = NpcAPI.Instance ( ).getPlayer ( Main.instance.getServer ( ).getPlayer ( uuidpl ).getName ( ) )
                        .getDBCPlayer ( );
                currentBoss = (ICustomNpc<?>) player1.getWorld ( ).spawnClone ( player1.getPosition ( ), 5, "Vegeta (RAID)" );
                secundaryBoss = (ICustomNpc<?>) player1.getWorld ( ).spawnClone ( player1.getPosition ( ), 5, "Nappa (RAID)" );
                currentBoss.setFaction ( 0 );
                secundaryBoss.setFaction ( 0 );
                //Derrotan al boss Vegeta
                new BukkitRunnable ( ) {
                    @Override
                    public void run () {
                        if (currentBoss == null) {
                            cancel ( );
                            return;
                        }
                        if (currentBoss.getHealth ( ) == 0 || !currentBoss.isAlive ( ) || currentBoss.getHealth ( ) <= 0) {
                            endRaidWithVictory ( );
                            cancel ( );
                        }
                    }
                }.runTaskTimer ( Main.instance, 0L, 20L ); // verifica cada segundo
            }
        } );

        contents.set ( 1, 4, clickable );
    }

    @Override
    public void update ( Player player, InventoryContents contents ) {

    }

    public static void open ( Player player ) {
        SmartInventory.builder ( )
                .id ( "raid_menu" )
                .provider ( new RaidMenu ( ) )
                .size ( 3, 9 )
                .title ( ChatColor.DARK_PURPLE + "Raid Boss" )
                .build ( )
                .open ( player );
    }

    private static BukkitRunnable raidTimer = null;

    public static void startRaidTimer () {
        if (raidTimer != null) {
            raidTimer.cancel ( ); // por si ya hay uno corriendo
        }

        raidTimer = new BukkitRunnable ( ) {
            int segundos = 600; // 10 minutos = 600 segundos

            @Override
            public void run () {
                if (jugadores.isEmpty ( )) {
                    cancel ( );
                    estado = EstadoRaid.EN_ESPERA;
                    Bukkit.broadcastMessage ( ChatColor.GRAY + "[RAID] La raid fue cancelada por falta de jugadores." );
                    return;
                }

                if (segundos <= 0) {
                    for (UUID uuid : jugadores) {
                        Player p = Bukkit.getPlayer ( uuid );
                        if (p != null && p.isOnline ( )) {
                            p.sendMessage ( ChatColor.RED + "¡Tiempo agotado! Has fallado la raid." );
                            Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spawn " + p.getName ( ) );
                        }
                    }

                    jugadores.clear ( );
                    estado = EstadoRaid.EN_ESPERA;
                    Bukkit.broadcastMessage ( ChatColor.DARK_RED + "[RAID] ¡El jefe ha ganado! El tiempo se ha agotado." );
                    cancel ( );
                    return;
                }

                if (segundos % 60 == 0 || segundos <= 10) {
                    Bukkit.broadcastMessage ( ChatColor.GOLD + "[RAID] Tiempo restante: " + (segundos / 60) + " min " + (segundos % 60) + " sec" );
                }

                segundos--;
            }
        };

        raidTimer.runTaskTimer ( Main.instance, 0L, 20L ); // cada 1 segundo
    }

    public static void resetRaid () {
        jugadores.clear ( );
        estado = EstadoRaid.EN_ESPERA;
    }

    public static void endRaidWithVictory () {
        if (raidTimer != null) raidTimer.cancel ( );
        Bukkit.broadcastMessage ( ChatColor.LIGHT_PURPLE + "[RAID] El jefe " + bossName + " ha sido derrotado. ¡Victoria!" );
        Bukkit.broadcastMessage ( ChatColor.GOLD + "[RAID] Ranking por número de golpes:" );

        RaidMenu.getRankingHits ( ).entrySet ( ).stream ( )
                .sorted ( Map.Entry.<UUID, Integer>comparingByValue ( ).reversed ( ) )
                .limit ( 3 )
                .forEach ( entry -> {
                    Player p = Bukkit.getPlayer ( entry.getKey ( ) );
                    if (p != null) {
                        Bukkit.broadcastMessage ( ChatColor.YELLOW + "- " + p.getName ( ) + ": " + ChatColor.AQUA + entry.getValue ( ) + " hits" );
                    }
                } );
        for (UUID uuid : jugadores) {
            int hits = RaidMenu.getRankingHits ( ).getOrDefault ( uuid, 0 );
            Player p = Bukkit.getPlayer ( uuid );
            if (p != null && p.isOnline ( )) {
                if (hits < 5) {
                    p.sendMessage ( ChatColor.RED + "No recibes recompensa porque hiciste menos de 20 golpes al jefe." );
                    continue;
                }
                p.sendMessage ( ChatColor.GREEN + "¡Felicidades! Has derrotado al jefe " + bossName );
                Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spawn " + p.getName ( ) );
            }
        }
        jugadores.clear ( );
        estado = EstadoRaid.EN_ESPERA;
        currentBoss = null;
        Bukkit.broadcastMessage ( ChatColor.LIGHT_PURPLE + "[RAID] El jefe " + bossName + " ha sido derrotado. ¡Victoria!" );
    }

    private static final Map<UUID, Integer> hitsPorJugador = new HashMap<> ( );

    public static Map<UUID, Integer> getRankingHits () {
        return hitsPorJugador;
    }

    @EventHandler
    public void onBossDamaged ( EntityDamageByEntityEvent event ) {
        ICustomNpc<?> boss = RaidMenu.getCurrentBoss ( );
        if (!(event.getDamager ( ) instanceof Player)) return;
        if (boss == null || !boss.isAlive ( )) return;
        if (!RegionUtils.isLocationInRegion ( event.getDamager ( ).getLocation ( ), "raidt1" ))
            return;
        int id = event.getEntity ( ).getEntityId ( );
        IDBCPlayer idbcPlayer = General.getDBCPlayer ( ((Player) event.getDamager ( )).getName ( ) );
        ICustomNpc<?> iCustomNpc = (ICustomNpc<?>) idbcPlayer.getWorld ( ).getEntityByID ( id );
        if (!iCustomNpc.equals ( boss )) return;
        UUID uuid = event.getDamager ( ).getUniqueId ( );
        RaidMenu.getRankingHits ( ).put ( uuid, RaidMenu.getRankingHits ( ).getOrDefault ( uuid, 0 ) + 1 );
    }

    public static void startOleadasDeSaibaimans () {
        Bukkit.broadcastMessage ( ChatColor.DARK_GREEN + "[RAID] ¡Oleadas de Saibaimans han comenzado!" );

        new BukkitRunnable ( ) {
            int oleada = 1;
            final int maxOleadas = 4;

            @Override
            public void run () {
                if (oleada > maxOleadas) {
                    Bukkit.broadcastMessage ( ChatColor.GREEN + "[RAID] Las oleadas de Saibaimans han terminado." );
                    cancel ( );
                    return;
                }

                Bukkit.broadcastMessage ( ChatColor.YELLOW + "[RAID] ¡Oleada #" + oleada + " de Saibaimans!" );

                for (UUID uuid : jugadores) {
                    Player p = Bukkit.getPlayer ( uuid );
                    if (p != null && p.isOnline ( )) {
                        IDBCPlayer dbcPlayer = General.getDBCPlayer ( p.getName ( ) );
                        //Spawneamos Saibaiman para que los maten en oleadas durante 2 mins intervalos de 30s entre holeada
                        ICustomNpc<?> saibaiman = (ICustomNpc<?>) dbcPlayer.getWorld ( ).spawnClone (
                                dbcPlayer.getPosition ( ).add ( 2, 0, 2 ),  // Ajusta si se solapan
                                5,
                                "Saibaiman"
                        );
                        saibaimans.add ( saibaiman );
                        p.sendMessage ( ChatColor.DARK_PURPLE + "¡Un Saibaiman ha aparecido!" );
                    }
                }

                oleada++;
            }
        }.runTaskTimer ( Main.instance, 0L, 20L * 30 ); // cada 30 segundos
        new BukkitRunnable ( ) {
            @Override
            public void run () {
                saibaimans.removeIf ( npc -> npc == null || !npc.isAlive ( ) );
                if (saibaimans.isEmpty ( )) {
                    secundaryBoss.setFaction ( 2 ); //Colocamos en Agresivo a Nappa para que lo puedan matar
                    cancel ( );
                }
            }
        }.runTaskTimer ( Main.instance, 0L, 1L );
        new BukkitRunnable ( ) {
            @Override
            public void run () {
                if (secundaryBoss == null || !secundaryBoss.isAlive ( )) {
                    currentBoss.setFaction ( 2 ); //Colocamos en Agresivo a Vegeta para que lo puedan matar
                    cancel ( );
                }
            }
        }.runTaskTimer ( Main.instance, 0L, 1L );

    }

}
