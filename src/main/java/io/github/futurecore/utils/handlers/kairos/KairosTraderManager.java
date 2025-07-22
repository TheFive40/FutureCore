package io.github.futurecore.utils.handlers.kairos;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class KairosTraderManager {

    public static final int ID_KAIROS_1 = 6175;
    public static final int ID_KAIROS_5 = 6176;
    public static final int ID_KAIROS_20 = 6174;

    public static ItemStack getKairosItem(double amount) {
        ItemStack item = new ItemStack(Material.getMaterial( getKairosId(amount) ));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e⛁ " + amount + " ZENKAIS");
        meta.setLore(Arrays.asList(
                "§7Click izquierdo para intercambiar",
                "§7Convertir tus §eZenkais virtuales",
                "§7en Zenkais físicos (ítems)."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static int getKairosId(double amount) {
        if (amount == 50.0) return ID_KAIROS_1;
        if (amount == 10.0) return ID_KAIROS_5;
        if (amount == 100.0) return ID_KAIROS_20;
        return 0;
    }

    public static void openTrader(Player player) {
        KairosTraderMenu.getInventory().open(player);
    }
}
