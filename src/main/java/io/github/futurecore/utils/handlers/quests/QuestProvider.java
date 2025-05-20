package io.github.futurecore.utils.handlers.quests;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.General;
import io.github.futurecore.utils.handlers.kairos.builder.ItemBuilder;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IDBCPlayer;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.handler.IQuestHandler;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.constants.EnumQuestType;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

import static io.github.futurecore.Main.npcLocations;

public class QuestProvider implements InventoryProvider {
    private int page = 0;
    private final int ITEMS_PER_PAGE = 36;

    @Override
    public void init ( Player player, InventoryContents contents ) {
        IDBCPlayer idbcPlayer = General.getDBCPlayer ( player.getName ( ) );
        List<IQuest> finishedQuests = Arrays.asList ( idbcPlayer.getFinishedQuests ( ) );
        List<IQuest> activeQuests = Arrays.asList ( idbcPlayer.getActiveQuests ( ) );
        IQuestHandler questHandler = NpcAPI.Instance ( ).getQuests ( );

        List<IQuest> otherQuests = new ArrayList<> ( );

        questHandler.categories ( ).forEach ( e -> {
            e.quests ( ).forEach ( v -> {
                if (!finishedQuests.contains ( v ) && !activeQuests.contains ( v )) {
                    otherQuests.add ( v );
                }
            } );
        } );

        List<ClickableItem> items = new ArrayList<> ( );
        for (IQuest quest : finishedQuests) {
            if (quest.getNpcName ( ).isEmpty ( ) || Objects.equals ( quest.getNpcName ( ), "" )) continue;
            items.add ( createQuestItem ( quest, "Completada", player ) );
        }
        for (IQuest quest : activeQuests) {
            if (quest.getNpcName ( ).isEmpty ( ) || Objects.equals ( quest.getNpcName ( ), "" )) continue;
            items.add ( createQuestItem ( quest, "Activa", player ) );
        }
        for (IQuest quest : otherQuests) {
            if (quest.getNpcName ( ).isEmpty ( ) || Objects.equals ( quest.getNpcName ( ), "" )) continue;
            items.add ( createQuestItem ( quest, "Disponible", player ) );
        }

        Pagination pagination = contents.pagination ( );
        pagination.setItemsPerPage ( ITEMS_PER_PAGE );
        pagination.setItems ( items.toArray ( new ClickableItem[0] ) );
        pagination.addToIterator ( contents.newIterator ( SlotIterator.Type.HORIZONTAL, 1, 1 )
                .allowOverride ( false )
                .blacklist ( 4, 1 )
                .blacklist ( 4, 2 )
                .blacklist ( 4, 3 )
                .blacklist ( 4, 4 )
                .blacklist ( 4, 5 )
                .blacklist ( 4, 6 )
                .blacklist ( 4, 7 ) );

        contents.set ( 4, 2, ClickableItem.of (
                new ItemBuilder ( Material.ARROW ).name ( ChatColor.YELLOW + "Página anterior" ).build ( ),
                e -> {
                    if (!pagination.isFirst ( )) {
                        int page = pagination.previous ( ).getPage ( );
                        contents.inventory ( ).open ( player, page );
                    }
                } ) );

        contents.set ( 4, 6, ClickableItem.of (
                new ItemBuilder ( Material.ARROW ).name ( ChatColor.YELLOW + "Página siguiente" ).build ( ),
                e -> {
                    if (!pagination.isLast ( )) {
                        int page = pagination.next ( ).getPage ( );
                        contents.inventory ( ).open ( player, page );
                    }
                } ) );

        contents.fillBorders ( ClickableItem.empty ( new ItemStack ( Material.STAINED_GLASS_PANE, 1, (short) 10 ) ) );
    }

