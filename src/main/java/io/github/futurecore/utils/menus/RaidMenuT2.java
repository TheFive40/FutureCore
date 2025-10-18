package io.github.futurecore.utils.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import io.github.futurecore.Main;
import io.github.futurecore.minigames.BossRaid;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.General;
import io.github.futurecore.utils.RegionUtils;
import lombok.Getter;
import noppes.npcs.api.IPos;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RaidMenuT2 implements InventoryProvider, Listener {

    private static final String bossName = "Metal Cooler";
    private static final int maxJugadores = 1;
    private static final long COOLDOWN_MILLIS = 3 * 60 * 60 * 1000;
    @Getter
    public static final Set<UUID> jugadores = new HashSet<> ( );
    public static final Map<UUID, Long> cooldowns = new HashMap<> ( );
    public static EstadoRaid estado = EstadoRaid.EN_ESPERA;
    @Getter
    public static ICustomNpc<?> currentBoss = null;
    private static final Map<UUID, List<ICustomNpc<?>>> saibaimansPorJugador = new HashMap<> ( );

    public static void removeJugador ( UUID uuid ) {
        jugadores.remove ( uuid );
    }

    public enum EstadoRaid {
        EN_ESPERA,
        INICIADA
    }
    public static void saveCooldowns( File file) {
        YamlConfiguration config = new YamlConfiguration ();
        for (Map.Entry<UUID, Long> entry : cooldowns.entrySet()) {
            config.set("cooldownsT2." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void loadCooldowns(File file) {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        cooldowns.clear();
        if (config.contains("cooldownsT2")) {
            for (String key : config.getConfigurationSection("cooldownsT2").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long value = config.getLong("cooldownsT2." + key);
                    cooldowns.put(uuid, value);
                } catch (IllegalArgumentException ex) {
                    System.out.println("UUID inválido: " + key);
                }
            }
        }
    }
    @Override
    public void init ( Player player, InventoryContents contents ) {
        ItemStack raidItem = new ItemStack ( Material.getMaterial ( 6178 ) );

        ItemMeta meta = raidItem.getItemMeta ( );
        meta.setDisplayName ( ChatColor.GREEN + "Ingresar a la Raid" );

        List<String> lore = new ArrayList<> ( );
        lore.add ( ChatColor.YELLOW + "Jefe: " + ChatColor.RED + bossName );
        lore.add ( ChatColor.YELLOW + "Estado: " + ChatColor.AQUA + (estado == EstadoRaid.EN_ESPERA ? "En espera" : "Iniciada") );
        lore.add ( ChatColor.YELLOW + "Jugadores: " + jugadores.size ( ) + "/" + maxJugadores );

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

            ItemStack ticket = player.getItemInHand ( );
            if (ticket == null) {
                player.sendMessage ( CC.translate ( "&8[&6RAID&8] &7Necesitas tener &ax16 &7tickets para ingresar a la raid." ) );
                return;
            }
            if (currentBoss != null) currentBoss.despawn ( );

            if (ticket.getTypeId ( ) == 6186 && ticket.getAmount ( ) >= 32 && jugadores.size ( ) < maxJugadores) {
                player.sendMessage ( ChatColor.GREEN + "Te has unido a la raid contra " + bossName );
                Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp raidt2 " + player.getName ( ) );
                ticket.setAmount ( ticket.getAmount ( ) - 32 );
                player.setItemInHand ( ticket );
                jugadores.add ( uuid );
                cooldowns.put ( uuid, now );
            } else {
                player.sendMessage ( CC.translate ( "&8[&6RAID&8] &7Necesitas tener &ax32 &7tickets para ingresar a la raid." ) );
            }
            if (jugadores.size ( ) == maxJugadores) {
                iniciarRaidConCuentaRegresiva ( player );
            }
        } );
        contents.set ( 1, 4, clickable );
        ItemStack filler = new ItemStack ( Material.STAINED_GLASS_PANE, 1, DyeColor.RED.getWoolData ( ) );
        ItemMeta fillerMeta = filler.getItemMeta ( );
        fillerMeta.setDisplayName ( " " );
        filler.setItemMeta ( fillerMeta );
        ClickableItem fillerItem = ClickableItem.empty ( filler );

        for (int row = 0; row < contents.inventory ( ).getRows ( ); row++) {
            for (int col = 0; col < 9; col++) {
                if (!contents.get ( row, col ).isPresent ( )) {
                    contents.set ( row, col, fillerItem );
                }
            }
        }
    }

    @Override
    public void update ( Player player, InventoryContents contents ) {

    }

    public static void open ( Player player ) {
        SmartInventory.builder ( )
                .id ( "raid_menuT2" )
                .provider ( new RaidMenuT2 ( ) )
                .size ( 3, 9 )
                .title ( ChatColor.DARK_PURPLE + "Raid Boss" )
                .build ( )
                .open ( player );
    }

    private static BukkitRunnable raidTimer = null;

    public static void startRaidTimer () {
        if (raidTimer != null) {
            raidTimer.cancel ( );
        }

        raidTimer = new BukkitRunnable ( ) {
            int segundos = 600;
            boolean vegetaTransformado = false;

            @Override
            public void run () {
                if (jugadores.isEmpty ( )) {
                    cancel ( );
                    estado = EstadoRaid.EN_ESPERA;
                    if(currentBoss != null){
                        currentBoss.despawn ( );
                    }
                    saibaimansPorJugador.forEach ( ( k, v ) -> {
                        for (ICustomNpc<?> e : v) {
                            if(e != null){
                                e.despawn ( );
                            }
                        }
                    } );
                    saibaimansPorJugador.clear ( );
                    return;
                }

                if (!vegetaTransformado && segundos <= 360) {
                    vegetaTransformado = true;
                    sendMessageAllPlayers ( CC.translate ( "&d[RAID] ¡Metal Cooler ha perdido el control y ha desatado todo su poder!" ) );
                    if (currentBoss != null) {
                        currentBoss.despawn ( );
                    }

                    UUID uuidJugador = jugadores.iterator ( ).next ( );
                    Player player = Bukkit.getPlayer ( uuidJugador );
                    IDBCPlayer dbcPlayer = General.getDBCPlayer ( player.getName ( ) );
                    currentBoss = (ICustomNpc<?>) dbcPlayer.getWorld ( ).spawnClone (
                            dbcPlayer.getPosition ( ).add ( 2.0, 0.0, 2.0 ),
                            5,
                            "Metal Cooler Gigant (RAID)"
                    );

                    currentBoss.getWorld ( ).thunderStrike ( currentBoss.getPosition ( ) );
                    currentBoss.setFaction ( 2 );
                    player.playSound ( player.getLocation ( ), Sound.ENDERDRAGON_DEATH, 1.0f, 1.0f );
                    sendMessageAllPlayers ( "&8[&5RAID&8] &d¡El jefe secreto §fMetal Cooler§d ha aparecido!" );
                    player.playSound ( player.getLocation ( ), "jinryuudragonbc:DBC3.teleport", 1.0f, 0.8f );
                }

                if (segundos <= 0) {
                    for (UUID uuid : jugadores) {
                        Player p = Bukkit.getPlayer ( uuid );
                        if (p != null && p.isOnline ( )) {
                            p.sendMessage ( ChatColor.RED + "¡Tiempo agotado! Has fallado la raid." );
                            Bukkit.dispatchCommand ( Bukkit.getConsoleSender ( ), "warp spawn " + p.getName ( ) );
                        }
                    }
                    cancel ( );
                    jugadores.clear ( );
                    estado = EstadoRaid.EN_ESPERA;
                    if(currentBoss != null){
                        currentBoss.despawn ( );
                    }
                    saibaimansPorJugador.values ( ).forEach ( lista -> lista.forEach ( IEntity::despawn ) );
                    Bukkit.broadcastMessage ( ChatColor.DARK_RED + "[RAID] ¡El jefe ha ganado! El tiempo se ha agotado." );
                    return;
                }

                if (segundos % 60 == 0 || segundos <= 10) {
                    Bukkit.broadcastMessage ( CC.translate ( "&8[&4&lRAID T2&8] &cTiempo restante: " + (segundos / 60) + " min " + (segundos % 60) + " sec" ) );
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
        BossRaid.bossHits = 0;
        if(currentBoss != null){
            currentBoss.despawn ( );
        }
        saibaimansPorJugador.values ( ).forEach ( lista -> lista.forEach ( IEntity::despawn ) );
        for (UUID uuid : jugadores) {
            Player p = Bukkit.getPlayer ( uuid );
            if (p != null && p.isOnline ( )) {
                p.sendMessage ( ChatColor.GREEN + "¡Felicidades! Has derrotado al jefe " + bossName );
                Main.instance.getServer ().dispatchCommand ( Main.instance.getServer ().getConsoleSender ( ), "give " + p.getName ( ) + " 4461 128" );
                Main.instance.getServer ().dispatchCommand ( Main.instance.getServer ().getConsoleSender ( ), "eco give " + p.getName ( ) + " 15000" );
                Main.instance.getServer ().dispatchCommand ( Main.instance.getServer ().getConsoleSender ( ), "zenkais give 100 " + p.getName ( ) );
                Main.instance.getServer ().dispatchCommand ( Main.instance.getServer ().getConsoleSender ( ), "dartps " + p.getName ( ) + " " +
                        General.getLVL ( p ) * 1200 );
                Main.instance.getServer ().dispatchCommand ( Main.instance.getServer ().getConsoleSender ( ), "warp spawn " + p.getName ( ) );
            }
        }
        jugadores.clear ( );
        estado = EstadoRaid.EN_ESPERA;
        currentBoss = null;
    }


    public static final Map<UUID, Integer> hitsPorJugador = new HashMap<> ( );

    public static Map<UUID, Integer> getRankingHits () {
        return hitsPorJugador;
    }

    public static void sendMessageAllPlayers ( String message ) {
        jugadores.forEach ( e -> {
            Player player = Bukkit.getPlayer ( e );
            player.sendMessage ( CC.translate ( message ) );
        } );
    }

    public static void startOleadasDeSaibaimans () {
        new BukkitRunnable ( ) {
            int oleada = 1;
            final int maxOleadas = 20;

            @Override
            public void run () {
                if (jugadores.isEmpty ( )) {
                    cancel ( );
                    if (currentBoss != null) {
                        currentBoss.despawn ( );
                    }
                    saibaimansPorJugador.values ( ).forEach ( lista -> lista.forEach ( IEntity::despawn ) );
                }
                if (oleada > maxOleadas) {
                    cancel ( );
                    saibaimansPorJugador.values ( ).forEach ( lista -> lista.forEach ( IEntity::despawn ) );
                    saibaimansPorJugador.clear ( );
                    jugadores.forEach ( e -> {
                        Player player = Bukkit.getPlayer ( e );
                        player.sendMessage ( "§8[§6RAID§8] §7Las oleadas de §aMetal Cooler§7 han terminado." );
                    } );
                    for (UUID uuid : jugadores) {
                        Player player = Bukkit.getPlayer ( uuid );
                        if (player != null) {
                            player.playSound ( player.getLocation ( ), Sound.WITHER_SPAWN, 1.0f, 1.0f );
                        }
                    }
                    currentBoss.setFaction ( 2 );
                    return;
                }

                for (UUID uuid : jugadores) {
                    Player p = Bukkit.getPlayer ( uuid );
                    if (p != null && p.isOnline ( )) {
                        List<ICustomNpc<?>> actuales = saibaimansPorJugador.getOrDefault ( uuid, new ArrayList<> ( ) );

                        boolean todosMuertos = actuales.isEmpty ( ) || actuales.stream ( ).allMatch ( npc -> !npc.isAlive ( ) );

                        if (todosMuertos || oleada == 1) {
                            IDBCPlayer dbcPlayer = General.getDBCPlayer ( p.getName ( ) );
                            if (!RegionUtils.isLocationInRegion ( p.getLocation ( ), "raidt2" )) {
                                removeJugador ( uuid );
                                continue;
                            }
                            List<ICustomNpc<?>> nuevos = new ArrayList<> ( );

                            for (IPos offset : Arrays.asList (
                                    dbcPlayer.getPosition ( ).add ( 2, 1, 2 ),
                                    dbcPlayer.getPosition ( ).add ( 1, 1, 1 ),
                                    dbcPlayer.getPosition ( ).add ( -1, 1, -1 ),
                                    dbcPlayer.getPosition ( ).add ( -2, 0, -2 ),
                                    dbcPlayer.getPosition ( ).add ( -2, 1, 2 ),
                                    dbcPlayer.getPosition ( ).add ( 2, 1, -2 )
                            )) {
                                ICustomNpc<?> saibaiman = (ICustomNpc<?>) dbcPlayer.getWorld ( ).spawnClone ( offset, 5, "Metal Cooler" );
                                nuevos.add ( saibaiman );
                                p.playSound ( p.getLocation ( ), "jinryuudragonbc:DBC3.teleport", 1.0f, 1.0f );
                            }

                            double porcentajeVida = 1.0, porcentajeFuerza = 1.0, porcentajeRegen = 1.0;
                            switch (oleada) {
                                case 2:
                                case 3:
                                    porcentajeVida = 1.05;
                                    porcentajeFuerza = 1.07;
                                    porcentajeRegen = 1.01;
                                    break;
                                case 4:
                                case 5:
                                    porcentajeVida = 1.10;
                                    porcentajeFuerza = 1.14;
                                    porcentajeRegen = 1.02;
                                    break;
                                case 6:
                                case 7:
                                    porcentajeVida = 1.15;
                                    porcentajeFuerza = 1.21;
                                    porcentajeRegen = 1.03;
                                    break;
                                case 8:
                                case 9:
                                    porcentajeVida = 1.20;
                                    porcentajeFuerza = 1.28;
                                    porcentajeRegen = 1.04;
                                    break;
                                case 10:
                                    porcentajeVida = 1.25;
                                    porcentajeFuerza = 1.35;
                                    porcentajeRegen = 1.05;
                                    break;
                            }

                            for (ICustomNpc<?> s : nuevos) {
                                s.setMaxHealth ( s.getMaxHealth ( ) * porcentajeVida );
                                s.setMeleeStrength ( s.getMeleeStrength ( ) * porcentajeFuerza );
                                s.setCombatRegen ( (float) (s.getCombatRegen ( ) * porcentajeRegen) );
                            }

                            saibaimansPorJugador.put ( uuid, nuevos );
                            oleada++;
                        }
                    }
                }

            }
        }.runTaskTimer ( Main.instance, 0L, 20L * 5 );

        new BukkitRunnable ( ) {
            @Override
            public void run () {
                if (jugadores.isEmpty ( )) {
                    cancel ( );
                    if(currentBoss != null){
                        currentBoss.despawn ( );
                    }
                    saibaimansPorJugador.values ( ).forEach ( lista -> lista.forEach ( IEntity::despawn ) );
                }
            }
        }.runTaskTimer ( Main.instance, 0L, 1L );
    }


    public void iniciarRaidConCuentaRegresiva ( Player jugador ) {
        new BukkitRunnable ( ) {
            int tiempo = 5;

            @Override
            public void run () {
                if (tiempo > 0) {
                    jugador.sendMessage ( "§8[§6RAID§8] §7La raid comenzará en §c" + tiempo + "§7 segundos..." );
                    tiempo--;
                } else {

                    cancel ( );
                    jugador.sendMessage ( "§8[§6RAID§8] §a¡La raid ha comenzado!" );
                    jugador.sendMessage ( "§8[§6RAID§8] §e¡Metal Cooler ha llegado al campo de batalla!" );

                    estado = EstadoRaid.INICIADA;
                    startRaidTimer ( );

                    UUID uuidpl = jugadores.iterator ( ).next ( );
                    IDBCPlayer player1 = NpcAPI.Instance ( ).getPlayer ( Main.instance.getServer ( ).getPlayer ( uuidpl ).getName ( ) ).getDBCPlayer ( );

                    currentBoss = (ICustomNpc<?>) player1.getWorld ( ).spawnClone ( player1.getPosition ( ).add ( 1.0, 0.0, 1.0 ), 5, "Metal Cooler Final (RAID)" );

                    currentBoss.getWorld ( ).thunderStrike ( currentBoss.getPosition ( ) );
                    currentBoss.setFaction ( 0 );

                    jugador.playSound ( jugador.getLocation ( ), "jinryuudragonbc:DBC3.teleport", 1.0f, 1.0f );

                    new BukkitRunnable ( ) {
                        int tiempoSpawn = 5;

                        @Override
                        public void run () {
                            if (tiempoSpawn > 0) {
                                jugador.sendMessage ( "§8[§6RAID§8] §7Los §aMetal Coolers§7 aparecerán en §c" + tiempoSpawn + "§7 segundos..." );
                                tiempoSpawn--;
                            } else {
                                cancel ( );
                                jugador.sendMessage ( "§8[§6RAID§8] §a¡Los Metal Coolers han aparecido!" );
                                jugador.sendMessage ( "§8[§6RAID§8] §e¡Prepárate para luchar!" );

                                // Reproducir sonido al jugador cuando aparecen los Saibaimans
                                jugador.playSound ( jugador.getLocation ( ), "jinryuudragonbc:DBC3.teleport", 1.0f, 1.0f );

                                startOleadasDeSaibaimans ( );
                            }
                        }
                    }.runTaskTimer ( Main.instance, 0L, 20L );
                    new BukkitRunnable ( ) {
                        @Override
                        public void run () {
                            if (currentBoss == null) {
                                endRaidWithVictory ( );
                                cancel ( );
                                return;
                            }
                            if (currentBoss.getHealth ( ) == 0 || !currentBoss.isAlive ( ) || currentBoss.getHealth ( ) <= 0
                                    || jugadores.isEmpty ( )) {
                                endRaidWithVictory ( );
                                cancel ( );
                            }
                        }
                    }.runTaskTimer ( Main.instance, 0L, 20L ); // verifica cada segundo

                }
            }
        }.runTaskTimer ( Main.instance, 0L, 20L );
    }

}