package io.github.futurecore.commands.player.cmdcore.cmdItemsAbilities;

import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CmdKiScythe2 extends BaseCommand {

    @Command(name = "guadana2", aliases = "guadana2", permission = "dbfuture.guadana2")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        Player player = command.getPlayer();

        ItemStack scythe = new ItemStack(5009, 1);
        ItemMeta meta = scythe.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                "&d&lGuadaña de Ki &7(Black)"
        ));

        meta.setLore(Arrays.asList(
                ChatColor.translateAlternateColorCodes('&', "&7El llamado del rey nunca queda sin respuesta..."),
                ChatColor.translateAlternateColorCodes('&', "&7La Orden Delaware despierta para proteger a su soberano."),
                "",
                ChatColor.translateAlternateColorCodes('&', "&5&lHABILIDAD ESPECIAL:"),
                ChatColor.translateAlternateColorCodes('&', "&6Invoca guardianes élite de la Orden Delaware,"),
                ChatColor.translateAlternateColorCodes('&', "&6quienes copiarán tus estadísticas y te defenderán"),
                ChatColor.translateAlternateColorCodes('&', "&6durante &a10 segundos &6con lealtad inquebrantable."),
                "",
                ChatColor.translateAlternateColorCodes('&', "&eCooldown: &c15 minutos"),
                "",
                ChatColor.translateAlternateColorCodes('&', "&d&lITEM DIVINO")
        ));

        scythe.setItemMeta(meta);
        player.getInventory().addItem(scythe);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a¡Has recibido la &d&lGuadaña de Ki (Black)&a!"));
    }

    /**
     * Verifica si un ItemStack es la Guadaña de Ki.
     * @param item ItemStack a comprobar.
     * @return true si cumple con ID, nombre y lore.
     */
    public static boolean isGuadana(ItemStack item) {
        if (item == null) return false;
        if (item.getTypeId() != 5009) return false; // Verifica el ID

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.hasLore()) return false;

        String expectedName = ChatColor.translateAlternateColorCodes('&',
                "&d&lGuadaña de Ki &7(Black)");

        if (!Objects.equals(meta.getDisplayName(), expectedName)) return false;

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return false;

        // Validamos que contenga las líneas principales de la habilidad
        List<String> requiredLines = Arrays.asList(
                ChatColor.translateAlternateColorCodes('&', "&5&lHABILIDAD ESPECIAL:"),
                ChatColor.translateAlternateColorCodes('&', "&6Invoca guardianes élite de la Orden Delaware,"),
                ChatColor.translateAlternateColorCodes('&', "&eCooldown: &c15 minutos"),
                ChatColor.translateAlternateColorCodes('&', "&d&lITEM DIVINO")
        );

        for (String requiredLine : requiredLines) {
            if (!lore.contains(requiredLine)) {
                return false;
            }
        }

        return true;
    }
}
