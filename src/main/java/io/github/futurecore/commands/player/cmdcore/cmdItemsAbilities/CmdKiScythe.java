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

public class CmdKiScythe extends BaseCommand {
    @Command(name = "guadana", aliases = "guadana", permission = "dbfuture.guadana")
    @Override
    public void onCommand(CommandArgs command) throws IOException {
        Player player = command.getPlayer();

        ItemStack scythe = new ItemStack(5009, 1);
        ItemMeta meta = scythe.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                "&d&lGuadaña de Ki &7(Black)"
        ));

        meta.setLore(Arrays.asList(
                ChatColor.translateAlternateColorCodes('&', "&7Golpes directos inmovilizan al enemigo durante &c5s&7,"),
                ChatColor.translateAlternateColorCodes('&', "&7dejándolo vulnerable al juicio espiritual."),
                "",
                ChatColor.translateAlternateColorCodes('&', "&7- Con un &e100% &7de probabilidad, activa &5Corte Espiritual&7:"),
                ChatColor.translateAlternateColorCodes('&', "&7  &8• &7Bloquea el movimiento y el salto del objetivo."),
                ChatColor.translateAlternateColorCodes('&', "&7  &8• &7Drena un &c5% &7de la vida del enemigo cada segundo por &c5s&7."),
                ChatColor.translateAlternateColorCodes('&', "&7  &8• &7Tras &c5s&7 una &dexplosión espiritual &7sacude al objetivo,"),
                ChatColor.translateAlternateColorCodes('&', "&7    quitando &c18% &7de su vida, &b30% &7de su Ki y &e12% &7de su Stamina."),
                "",
                ChatColor.translateAlternateColorCodes('&', "&eCooldown: &c5 minutos"),
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
        if (item.getTypeId() != 5009) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.hasLore()) return false;

        String expectedName = ChatColor.translateAlternateColorCodes('&',
                "&d&lGuadaña de Ki &7(Black)");

        if (!Objects.equals(meta.getDisplayName(), expectedName)) return false;

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return false;

        List<String> requiredLines = Arrays.asList(
                ChatColor.translateAlternateColorCodes('&', "&7Golpes directos inmovilizan al enemigo durante &c5s&7,"),
                ChatColor.translateAlternateColorCodes('&', "&7dejándolo vulnerable al juicio espiritual."),
                "",
                ChatColor.translateAlternateColorCodes('&', "&7- Con un &e100% &7de probabilidad, activa &5Corte Espiritual&7:"),
                ChatColor.translateAlternateColorCodes('&', "&7  &8• &7Bloquea el movimiento y el salto del objetivo."),
                ChatColor.translateAlternateColorCodes('&', "&7  &8• &7Drena un &c5% &7de la vida del enemigo cada segundo por &c5s&7."),
                ChatColor.translateAlternateColorCodes('&', "&7  &8• &7Tras &c5s&7 una &dexplosión espiritual &7sacude al objetivo,"),
                ChatColor.translateAlternateColorCodes('&', "&7    quitando &c18% &7de su vida, &b30% &7de su Ki y &e12% &7de su Stamina."),
                "",
                ChatColor.translateAlternateColorCodes('&', "&eCooldown: &c5 minutos"),
                "",
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
