package io.github.futurecore;

import io.github.futurecore.commands.player.cmdBosses.CmdBosses;
import io.github.futurecore.utils.ClassesRegistration;
import io.github.futurecore.utils.ItemSerializationUtil;
import io.github.futurecore.utils.commands.CommandFramework;
import io.github.futurecore.utils.data.KairosData.PKairos;
import io.github.futurecore.utils.handlers.bosses.BossStorage;
import io.github.futurecore.utils.handlers.kairos.KairosHandlerManager;
import io.github.futurecore.utils.handlers.kairos.KairosShopManager;
import io.github.futurecore.utils.handlers.kairos.KairosStorage;
import io.github.futurecore.utils.handlers.quests.LocationStorage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.util.io.netty.util.internal.ConcurrentSet;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.delaware.tools.CC;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import static io.github.futurecore.utils.data.KairosDataHandler.KairosDataHandler.itemsKairos;
import static io.github.futurecore.utils.data.KairosDataHandler.KairosDataHandler.pKairos;
import static net.minecraft.entity.boss.BossStatus.bossName;

public class Main extends JavaPlugin {
    public static ConcurrentSet<PKairos> kairos = new ConcurrentSet<> ( );
    public static HashMap<String, Location> npcLocations = new HashMap<> ( );
    public static Main instance;
    private final CommandFramework commandFramework = new CommandFramework ( this );
    private final ClassesRegistration classesRegistration = new ClassesRegistration ( );
    private ICustomNpc<?> boss;

    public CommandFramework getCommandFramework () {
        return commandFramework;
    }

    public static LuckPerms luckPermsAPI;

    public ClassesRegistration getClassesRegistration () {
        return classesRegistration;
    }

    public void onEnable () {
        instance = this;
        luckPermsAPI = LuckPermsProvider.get ( );
        classesRegistration.loadCommands ( "io.github.futurecore.commands.player.cmdkairos" );
        classesRegistration.loadCommands ( "io.github.futurecore.commands.player.cmdUsages" );
        classesRegistration.loadCommands ( "io.github.futurecore.commands.player.cmdBosses" );
        classesRegistration.loadCommands ( "io.github.futurecore.commands.player.cmdcore" );
        classesRegistration.loadListeners ( "io.github.futurecore.events.bukkit" );
        classesRegistration.loadListeners ( "io.github.futurecore.events.customitems" );
        classesRegistration.loadListeners ( "io.github.futurecore.minigames" );

        npcLocations.putAll ( LocationStorage.loadNpcLocations ( this ) );
        pKairos.addAll ( KairosStorage.loadKairosList ( this ) );
        itemsKairos.putAll ( KairosStorage.loadItemsMap ( this ) );
        File file = new File ( Main.instance.getDataFolder ( ), "FutureCore/kairos/player/shopitems.txt" );
        if (!file.exists ( )) {
            try {
                file.createNewFile ( );
            } catch (IOException e) {
                e.printStackTrace ( );
                return;
            }
        }
        BossStorage.loadNpcRarity ( this );
        BossStorage.loadNpcNames ( this );
        KairosShopManager.items = ItemSerializationUtil.loadItemsFromFile ( file );
        startHourlyTask ( );
        startTwoHourlyTask ( );
        System.out.println ( "Plugin successfully enabled" );
        System.out.println ( "Version: 1.1.5 " );
        System.out.println ( "By DelawareX" );
        KairosHandlerManager.startAutoSave ( this );
    }

    @Override
    public void onDisable () {
        KairosStorage.saveKairosList ( this, pKairos );
        KairosStorage.saveItemsMap ( this, itemsKairos );
        BossStorage.saveNpcNames ( this );
        BossStorage.saveNpcRarity ( this );
        LocationStorage.saveNpcLocations ( this, npcLocations );
    }

    public void startHourlyTask () {
        getServer ( ).getScheduler ( ).runTaskTimer ( this, () -> {
            if (boss == null) return;
            boss.kill ( );
            Bukkit.broadcastMessage ( CC.translate ( "&c[FutureCore] &fEl boss &6" + bossName + " &fha sido eliminado automáticamente." ) );
            boss = null;
        }, 60 * 5 * 20, 60 * 60 * 20L );
    }

    public void startTwoHourlyTask () {
        getServer ( ).getScheduler ( ).runTaskTimer ( this, () -> {
            int idWorld = this.getServer ( ).getWorld ( "world" ).getEnvironment ( ).getId ( );
            IWorld world = NpcAPI.Instance ( ).getIWorld ( idWorld );
            World worldBukkit = Bukkit.getWorld ( "world" );

            Location randomLocation = getRandomLocation ( worldBukkit, 0, 0, 200 );
            String npcName = getRandomNpcName ( );
            if (npcName == null) {
                return;
            }
            Integer rarity = CmdBosses.npcRarity.get ( npcName );
            if (rarity == null) rarity = 100;

            if (Math.random ( ) * 100 > rarity) {
                return;
            }

            boss = (ICustomNpc<?>) world.spawnClone (
                    randomLocation.getBlockX ( ),
                    randomLocation.getBlockY ( ),
                    randomLocation.getBlockZ ( ),
                    1,
                    npcName
            );

            String coords = String.format ( "X: %d, Y: %d, Z: %d",
                    randomLocation.getBlockX ( ),
                    randomLocation.getBlockY ( ),
                    randomLocation.getBlockZ ( )
            );

            if (rarity < 20) {
                Bukkit.broadcastMessage ( CC.translate ( "&c[Alerta Boss] &f¡Un boss legendario ha aparecido!&6 " + npcName + " &fen &b" + coords + "&f!" ) );
                for (Player player : this.getServer ( ).getOnlinePlayers ( )) {
                    player.playSound ( player.getLocation ( ), Sound.WITHER_SPAWN, 1.0f, 1.0f );
                }
            } else {
                Bukkit.broadcastMessage ( CC.translate ( "&c[Alerta Boss] &f¡Ha aparecido un boss llamado &6" + npcName + " &fen &b" + coords + "&f!" ) );
                for (Player player : this.getServer ( ).getOnlinePlayers ( )) {
                    player.playSound ( player.getLocation ( ), Sound.ENDERDRAGON_GROWL, 1.0f, 1.0f );
                }
            }
        }, 60 * 5 * 20, 2 * 60 * 60 * 20L );
    }


    private static final Random random = new Random ( );

    /**
     * Genera una ubicación aleatoria dentro de un rango en el mundo especificado.
     *
     * @param world   Mundo en el que se generará la ubicación
     * @param centerX Coordenada X central (puede ser 0 si quieres que sea aleatorio desde el spawn)
     * @param centerZ Coordenada Z central
     * @param range   Rango máximo en bloques desde el centro
     * @return Una ubicación aleatoria
     */
    public static Location getRandomLocation ( World world, int centerX, int centerZ, int range ) {
        int x = centerX + random.nextInt ( range * 2 ) - range;
        int z = centerZ + random.nextInt ( range * 2 ) - range;
        int y = world.getHighestBlockYAt ( x, z );

        return new Location ( world, x + 0.5, y, z + 0.5 );
    }

    public static String getRandomNpcName () {
        if (CmdBosses.npcNames.isEmpty ( )) return null;

        int index = random.nextInt ( CmdBosses.npcNames.size ( ) );
        return CmdBosses.npcNames.get ( index );
    }
}