    private ClickableItem createQuestItem ( IQuest quest, String status, Player player ) {
        String nameColor = status.equals ( "Completada" ) ? "&a" : status.equals ( "Activa" ) ? "&e" : "&b";
        String name = ChatColor.translateAlternateColorCodes ( '&',
                nameColor + status + " #" + quest.getId ( ) );

        String npcName = quest.getNpcName ( );
        String questName = quest.getName ( );
        String questLog = quest.getLogText ( );
        boolean isRepeatable = quest.getIsRepeatable ( );

        String repeatType = "NUNCA";

        List<String> lore = new ArrayList<> ( );
        lore.add ( ChatColor.GRAY + "Nombre de la misión: " + ChatColor.WHITE + questName );
        lore.add ( ChatColor.GRAY + "NPC: " + ChatColor.WHITE + npcName );
        if (isRepeatable) {
            if (quest.getRepeatType ( ) == EnumQuestRepeat.RLDAILY.ordinal ( )) {
                repeatType = ChatColor.WHITE + "Cada 20 Minutos";

            } else if (quest.getRepeatType ( ) == EnumQuestRepeat.RLDAILY.ordinal ( )) {
                repeatType = ChatColor.GOLD + "Cada 24 Horas";
            } else if (quest.getRepeatType ( ) == EnumQuestRepeat.RLWEEKLY.ordinal ( )) {
                repeatType = ChatColor.DARK_GREEN + "Cada 7 dias";
            }
        } else {
            repeatType = ChatColor.RED + "NUNCA";
        }
        lore.add ( ChatColor.GRAY + "Repetible: " + repeatType );
        lore.add ( ChatColor.GRAY + "Quest Log:" );
        lore.addAll ( splitText ( CC.translate ( "&a" + questLog ), 40 ) ); // 40 caracteres por línea aprox.
        ItemStack item = new ItemBuilder ( Material.getMaterial ( 4437 ) ) // ID personalizado
                .name ( name )
                .lore ( lore )
                .build ( );

        return ClickableItem.of ( item, e -> {
            Location loc = npcLocations.get ( getQuestIdFromItem ( item ) );
            if (loc == null) {
                player.sendMessage ( CC.translate ( "&cMisión no disponible. Asegúrate de que la misión tenga una ubicación registrada." ) );
                return;
            }
            player.teleport ( loc );
            player.sendMessage ( CC.translate ("&aHas sido teleportado a la ubicación de la misión." ));

        } );
    }

    private List<String> splitText ( String text, int maxLineLength ) {
        List<String> result = new ArrayList<> ( );
        StringBuilder currentLine = new StringBuilder ( );
        for (String word : text.split ( " " )) {
            if (currentLine.length ( ) + word.length ( ) + 1 > maxLineLength) {
                result.add ( CC.translate ( "&a" + currentLine ) );
                currentLine = new StringBuilder ( );
            }
            if (currentLine.length ( ) > 0) currentLine.append ( " " );
            currentLine.append ( CC.translate ( "&a" + word ) );
        }
        if (!currentLine.toString ( ).isEmpty ( )) result.add ( CC.translate ( "&a" + currentLine ) );
        return result;
    }

    public static String getQuestIdFromItem ( ItemStack item ) {
        if (item == null || !item.hasItemMeta ( )) return null;

        ItemMeta meta = item.getItemMeta ( );
        if (!meta.hasDisplayName ( )) return null;

        String displayName = meta.getDisplayName ( );
        int start = displayName.indexOf ( "#" );
        if (start == -1) return null;

        int end = displayName.indexOf ( " ", start );
        if (end == -1) end = displayName.length ( );

        return displayName.substring ( start + 1, end ).replaceAll ( "[^0-9]", "" ); // sólo números
    }

    @Override
    public void update ( Player player, InventoryContents contents ) {
    }
}


class MundoHelper {

    private final MultiverseCore mvCore;

    public MundoHelper () {
        Plugin plugin = Bukkit.getPluginManager ( ).getPlugin ( "Multiverse-Core" );
        if (plugin instanceof MultiverseCore) {
            this.mvCore = (MultiverseCore) plugin;
        } else {
            throw new IllegalStateException ( "Multiverse-Core no está habilitado." );
        }
    }

    public World obtenerMundoPorNombre ( String nombre ) {
        MultiverseWorld mvWorld = mvCore.getMVWorldManager ( ).getMVWorld ( nombre );
        if (mvWorld != null) {
            return mvWorld.getCBWorld ( );
        }
        return null;
    }
}
