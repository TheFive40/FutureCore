package io.github.futurecore.commands.player.cmdcore.cmdItemsAbilities;

import io.github.futurecore.Main;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.delaware.tools.NbtHandler.NbtHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CmdSwordZ extends BaseCommand {

    @Command(name = "espadaz", aliases = "espadaz", permission = "dbfuture.espadaz", inGameOnly = false)
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        Player player = Main.instance.getServer ( ).getPlayer ( command.getArgs ( 0 ) );
        ItemStack sword = new ItemStack ( 4467, 1 );
        ItemMeta meta = sword.getItemMeta ( );

        meta.setDisplayName ( ChatColor.translateAlternateColorCodes ( '&',
                "&e&lEspada Z &7(Dragon Ball)"
        ) );

        List<String> lore = new ArrayList<> ( );
        lore.add ( ChatColor.translateAlternateColorCodes ( '&', "&7La espada legendaria custodiada por los Kaioshin." ) );
        lore.add ( ChatColor.translateAlternateColorCodes ( '&', "&7Dicen que ningún mortal pudo blandirla jamás." ) );
        lore.add ( "" );
        lore.add ( ChatColor.translateAlternateColorCodes ( '&', "&6&lRELIQUIA SAGRADA:" ) );
        lore.add ( ChatColor.translateAlternateColorCodes ( '&', "&eForjada en tiempos antiguos, encierra un poder" ) );
        lore.add ( ChatColor.translateAlternateColorCodes ( '&', "&eque solo un guerrero digno puede liberar." ) );
        lore.add ( "" );
        lore.add ( ChatColor.translateAlternateColorCodes ( '&', "&8Se dice que, al romperse, desata el" ) );
        lore.add ( ChatColor.translateAlternateColorCodes ( '&', "&cPoder &fMístico &8oculto en tu interior." ) );
        lore.add ( "" );
        lore.add ( ChatColor.translateAlternateColorCodes ( '&', "&aUsages: &2500/500" ) );

        meta.setLore ( lore );
        sword.setItemMeta ( meta );
        NbtHandler nbtHandler = new NbtHandler ( sword );
        nbtHandler.setInteger ( "usages", 100 );
        nbtHandler.setInteger ( "maxUses", 100 );
        sword = nbtHandler.getItemStack ( );

        player.getInventory ( ).addItem ( sword );
        player.sendMessage ( ChatColor.translateAlternateColorCodes ( '&',
                "&a¡Has recibido la &d&lEspada Z&a!" ) );
    }

    /**
     * Verifica si un ItemStack es la Espada Z.
     *
     * @param item ItemStack a comprobar.
     * @return true si cumple con ID, nombre y lore.
     */
    public static boolean isEspadaZ ( ItemStack item ) {
        if (item == null) return false;
        if (item.getTypeId ( ) != 4467) return false;

        ItemMeta meta = item.getItemMeta ( );
        if (meta == null || !meta.hasDisplayName ( ) || !meta.hasLore ( )) return false;

        String expectedName = ChatColor.translateAlternateColorCodes ( '&',
                "&e&lEspada Z &7(Dragon Ball)" );

        if (!Objects.equals ( meta.getDisplayName ( ), expectedName )) return false;

        List<String> lore = meta.getLore ( );
        if (lore == null || lore.isEmpty ( )) return false;

        List<String> requiredLines = Arrays.asList (
                ChatColor.translateAlternateColorCodes ( '&', "&7La espada legendaria custodiada por los Kaioshin." ),
                ChatColor.translateAlternateColorCodes ( '&', "&7Dicen que ningún mortal pudo blandirla jamás." ),
                ChatColor.translateAlternateColorCodes ( '&', "&6&lRELIQUIA SAGRADA:" ),
                ChatColor.translateAlternateColorCodes ( '&', "&eForjada en tiempos antiguos, encierra un poder" ),
                ChatColor.translateAlternateColorCodes ( '&', "&eque solo un guerrero digno puede liberar." ),
                ChatColor.translateAlternateColorCodes ( '&', "&8Se dice que, al romperse, desata el" ),
                ChatColor.translateAlternateColorCodes ( '&', "&cPoder &fMístico &8oculto en tu interior." )
        );
        for (String requiredLine : requiredLines) {
            if (!lore.contains ( requiredLine )) {
                return false;
            }
        }

        return true;
    }

}
