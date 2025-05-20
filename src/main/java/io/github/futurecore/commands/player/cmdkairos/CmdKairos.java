package io.github.futurecore.commands.player.cmdkairos;

import io.github.futurecore.Main;
import io.github.futurecore.utils.CC;
import io.github.futurecore.utils.ItemSerializationUtil;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import io.github.futurecore.utils.data.KairosData.ItemKairos;
import io.github.futurecore.utils.data.KairosData.PKairos;
import io.github.futurecore.utils.data.KairosDataHandler.KairosDataHandler;
import io.github.futurecore.utils.handlers.kairos.KairosShopManager;
import io.github.futurecore.utils.handlers.kairos.KairosTopMenu;
import io.github.futurecore.utils.handlers.kairos.KairosTraderManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.delaware.tools.NbtHandler.NbtHandler;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CmdKairos extends BaseCommand {
    @Command(name = "kairos", inGameOnly = false)
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        if (command.getArgs ( ).length == 0) {
            showHelpMessage ( command );
            return;
        }

        PKairos pKairos = null;
        if (command.getSender ( ) instanceof Player) {
            pKairos = new PKairos ( command.getPlayer ( ).getUniqueId ( ) );
        }

        String aliases = command.getArgs ( 0 );
        switch (aliases.toLowerCase ( )) {
            case "balance":
                if (command.getSender ( ) instanceof ConsoleCommandSender) {
                    command.getSender ( ).sendMessage ( CC.translate ( "&dBalance: &5Infinito" ) );
                    return;
                }
                if (pKairos == null) return;
                pKairos.balance ( );
                break;

            case "pay":
                if (pKairos == null || command.getArgs ( ).length < 3) return;
                double amount = 0.0;
                try {
                    amount = Double.parseDouble ( command.getArgs ( 1 ) );
                } catch (NumberFormatException exception) {
                    return;
                }
                Player toPlayer = Main.instance.getServer ( ).getPlayer ( command.getArgs ( 2 ) );
                if (toPlayer == null) return;

                if (toPlayer.getName ( ).equals ( command.getPlayer ( ).getName ( ) )) {
                    command.getPlayer ( ).sendMessage ( CC.translate ( "&c¡No puedes pagarte a ti mismo!" ) );
                    return;
                }

                if (pKairos.getAmount ( ) >= amount) {
                    pKairos.pay ( amount, toPlayer.getUniqueId ( ) );
                    command.getSender ( ).sendMessage ( CC.translate ( "&a¡Has enviado &2" + amount + " &aa la cuenta del jugador: &2" + toPlayer.getName ( ) + "!" ) );
                    toPlayer.sendMessage ( CC.translate ( "&aEl jugador &2" + command.getSender ( ).getName ( ) + "&a te ha depositado en tu cuenta" ) );
                } else {
                    command.getPlayer ( ).sendMessage ( CC.translate ( "&c¡No tienes saldo suficiente en tu cuenta para realizar esta acción!" ) );
                }
                break;

            case "shop":
                KairosShopManager.openShop ( command.getPlayer ( ) );
                break;

            case "top":
                KairosTopMenu.open ( command.getPlayer ( ) );
                break;

            case "trader":
                KairosTraderManager.openTrader ( command.getPlayer ( ) );
                break;

            case "additem":
                if (!command.getPlayer ( ).hasPermission ( "kairos.additem" )) {
                    command.getPlayer ( ).sendMessage ( ChatColor.RED + "No tienes permiso para usar este comando." );
                    return;
                }
                Player player = command.getPlayer ( );
                try {
                    double price = Double.parseDouble ( command.getArgs ( 1 ) );
                    ItemStack handItem = player.getItemInHand ( );
                    if (handItem == null || handItem.getType ( ) == Material.AIR) {
                        player.sendMessage ( ChatColor.RED + "¡Debes tener un ítem en la mano!" );
                        return;
                    }

                    DecimalFormat formatter = new DecimalFormat ( "#,###.00" );
                    String formattedPrice = formatter.format ( price );
                    ItemMeta meta = handItem.getItemMeta ( );
                    List<String> lore = meta.hasLore ( ) ? new ArrayList<> ( meta.getLore ( ) ) : new ArrayList<> ( );
                    lore.add ( CC.translate ( "&f " ) );
                    lore.add ( ChatColor.GREEN + "Precio: " + ChatColor.DARK_AQUA + "$" + formattedPrice + " KAIROS" );
                    meta.setLore ( lore );
                    handItem.setItemMeta ( meta );
                    KairosShopManager.addItem ( handItem );

                    File file = new File ( Main.instance.getDataFolder ( ), "FutureCore/kairos/player/shopitems.txt" );
                    try {
                        KairosShopManager.items = ItemSerializationUtil.loadItemsFromFile ( file );
                    } catch (Exception e) {
                        e.printStackTrace ( );
                        player.sendMessage ( "§cHubo un error al cargar la tienda, se usará una tienda vacía." );
                    }

                    NbtHandler nbtHandler = new NbtHandler ( handItem );
                    int usages = nbtHandler.getInteger ( "usages" );
                    int maxUses = nbtHandler.getInteger ( "maxUses" );

                    meta = handItem.getItemMeta ( );
                    lore = meta.hasLore ( ) ? meta.getLore ( ) : new ArrayList<> ( );
                    lore.add ( CC.translate ( "&aUsages: &2" + usages + "/" + maxUses ) );
                    meta.setLore ( lore );
                    handItem.setItemMeta ( meta );

                    KairosShopManager.items.add ( handItem );
                    ItemSerializationUtil.saveItemsToFile ( file, KairosShopManager.items );

                    player.sendMessage ( ChatColor.GREEN + "Ítem agregado a la tienda con precio $" + formattedPrice + " KAIROS." );
                } catch (NumberFormatException e) {
                    player.sendMessage ( ChatColor.RED + "Precio inválido." );
                }
                player.sendMessage ( ChatColor.RED + "Uso: /kairos addItem <precio>" );
                break;

            case "item":
                if (!command.getPlayer ( ).hasPermission ( "kairos.item" )) {
                    command.getPlayer ( ).sendMessage ( ChatColor.RED + "No tienes permiso para usar este comando." );
                    return;
                }
                player = command.getPlayer ( );
                amount = Double.parseDouble ( command.getArgs ( 1 ) );
                ItemStack item = player.getItemInHand ( );
                if (item == null) return;

                ItemMeta meta = item.getItemMeta ( );
                List<String> lore = new ArrayList<> ( );
                lore.add ( CC.translate ( "&7Moneda de uso especial en el mercado." ) );
                lore.add ( CC.translate ( "&7Aceptada por comerciantes de todo el reino." ) );
                lore.add ( CC.translate ( "&7Puede canjearse por objetos, armas y recursos." ) );
                lore.add ( CC.translate ( "&8No se puede fabricar ni duplicar." ) );
                meta.setDisplayName ( CC.translate ( "&E ⛃ " + amount + " KAIROS" ) );
                meta.setLore ( lore );
                item.setItemMeta ( meta );

                NbtHandler nbtHandler = new NbtHandler ( item );
                nbtHandler.setInteger ( "kairosAmount", (int) amount );
                nbtHandler.addCompound ( "Kairos" );
                player.setItemInHand ( nbtHandler.getItemStack ( ) );

                player.sendMessage ( CC.translate ( "&aMoneda del Kairos de &2" + amount + " &aregistrada correctamente" ) );
                new ItemKairos ( meta.getDisplayName ( ), meta.getLore ( ), item.getTypeId ( ), (int) amount );
                break;

            case "get":
                if (!command.getPlayer ( ).hasPermission ( "kairos.get" )) {
                    command.getPlayer ( ).sendMessage ( ChatColor.RED + "No tienes permiso para usar este comando." );
                    return;
                }
                player = command.getPlayer ( );
                amount = Integer.parseInt ( command.getArgs ( 1 ) );
                ItemKairos itemKairos = KairosDataHandler.itemsKairos.get ( (int) amount );
                ItemStack itemStack = new ItemStack ( itemKairos.getId ( ), 1 );
                meta = itemStack.getItemMeta ( );
                meta.setDisplayName ( CC.translate ( itemKairos.getDisplayName ( ) ) );
                meta.setLore ( itemKairos.getLore ( ) );
                itemStack.setItemMeta ( meta );

                nbtHandler = new NbtHandler ( itemStack );
                nbtHandler.setInteger ( "kairosAmount", (int) amount );
                nbtHandler.addCompound ( "Kairos" );
                player.setItemInHand ( nbtHandler.getItemStack ( ) );

                player.sendMessage ( CC.translate ( "&aMoneda del Kairos de &2" + amount + " &agiveada correctamente" ) );
                break;

            case "give":
                if (!command.getPlayer ( ).hasPermission ( "kairos.give" )) {
                    command.getPlayer ( ).sendMessage ( ChatColor.RED + "No tienes permiso para usar este comando." );
                    return;
                }
                if (pKairos == null || command.getArgs ( ).length < 2) return;
                try {
                    double amountGive = Double.parseDouble ( command.getArgs ( 1 ) );
                    String playerName = command.getArgs ( 2 );
                    player = command.getPlayer ( ).getServer ( ).getPlayer ( playerName );
                    pKairos = new PKairos ( player.getUniqueId ( ) );
                    pKairos.pay ( amountGive );
                    command.getPlayer ( ).sendMessage ( CC.translate ( "&aSe han añadido &2" + amountGive + " &aKairos a tu cuenta." ) );
                } catch (NumberFormatException e) {
                    command.getPlayer ( ).sendMessage ( ChatColor.RED + "Cantidad inválida." );
                }
                break;

            default:
                showHelpMessage ( command );
                break;
        }
    }


    public void showHelpMessage ( CommandArgs command ) {
        command.getSender ( ).sendMessage ( CC.translate ( "&8&m---------------------------------" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&b&lFutureCore Kairos - Ayuda" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&7/kairos balance &8- &fMuestra tu saldo actual" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&7/kairos pay <jugador> <cantidad> &8- &fEnvía monedas a otro jugador" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&7/kairos shop &8- &fAbre la tienda de intercambio" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&7/kairos trader &8- &fIntercambia kairos virtuales por ítems físicos" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&7/kairos additem <precio> &8- &fAgrega el ítem en tu mano a la tienda" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&7/kairos item <cantidad> &8- &fConvierte el ítem en tu mano en una moneda kairos" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&7/kairos get <cantidad> &8- &fObtiene una moneda física de kairos (para admins)" ) );
        command.getSender ( ).sendMessage ( CC.translate ( "&8&m---------------------------------" ) );

    }
}
