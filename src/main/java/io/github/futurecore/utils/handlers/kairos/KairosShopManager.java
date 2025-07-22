package io.github.futurecore.utils.handlers.kairos;

import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import io.github.futurecore.utils.CC;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class KairosShopManager {

    public static List<ItemStack> items = new ArrayList<> ( );
    public static void addItem ( ItemStack item ) {
        items.add ( item.clone ( ) );
    }

    public static void openShop ( Player player ) {
        Inventory contents = SmartInventory.builder ( )
                .id ( "kairos-shop" )
                .provider ( new KairosShopMenu ( items ) )
                .type ( InventoryType.CHEST )
                .size ( 6, 9 )
                .title ( CC.translate ( "&6❃ &eTienda de Zenkais &6❃" ) )
                .build ( )
                .open ( player );

    }
}

