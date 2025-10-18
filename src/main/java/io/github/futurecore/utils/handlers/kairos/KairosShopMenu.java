package io.github.futurecore.utils.handlers.kairos;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.ItemSerializationUtil;
import io.github.futurecore.utils.data.KairosData.PKairos;
import io.github.futurecore.utils.data.KairosDataHandler.KairosDataHandler;
import io.github.futurecore.utils.handlers.kairos.builder.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.delaware.tools.CustomItems.CustomItems;
import org.delaware.tools.NbtHandler.NbtHandler;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KairosShopMenu implements InventoryProvider {

    private static List<ItemStack> items;

    public KairosShopMenu ( List<ItemStack> items ) {
        this.items = items;
    }

    public static SmartInventory getInventory ( List<ItemStack> items ) {
        return SmartInventory.builder ( )
                .id ( "kairos_shop" )
                .provider ( new KairosShopMenu ( items ) )
                .size ( 6, 9 )
                .title ( ChatColor.GOLD + " » Tienda Zenkais" )
                .build ( );
    }

    @Override
    public void init ( Player player, InventoryContents contents ) {
        Pagination pagination = contents.pagination ( );
        pagination.setItemsPerPage ( 25 );

        List<ClickableItem> clickableItems = new ArrayList<> ( );
        for (ItemStack item : KairosShopManager.items) {
            clickableItems.add ( ClickableItem.of ( item, event -> {
                if (event.getClick ( ) == ClickType.LEFT) {
                    PKairos pKairos = new PKairos ( player.getUniqueId ( ) );
                    double price = getPriceFromLore ( item );

                    if (price == -1) {
                        player.sendMessage ( ChatColor.RED + "Este ítem no tiene un precio válido." );
                        return;
                    }

                    if (pKairos.getAmount ( ) < price) {
                        player.sendMessage ( CC.translate ( "&c¡No tienes suficientes Zenkais! Necesitas &4" + format ( price ) ) );
                        return;
                    }

                    pKairos.pay ( -price );
                    ItemStack cloned = ItemSerializationUtil.deserializeItem ( ItemSerializationUtil.serializeItem ( item ) );

                    if (cloned != null && cloned.hasItemMeta ( )) {
                        ItemMeta meta = cloned.getItemMeta ( );
                        if (meta.hasLore ( )) {
                            for (String line : meta.getLore ( )) {
                                if (line.contains ( "Usages:" )) {
                                    String raw = line.replaceAll ( "§[0-9a-fk-or]", "" );
                                    String[] parts = raw.split ( ": " )[1].split ( "/" );
                                    int usages = Integer.parseInt ( parts[0] );
                                    int maxUses = Integer.parseInt ( parts[1] );
                                    NbtHandler nbtHandler = new NbtHandler ( cloned );
                                    nbtHandler.setInteger ( "usages", usages );
                                    nbtHandler.setInteger ( "maxUses", maxUses );
                                    if (KairosDataHandler.getCustomId ( (ArrayList<String>) meta.getLore ( ) ) == null) {
                                        nbtHandler.setString ( "CUSTOMID", "kairosITEM" );
                                    } else {
                                        nbtHandler.setString ( "CUSTOMID", KairosDataHandler.getCustomId ( (ArrayList<String>) meta.getLore ( ) ) );
                                    }
                                    cloned = nbtHandler.getItemStack ( );
                                    break;
                                }
                            }
                        }
                    }
                    player.getInventory ( ).addItem ( cloned );
                    player.sendMessage ( CC.translate ( "&a¡Has comprado el ítem por &2" + format ( price ) + " &aZenkais!" ) );
                } else if (event.getClick ( ) == ClickType.RIGHT) {
                    if (hasPermissionToRemove ( player )) {
                        items.remove ( item );
                        KairosShopManager.items.remove ( item );
                        player.sendMessage ( ChatColor.GREEN + "¡Has eliminado el ítem de la tienda!" );
                        SmartInventory inventory = getInventory ( items );
                        inventory.open ( player );
                        File file = new File ( Main.instance.getDataFolder ( ), "FutureCore/kairos/player/shopitems.txt" );
                        ItemSerializationUtil.saveItemsToFile ( file, KairosShopManager.items );
                    } else {
                        player.sendMessage ( ChatColor.RED + "No tienes permisos para eliminar este ítem." );
                    }
                }
            } ) );
        }

        pagination.setItems ( clickableItems.toArray ( new ClickableItem[0] ) );
        pagination.addToIterator ( contents.newIterator ( SlotIterator.Type.HORIZONTAL, 1, 1 )
                .allowOverride ( false )
                .blacklist ( 4, 1 )
                .blacklist ( 4, 2 )
                .blacklist ( 4, 3 )
                .blacklist ( 4, 4 )
                .blacklist ( 4, 5 )
                .blacklist ( 4, 6 )
                .blacklist ( 4, 7 ) );
        contents.set ( 4, 2, ClickableItem.of ( new ItemBuilder ( Material.ARROW ).name ( ChatColor.YELLOW + "Página anterior" ).build ( ),
                e -> contents.inventory ( ).open ( player, pagination.previous ( ).getPage ( ) ) ) );
        contents.set ( 4, 6, ClickableItem.of ( new ItemBuilder ( Material.ARROW ).name ( ChatColor.YELLOW + "Página siguiente" ).build ( ),
                e -> contents.inventory ( ).open ( player, pagination.next ( ).getPage ( ) ) ) );


        contents.fillBorders ( ClickableItem.empty ( new ItemStack ( Material.STAINED_GLASS_PANE, 1, (short) 10 ) ) );
    }

    private boolean hasPermissionToRemove ( Player player ) {
        return player.hasPermission ( "kairos.remove.item.manager" ) ||
                player.hasPermission ( "kairos.remove.item.owner" ) ||
                player.hasPermission ( "kairos.remove.item.founder" );
    }


    @Override
    public void update ( Player player, InventoryContents contents ) {
    }

    private double getPriceFromLore ( ItemStack item ) {
        if (!item.hasItemMeta ( ) || !item.getItemMeta ( ).hasLore ( )) return -1;

        for (String line : item.getItemMeta ( ).getLore ( )) {
            String raw = ChatColor.stripColor ( line );
            if (raw.toLowerCase ( ).contains ( "precio:" )) {
                try {
                    String value = raw.replaceAll ( "[^\\d.,]", "" ).replace ( ",", "" );
                    return Double.parseDouble ( value );
                } catch (Exception e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private String format ( double value ) {
        return new DecimalFormat ( "#,###.00" ).format ( value );
    }

    private ItemStack getArrow ( Material material, String name ) {
        ItemStack arrow = new ItemStack ( material );
        ItemMeta meta = arrow.getItemMeta ( );
        meta.setDisplayName ( ChatColor.translateAlternateColorCodes ( '&', name ) );
        arrow.setItemMeta ( meta );
        return arrow;
    }
}
