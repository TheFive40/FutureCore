package io.github.futurecore.commands.player.cmdUsages;

import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import org.bukkit.ChatColor;
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
        ItemStack handItem = nbtHandler.getItemStack ( );
        ItemMeta meta = handItem.getItemMeta ( );
        List<String> lore;
        lore = meta.hasLore ( ) ? meta.getLore ( ) : new ArrayList<> ( );
        lore.add ( CC.translate ( "&aUsages: &2" + maxUses + "/" + maxUses ) );
        meta.setLore ( lore );
        handItem.setItemMeta ( meta );
        command.getPlayer ( ).setItemInHand ( handItem );
        command.getSender ( ).sendMessage ( CC.translate ( "&aUsos máximos del item: &e" + maxUses + " &aregistrados correctamente" ) );
    }

    public static boolean hasPermanent ( ItemStack item ) throws Exception {
        if (item == null) return false;
        NbtHandler nbtHandler = new NbtHandler ( item );
        return nbtHandler.getInteger ( "usages" ) <= -10;
    }

    public static ItemStack hasUses ( ItemStack itemStack, Player player ) throws Exception {
        if (itemStack == null) return null;

        ItemMeta itemMeta = itemStack.getItemMeta ( );
        if (itemMeta == null || !itemMeta.hasLore ( ) || itemMeta.getLore ( ) == null) return itemStack;

        List<String> lore = new ArrayList<> ( itemMeta.getLore ( ) );
        String usageLinePrefix = "§aUsages:";

        int uses = -1;
        int maxUses = -1;

        for (int i = 0; i < lore.size ( ); i++) {
            String line = lore.get ( i );
            if (line.startsWith ( usageLinePrefix )) {
                String[] parts = ChatColor.stripColor ( line ).split ( "Usages:" )[1].trim ( ).split ( "/" );
                if (parts.length == 2) {
                    try {
                        uses = Integer.parseInt ( parts[0].trim ( ) );
                        maxUses = Integer.parseInt ( parts[1].trim ( ) );
                        uses -= 1;
                        if (uses < 0) uses = 0;

                        double percentage = (double) uses / maxUses;
                        String color = percentage <= 0.25 ? "&c" : percentage <= 0.5 ? "&e" : "&2";

                        String usageLine = usageLinePrefix + " " + color + uses + "/" + maxUses;
                        lore.set ( i, CC.translate ( usageLine ) );

                        itemMeta.setLore ( lore );
                        itemStack.setItemMeta ( itemMeta );

                        if (uses == 0 && maxUses > 0) {
                            player.playSound ( player.getLocation ( ), Sound.ANVIL_BREAK, 1.0f, 1.0f );
                            player.sendMessage ( CC.translate ( "&cHas alcanzado la cantidad máxima de usos" ) );
                            return null;
                        }

                        return itemStack;

                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            }
        }

        return itemStack;
    }
    public static ItemStack repairUses(ItemStack itemStack, int amountToRepair, Player player) {
        if (itemStack == null || amountToRepair <= 0) return itemStack;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || !itemMeta.hasLore() || itemMeta.getLore() == null) return itemStack;

        List<String> lore = new ArrayList<>(itemMeta.getLore());
        String usageLinePrefix = "§aUsages:";

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.startsWith(usageLinePrefix)) {
                String[] parts = ChatColor.stripColor(line).split("Usages:")[1].trim().split("/");
                if (parts.length == 2) {
                    try {
                        int currentUses = Integer.parseInt(parts[0].trim());
                        int maxUses = Integer.parseInt(parts[1].trim());

                        // Suma la cantidad a reparar
                        currentUses += amountToRepair;

                        // No permitir que pase del máximo
                        if (currentUses > maxUses) currentUses = maxUses;

                        double percentage = (double) currentUses / maxUses;
                        String color = percentage <= 0.25 ? "&c" : percentage <= 0.5 ? "&e" : "&2";

                        String usageLine = usageLinePrefix + " " + color + currentUses + "/" + maxUses;
                        lore.set(i, CC.translate(usageLine));

                        itemMeta.setLore(lore);
                        itemStack.setItemMeta(itemMeta);

                        player.sendMessage(CC.translate("&aEl item ha sido reparado &e+" + amountToRepair + " &ausos"));
                        return itemStack;

                    } catch (NumberFormatException ignored) {}
                }
                break;
            }
        }

        return itemStack;
    }


}
