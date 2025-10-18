package io.github.futurecore.commands.player.cmdRaids;

import io.github.futurecore.Main;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import io.github.futurecore.utils.menus.RaidMenu;
import io.github.futurecore.utils.menus.RaidMenuT2;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;

public class CmdRaids extends BaseCommand {
    @Command(name = "cmdRaids", aliases = "cmdRaids", inGameOnly = false)
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        String prefix = command.getArgs ( 0 );
        switch (prefix.toLowerCase ( )) {
            case "t1":
                RaidMenu.open ( Main.instance.getServer ( ).getPlayer ( command.getArgs ( 1 ) ) );
                break;
            case "t2":
                RaidMenuT2.open ( Main.instance.getServer ( ).getPlayer ( command.getArgs ( 1 ) ) );
                break;
            case "t3":
                break;
            case "restart":
                Player player = Main.instance.getServer ( ).getPlayer ( command.getArgs ( 1 ) );
                RaidMenu.cooldowns.remove ( player.getUniqueId ( ) );
                RaidMenuT2.cooldowns.remove ( player.getUniqueId ( ) );
                player.sendMessage ( ChatColor.GREEN + "Raids reestablecidas para el jugador. " );
                break;
        }
    }
}