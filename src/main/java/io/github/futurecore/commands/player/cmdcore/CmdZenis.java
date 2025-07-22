package io.github.futurecore.commands.player.cmdcore;

import io.github.futurecore.Main;
import io.github.futurecore.utils.commands.BaseCommand;
import io.github.futurecore.utils.commands.Command;
import io.github.futurecore.utils.commands.CommandArgs;
import io.github.futurecore.utils.handlers.zenis.ZenisTraderManager;
import io.github.futurecore.utils.handlers.zenis.ZenisTraderMenu;
import org.bukkit.entity.Player;

import java.io.IOException;

public class CmdZenis extends BaseCommand {
    @Command(name = "zenis", aliases = "zenis", inGameOnly = false)
    @Override
    public void onCommand ( CommandArgs command ) throws IOException {
        if (command.getSender ( ) instanceof Player) {
            ZenisTraderManager.openTrader ( command.getPlayer ( ) );
            return;
        }
        Player player = Main.instance.getServer ( ).getPlayer ( command.getArgs ( 0 ) );
        ZenisTraderManager.openTrader ( player );

    }
}
