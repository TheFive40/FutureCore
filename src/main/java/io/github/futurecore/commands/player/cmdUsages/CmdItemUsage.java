package io.github.futurecore.commands.player.cmdUsages;

import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.delaware.tools.NbtHandler.NbtHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CmdItemUsage extends BaseCommand {
    @Command(name = "futurecore.usage", permission = "futurecore.items.usage", aliases = "itemUses")
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        if (command.getPlayer ( ).getItemInHand ( ) == null) return;
        int maxUses = 100;
        try {
            maxUses = Integer.parseInt ( command.getArgs ( 0 ) );

        } catch (NumberFormatException exception) {
            command.getSender ( ).sendMessage ( CC.translate ( "&cUtiliza el formato númerico" ) );
        }
        NbtHandler nbtHandler = new NbtHandler ( command.getPlayer ( ).getItemInHand ( ) );
        nbtHandler.setInteger ( "usages", maxUses );
        nbtHandler.setInteger ( "maxUses", maxUses );
        command.getPlayer ().setItemInHand ( nbtHandler.getItemStack () );
        command.getSender ( ).sendMessage ( CC.translate ( "&aUsos máximos del item: &e" + maxUses + " &aregistrados correctamente" ) );
    }

    public static boolean hasPermanent ( ItemStack item ) throws Exception {
        if (item == null ) return false;
        NbtHandler nbtHandler = new NbtHandler ( item );
        return nbtHandler.getInteger ( "usages" ) <= -10;
    }

    public static ItemStack hasUses(ItemStack itemStack, Player player) throws Exception {
        if (itemStack == null) return null;

        NbtHandler nbtHandler = new NbtHandler(itemStack);
        int uses = nbtHandler.getInteger("usages");
        int maxUses = nbtHandler.getInteger("maxUses");

        uses -= 1;

        if (uses > 0 || maxUses <= 0) {
            if (maxUses > 0) {
                nbtHandler.setInteger("usages", uses);
            }

            itemStack = nbtHandler.getItemStack();

            if (maxUses > 0) {
                double percentage = (double) uses / maxUses;
                String color = percentage <= 0.25 ? "&c" : percentage <= 0.5 ? "&e" : "&2";

                ItemMeta itemMeta = itemStack.getItemMeta();
                List<String> lore = (itemMeta.hasLore() && itemMeta.getLore() != null)
                        ? new ArrayList<>(itemMeta.getLore())
                        : new ArrayList<>();

                String usageLinePrefix = "§8➤ §aUsages:";
                String usageLine = usageLinePrefix + " " + color + uses + "/" + maxUses;

                boolean found = false;
                for (int i = 0; i < lore.size(); i++) {
                    if (lore.get(i).startsWith(usageLinePrefix)) {
                        lore.set(i, CC.translate(usageLine));
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    lore.add(CC.translate("&f "));
                    lore.add(CC.translate(usageLine));
                }

                itemMeta.setLore(lore);
                itemStack.setItemMeta(itemMeta);
            }

            return itemStack;
        }
        player.playSound(player.getLocation(), Sound.ANVIL_BREAK, 1.0f, 1.0f);
        player.sendMessage(CC.translate("&cHaz alcanzado la cantidad máxima de usos"));
        return null;
    }



}
