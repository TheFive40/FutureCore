package io.github.futurecore.utils.handlers.zenis;

import io.github.futurecore.utils.handlers.kairos.KairosTraderMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.delaware.tools.CustomItems.CustomItems;

public class ZenisTraderManager {

    public static final int ID_ZENIS_100 = 6086;
    public static final int ID_ZENIS_500 = 4444;
    public static final int ID_ZENIS_1000 = 6084;
    public static final int ID_ZENIS_10000 = 6080;


    public static ItemStack getZenisItem ( int amount ) {
        if (amount < 10000) {
            ItemStack item = CustomItems.getCustomItem ( "ZENIS_" + amount ).toItemStack ( );
            return item;
        }
        return CustomItems.getCustomItem ( "BOLSA_ZENIS" ).toItemStack ( );
    }

    private static int getZenisId ( double amount ) {
        if (amount == 100) return ID_ZENIS_100;
        if (amount == 500) return ID_ZENIS_500;
        if (amount == 1000) return ID_ZENIS_1000;
        if (amount == 10000) return ID_ZENIS_10000;
        return 0;
    }

    public static void openTrader ( Player player ) {
        ZenisTraderMenu.getInventory ( ).open ( player );
    }
}
