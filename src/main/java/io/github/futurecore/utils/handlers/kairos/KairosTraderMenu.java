package io.github.futurecore.utils.handlers.kairos;

import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.ClickableItem;
import io.github.futurecore.utils.data.KairosData.PKairos;
import io.github.futurecore.utils.handlers.nbt.NBTPhandler;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.delaware.tools.NbtHandler.NbtHandler;

public class KairosTraderMenu implements InventoryProvider {

    public static SmartInventory getInventory() {
        return SmartInventory.builder()
                .id("kairos_trader")
                .provider(new KairosTraderMenu())
                .size(3, 9)
                .title("§d☼ Intercambiador ☼")
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        addKairosButton(contents, player, 1, 2, 5.0);
        addKairosButton(contents, player, 1, 4, 10.0);
        addKairosButton(contents, player, 1, 6, 20.0);

        ItemStack border = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 10);

        for (int col = 0; col < 9; col++) {
            contents.set(0, col, ClickableItem.empty(border));
        }

        for (int col = 0; col < 9; col++) {
            contents.set(2, col, ClickableItem.empty(border));
        }

        contents.set(1, 0, ClickableItem.empty(border));
        contents.set(1, 8, ClickableItem.empty(border));
    }


    private void addKairosButton(InventoryContents contents, Player player, int row, int col, double amount) {
        ItemStack kairosItem = KairosTraderManager.getKairosItem(amount);
        contents.set(row, col, ClickableItem.of(kairosItem, e -> {
            PKairos data = new PKairos(player.getUniqueId());
            if (data.getAmount() < amount) {
                player.sendMessage("§cNo tienes suficientes Kairos para este intercambio.");
                return;
            }

            data.pay(-amount);
            NbtHandler nbtPhandler = new NbtHandler ( kairosItem );
            nbtPhandler.setInteger ( "kairosAmount", (int) amount );
            nbtPhandler.addCompound ( "Kairos" );
            player.getInventory().addItem(nbtPhandler.getItemStack ());
            player.updateInventory ();
            player.sendMessage("§aIntercambiaste §e" + amount + " Kairos §apor ítems físicos.");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1f);
        }));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }
}
