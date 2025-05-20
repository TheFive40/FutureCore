package io.github.futurecore.utils.handlers.kairos;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class KairosTraderManager {

    public static final int ID_KAIROS_1 = 4956;
    public static final int ID_KAIROS_5 = 4963;
    public static final int ID_KAIROS_20 = 4979;

    public static ItemStack getKairosItem(double amount) {
        ItemStack item = new ItemStack(Material.getMaterial((int) getKairosId(amount)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e⛁ " + amount + " KAIROS");
        meta.setLore(Arrays.asList(
                "§7Click izquierdo para intercambiar",
                "§7Convertir tus §eKairos virtuales",
                "§7en Kairos físicos (ítems)."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static int getKairosId(double amount) {
        if (amount == 10.0) return ID_KAIROS_1;
        if (amount == 5.0) return ID_KAIROS_5;
        if (amount == 20.0) return ID_KAIROS_20;
        return 0;
    }

    public static void openTrader(Player player) {
        KairosTraderMenu.getInventory().open(player);
    }
}
