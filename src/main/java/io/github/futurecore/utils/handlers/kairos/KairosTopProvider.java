package io.github.futurecore.utils.handlers.kairos;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import io.github.futurecore.utils.data.KairosData.PKairos;
import io.github.futurecore.utils.data.KairosDataHandler.KairosDataHandler;
import io.github.futurecore.utils.handlers.kairos.builder.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class KairosTopProvider implements InventoryProvider {

    private final int ITEMS_PER_PAGE = 25;

    @Override
    public void init ( Player player, InventoryContents contents ) {
        List<PKairos> topPlayers = KairosDataHandler.pKairos;

        Pagination pagination = contents.pagination ( );
        pagination.setItemsPerPage ( ITEMS_PER_PAGE );
        List<ClickableItem> items = new ArrayList<> ( );
        topPlayers.sort(Comparator.comparingDouble(PKairos::getAmount).reversed());

        int position = 1;
        for (PKairos p : topPlayers) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer ( p.getPlayerUUID ( ) );
            ItemStack skull = new ItemStack ( Material.SKULL_ITEM, 1, (short) 3 );
            SkullMeta meta = (SkullMeta) skull.getItemMeta ( );
            meta.setOwner ( offlinePlayer.getName ( ) );
            meta.setDisplayName ( ChatColor.LIGHT_PURPLE + "#" + position + " " + offlinePlayer.getName ( ) );
            meta.setLore ( Arrays.asList (
                    ChatColor.DARK_AQUA + "Kairos: " + ChatColor.GREEN + String.format ( "%,.2f", p.getAmount ( ) )
            ) );
            skull.setItemMeta ( meta );
            items.add ( ClickableItem.empty ( skull ) );
            position++;
        }

        pagination.setItems ( items.toArray ( new ClickableItem[0] ) );
        pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1)
                .allowOverride(false)
                .blacklist(4, 1)
                .blacklist(4, 2)
                .blacklist(4, 3)
                .blacklist(4, 4)
                .blacklist(4, 5)
                .blacklist(4, 6)
                .blacklist(4, 7));

        contents.set(4, 2, ClickableItem.of( new ItemBuilder ( Material.ARROW ).name ( ChatColor.YELLOW + "Página anterior" ).build ( ),
                e -> contents.inventory ().open(player, pagination.previous().getPage())));
        contents.set(4, 6, ClickableItem.of(new ItemBuilder ( Material.ARROW ).name ( ChatColor.YELLOW + "Página siguiente" ).build ( ),
                e -> contents.inventory ().open(player, pagination.next().getPage())));

        contents.fillBorders ( ClickableItem.empty ( new ItemStack ( Material.STAINED_GLASS_PANE, 1, (short) 10 ) ) );
    }

    @Override
    public void update ( Player player, InventoryContents contents ) {
    }
}

