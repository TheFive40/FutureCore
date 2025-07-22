package io.github.futurecore.utils.handlers.quests;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.handlers.kairos.builder.ItemBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static io.github.futurecore.Main.npcLocations;
import static io.github.futurecore.commands.player.cmdcore.CmdQuests.questDescriptions;
import static io.github.futurecore.commands.player.cmdcore.CmdQuests.questTitles;

public class QuestProvider implements InventoryProvider {
    private final int ITEMS_PER_PAGE = 21;

    @Override
    public void init ( Player player, InventoryContents contents ) {
        List<ClickableItem> items = new ArrayList<> ( );
        Pagination pagination = contents.pagination ( );
        for (String id : npcLocations.keySet ( )) {
            String title = questTitles.getOrDefault ( id, "Sin Título" );
            List<String> desc = questDescriptions.getOrDefault ( id, Arrays.asList ( "§7Sin descripción asignada." ) );
            ItemStack item = new ItemBuilder ( Material.BOOK )
                    .name ( "§b" + title + " §7(#" + id + ")" )
                    .lore ( CC.translate ( desc ) )
                    .build ( );
            items.add ( ClickableItem.of ( item, e -> {
                Location loc = npcLocations.get ( id );
                if (e.isRightClick ( )) {
                    if (player.hasPermission ( "dtools.misiones.eliminar" )) {
                        npcLocations.remove ( id );
                        questTitles.remove ( id );
                        questDescriptions.remove ( id );
                        player.sendMessage ( "§cHas eliminado la misión §e" + id + "§c." );
                        player.closeInventory ( );
                        Bukkit.getScheduler ( ).runTaskLater ( Main.instance, () -> {
                            player.performCommand ( "misiones" );
                        }, 2L );
                    } else {
                        player.sendMessage ( "§cNo tienes permiso para eliminar misiones." );
                    }
                    return;
                }
                if (loc == null) {
                    player.sendMessage ( "§cMisión no disponible. No hay ubicación registrada." );
                    return;
                }
                player.teleport ( loc );
                player.sendMessage ( "§aHas sido teleportado a la ubicación de la misión." );
            } ) );

        }
        pagination.setItems ( items.toArray ( new ClickableItem[0] ) );
        pagination.setItemsPerPage ( ITEMS_PER_PAGE );

        pagination.addToIterator ( contents.newIterator ( SlotIterator.Type.HORIZONTAL, 1, 1 )
                .allowOverride ( false )
                .blacklist ( 4, 1 )
                .blacklist ( 4, 2 )
                .blacklist ( 4, 3 )
                .blacklist ( 4, 4 )
                .blacklist ( 4, 5 )
                .blacklist ( 4, 6 )
                .blacklist ( 4, 7 )
                .blacklist ( 0,0 )
                .blacklist ( 1,0 )
                .blacklist ( 2,0 )
                .blacklist ( 3,0 )
                .blacklist ( 4,0 )
                .blacklist ( 5,0 )
                .blacklist ( 5,1 )
                .blacklist ( 5,2 )
                .blacklist ( 5,3 )
                .blacklist ( 5,4 )
                .blacklist ( 5,5 )
                .blacklist ( 5,6 )
                .blacklist ( 5,7 )
                .blacklist ( 5,8 )
                .blacklist ( 0,8 )
                .blacklist ( 1,8 )
                .blacklist ( 2,8 )
                .blacklist ( 3,8 )
                .blacklist ( 4,8 )
                .blacklist ( 5,8 )
                .blacklist ( 0,1 )
                .blacklist ( 0,2 )
                .blacklist ( 0,3 )
                .blacklist ( 0,4 )
                .blacklist ( 0,5 )
                .blacklist ( 0,6 )
                .blacklist ( 0,7 )
                .blacklist ( 0,8 )


        );

        contents.set ( 4, 2, ClickableItem.of (
                new ItemBuilder ( Material.ARROW ).name ( "§ePágina anterior" ).build ( ),
                e -> {
                    if (!pagination.isFirst ( )) {
                        int page = pagination.previous ( ).getPage ( );
                        contents.inventory ( ).open ( player, page );
                    }
                } ) );

        contents.set ( 4, 6, ClickableItem.of (
                new ItemBuilder ( Material.ARROW ).name ( "§ePágina siguiente" ).build ( ),
                e -> {
                    if (!pagination.isLast ( )) {
                        int page = pagination.next ( ).getPage ( );
                        contents.inventory ( ).open ( player, page );
                    }
                } ) );

        contents.fillBorders ( ClickableItem.empty ( new ItemStack ( Material.STAINED_GLASS_PANE, 1, (short) 10 ) ) );
    }

    @Override
    public void update ( Player player, InventoryContents contents ) {
    }
}


