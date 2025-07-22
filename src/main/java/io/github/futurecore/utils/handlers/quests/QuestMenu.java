package io.github.futurecore.utils.handlers.quests;
import fr.minuskube.inv.SmartInventory;
import io.github.futurecore.utils.CC;
import org.bukkit.entity.Player;

public class QuestMenu {
    private static final SmartInventory INVENTORY = SmartInventory.builder()
            .id("questsMenu")
            .provider(new QuestProvider ())
            .size(6, 9)
            .title( CC.translate ( "&e&k::&6Misiones&e&k::"))
            .build();

    public static void open( Player player) {
        INVENTORY.open(player);
    }
}
