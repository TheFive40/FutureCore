package io.github.futurecore.utils.handlers.kairos;

import fr.minuskube.inv.SmartInventory;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class KairosTopMenu {

    private static final SmartInventory INVENTORY = SmartInventory.builder()
            .id("kairosTop")
            .provider(new KairosTopProvider())
            .size(6, 9)
            .title( ChatColor.YELLOW + "Top Zenkais")
            .build();

    public static void open( Player player) {
        INVENTORY.open(player);
    }
}

